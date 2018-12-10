package lsfusion.gwt.server.form.navigator.handlers;

import lsfusion.gwt.server.form.navigator.NavigatorActionHandler;
import lsfusion.gwt.server.form.spring.LSFusionDispatchServlet;
import lsfusion.gwt.shared.form.actions.navigator.GetClientSettings;
import lsfusion.gwt.shared.form.actions.navigator.GetClientSettingsResult;
import lsfusion.interop.ClientSettings;
import net.customware.gwt.dispatch.server.ExecutionContext;
import net.customware.gwt.dispatch.shared.DispatchException;

import java.io.IOException;

public class GetClientSettingsHandler extends NavigatorActionHandler<GetClientSettings, GetClientSettingsResult> {
    public GetClientSettingsHandler(LSFusionDispatchServlet servlet) {
        super(servlet);
    }

    @Override
    public GetClientSettingsResult executeEx(GetClientSettings action, ExecutionContext context) throws DispatchException, IOException {
        ClientSettings clientSettings = getRemoteNavigator(action).getClientSettings();
        return new GetClientSettingsResult(clientSettings.busyDialog, clientSettings.busyDialogTimeout);
    }
}