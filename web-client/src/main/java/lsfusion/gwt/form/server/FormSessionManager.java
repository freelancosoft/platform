package lsfusion.gwt.form.server;

import lsfusion.gwt.base.server.LogicsAwareDispatchServlet;
import lsfusion.gwt.form.shared.view.GForm;
import lsfusion.interop.RemoteLogicsInterface;
import lsfusion.interop.form.RemoteFormInterface;

import java.io.IOException;

public interface FormSessionManager {
    public GForm createForm(String canonicalName, String formSID, RemoteFormInterface remoteForm, Object[] immutableMethods, byte[] firstChanges, LogicsAwareDispatchServlet<RemoteLogicsInterface> servlet) throws IOException;

    public FormSessionObject getFormSessionObjectOrNull(String formSessionID);

    public FormSessionObject getFormSessionObject(String formSessionID);

    public FormSessionObject removeFormSessionObject(String formSessionID);
}
