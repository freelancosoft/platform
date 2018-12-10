package lsfusion.gwt.server.form.form.handlers;

import lsfusion.gwt.server.form.spring.LSFusionDispatchServlet;
import lsfusion.gwt.server.form.form.FormServerResponseActionHandler;
import net.customware.gwt.dispatch.server.ExecutionContext;
import net.customware.gwt.dispatch.shared.DispatchException;
import lsfusion.gwt.server.form.form.spring.FormSessionObject;
import lsfusion.gwt.shared.form.actions.form.ServerResponseResult;
import lsfusion.gwt.shared.form.actions.form.SetTabVisible;

import java.io.IOException;

public class SetTabVisibleHandler extends FormServerResponseActionHandler<SetTabVisible> {
    public SetTabVisibleHandler(LSFusionDispatchServlet servlet) {
        super(servlet);
    }

    @Override
    public ServerResponseResult executeEx(SetTabVisible action, ExecutionContext context) throws DispatchException, IOException {
        FormSessionObject form = getFormSessionObject(action.formSessionID);
        return getServerResponseResult(form, form.remoteForm.setTabVisible(action.requestIndex, defaultLastReceivedRequestIndex, action.tabbedPaneID, action.tabIndex));
    }
}
