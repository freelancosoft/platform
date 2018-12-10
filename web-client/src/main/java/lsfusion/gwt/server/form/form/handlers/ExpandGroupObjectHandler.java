package lsfusion.gwt.server.form.form.handlers;

import lsfusion.gwt.server.form.form.FormServerResponseActionHandler;
import net.customware.gwt.dispatch.server.ExecutionContext;
import net.customware.gwt.dispatch.shared.DispatchException;
import lsfusion.gwt.server.form.spring.LSFusionDispatchServlet;
import lsfusion.gwt.server.form.form.spring.FormSessionObject;
import lsfusion.gwt.server.form.convert.GwtToClientConverter;
import lsfusion.gwt.shared.form.actions.form.ExpandGroupObject;
import lsfusion.gwt.shared.form.actions.form.ServerResponseResult;

import java.io.IOException;

public class ExpandGroupObjectHandler extends FormServerResponseActionHandler<ExpandGroupObject> {
    private static GwtToClientConverter gwtConverter = GwtToClientConverter.getInstance();

    public ExpandGroupObjectHandler(LSFusionDispatchServlet servlet) {
        super(servlet);
    }

    @Override
    public ServerResponseResult executeEx(ExpandGroupObject action, ExecutionContext context) throws DispatchException, IOException {
        FormSessionObject form = getFormSessionObject(action.formSessionID);

        byte[] keyValues = gwtConverter.convertOrCast(action.value);

        return getServerResponseResult(form, form.remoteForm.expandGroupObject(action.requestIndex, defaultLastReceivedRequestIndex, action.groupObjectId, keyValues));
    }
}
