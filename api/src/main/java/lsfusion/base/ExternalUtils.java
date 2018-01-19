package lsfusion.base;

import lsfusion.interop.RemoteLogicsInterface;
import org.apache.http.Consts;
import org.apache.http.HttpEntity;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.ByteArrayBody;
import org.apache.http.entity.mime.content.StringBody;

import javax.mail.BodyPart;
import javax.mail.MessagingException;
import javax.mail.internet.MimeMultipart;
import javax.mail.util.ByteArrayDataSource;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ExternalUtils {

    public static final String defaultCSVCharset = "UTF-8";
    public static final String defaultXMLJSONCharset = "UTF-8";

    public static final Charset stringCharset = Consts.UTF_8;
    public static final ContentType TEXT_PLAIN = ContentType.create(
            "text/plain", stringCharset);
    public static final ContentType MULTIPART_MIXED = ContentType.create(
            "multipart/mixed", stringCharset);

    private static final String ACTION_CN_PARAM = "action";
    private static final String SCRIPT_PARAM = "script";
    private static final String PARAMS_PARAM = "p";
    private static final String RETURNS_PARAM = "returns";
    private static final String PROPERTY_PARAM = "property";

    public static ExternalResponse processRequest(RemoteLogicsInterface remoteLogics, String uri, String query, InputStream is, ContentType requestContentType) throws IOException, MessagingException {
        List<Object> paramsList = BaseUtils.mergeList(getParameterValues(query, PARAMS_PARAM), getListFromInputStream(is, requestContentType));
        List<String> returns = getParameterValues(query, RETURNS_PARAM);
        List<Object> paramList = new ArrayList<>();

        String filename = "export";

        if (uri.startsWith("/exec")) {
            String action = getParameterValue(query, ACTION_CN_PARAM);
            paramList = remoteLogics.exec(action, returns.toArray(new String[returns.size()]), paramsList.toArray());
        } else if (uri.startsWith("/eval")) {
            Object script = getParameterValue(query, SCRIPT_PARAM);
            if (script == null && !paramsList.isEmpty()) {
                //Первый параметр считаем скриптом
                script = paramsList.get(0);
                paramsList = paramsList.subList(1, paramsList.size());
            }
            paramList = remoteLogics.eval(uri.startsWith("/eval/action"), script, returns.toArray(new String[returns.size()]), paramsList.toArray());
        } else if (uri.startsWith("/read")) {
            String property = getParameterValue(query, PROPERTY_PARAM);
            if (property != null) {
                filename = property;
                paramList.addAll(remoteLogics.read(property, paramsList.toArray()));
            }
        }

        HttpEntity entity = null;
        String contentDisposition = null;

        if (!paramList.isEmpty()) {
            Result<String> singleFileExtension = new Result<>();
            entity = getInputStreamFromList(paramList, singleFileExtension);

            if (singleFileExtension.result != null) // если возвращается один файл, задаем ему имя
                contentDisposition = "filename=" + (returns.isEmpty() ? filename : returns.get(0)).replace(',', '_') + "." + singleFileExtension.result;
        }
        return new ExternalResponse(entity, contentDisposition);
    }

    public static ContentType getContentType(String extension) {
        switch (extension) {
            case "jpg":
            case "jpeg":
            case "png":
            case "bmp":
                return ContentType.create("image/" + extension);
            case "":
                return ContentType.APPLICATION_OCTET_STREAM;
            case "csv":
                return ContentType.create("text/" + extension, defaultCSVCharset);
            case "json":
            case "xml":
                return ContentType.create("text/" + extension, defaultXMLJSONCharset);
            default:
                return ContentType.create("application/" + extension);
        }
    }

    public static String getExtensionFromContentType(ContentType contentType, Result<Boolean> humanReadable) {
        Pattern p = Pattern.compile("\\b(application|image)/(\\w*)\\b");
        String mimeType = contentType.getMimeType();
        Matcher m = p.matcher(mimeType);
        if(m.find()) {
            humanReadable.set(false);
            return m.group(2);
        }

        p = Pattern.compile("\\btext/(xml|json|csv)\\b");
        m = p.matcher(mimeType);
        if(m.find()) {
            humanReadable.set(true);
            return m.group(1);
        }

        return null;
    }

    private static String getParameterValue(String query, String key) {
        List<String> params = getParameterValues(query, key);
        return params.isEmpty() ? null : params.get(0);
    }

    private static List<String> getParameterValues(String query, String key) {
        List<String> values = new ArrayList<>();
        if (query != null) {
            for (String entry : query.split("&")) {
                if (entry.contains("=") && entry.substring(0, entry.indexOf("=")).equals(key))
                    values.add(entry.substring(Math.min(entry.indexOf("=") + 1, entry.length() - 1)));
            }
        }
        return values;
    }

    private static Object getRequestParam(Object object, ContentType contentType, boolean convertedToString) throws IOException {
        assert object instanceof InputStream || (object instanceof String && convertedToString);
        if(object instanceof InputStream)
            object = IOUtils.readBytesFromStream((InputStream) object);
        Result<Boolean> humanReadable = new Result<>();
        String extension = getExtensionFromContentType(contentType, humanReadable);
        if(extension != null) { // CUSTOMFILE
            byte[] file;
            if(humanReadable.result && convertedToString)
                file = ((String)object).getBytes(contentType.getCharset());
            else
                file = (byte[])object;
            return BaseUtils.mergeFileAndExtension(file, extension.getBytes());
        } else {
            if(!convertedToString)
                object = new String((byte[])object, contentType.getCharset());
            return object;
        }
    }
    // возвращает или byte[] для CustomFile или String для остальных, contentType может быть null если нет параметров
    public static List<Object> getListFromInputStream(InputStream is, ContentType contentType) throws IOException, MessagingException {
        List<Object> paramsList = new ArrayList<>();
        if (contentType != null) { // если есть параметры, теоретически можно было бы пытаться по другому угадать
            String mimeType;
            if ((mimeType = contentType.getMimeType()).startsWith("multipart/")) {
                byte[] postParams = IOUtils.readBytesFromStream(is);
                MimeMultipart multipart = new MimeMultipart(new ByteArrayDataSource(postParams, mimeType));
                for (int i = 0; i < multipart.getCount(); i++) {
                    BodyPart bodyPart = multipart.getBodyPart(i);
                    Object param = bodyPart.getContent();
                    paramsList.add(getRequestParam(param, ContentType.parse(bodyPart.getContentType()), true)); // multipart автоматически text/* возвращает как String
                }
            } else {
                paramsList.add(getRequestParam(is, contentType, false));
            }
        }
        return paramsList;
    }

    // paramList byte[] || String, можно было бы попровать getRequestResult (по аналогии с getRequestParam) выделить общий, но там возвращаемые классы разные, нужны будут generic'и и оно того не стоит
    public static HttpEntity getInputStreamFromList(List<Object> paramList, Result<String> singleFileExtension) {
        HttpEntity entity;
        int paramCount = paramList.size();
        if (paramCount > 1) {
            MultipartEntityBuilder builder = MultipartEntityBuilder.create();
            builder.setContentType(ExternalUtils.MULTIPART_MIXED);
            for (int i = 0; i < paramCount; i++) {
                Object value = paramList.get(i);
                if (value instanceof byte[])
                    builder.addPart("param" + i, new ByteArrayBody(BaseUtils.getFile((byte[]) value), getContentType(BaseUtils.getExtension((byte[]) value)), "filename"));
                else
                    builder.addPart("param" + i, new StringBody((String) value, ExternalUtils.TEXT_PLAIN));
            }
            entity = builder.build();
        } else {
            Object value = BaseUtils.single(paramList);
            if (value instanceof byte[]) {
                String extension = BaseUtils.getExtension((byte[]) value);
                entity = new ByteArrayEntity(BaseUtils.getFile((byte[]) value), getContentType(extension));
                if(singleFileExtension != null)
                    singleFileExtension.set(extension);
            } else {
                entity = new StringEntity((String) value, ExternalUtils.TEXT_PLAIN);
            }
        }
        return entity;
    }

    public static class ExternalResponse {
        public HttpEntity response;
        public String contentDisposition;

        public ExternalResponse(HttpEntity response, String contentDisposition) {
            this.response = response;
            this.contentDisposition = contentDisposition;
        }
    }
}