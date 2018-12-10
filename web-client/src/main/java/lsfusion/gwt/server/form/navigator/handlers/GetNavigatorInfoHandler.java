package lsfusion.gwt.server.form.navigator.handlers;

import lsfusion.client.logics.DeSerializer;
import lsfusion.client.navigator.ClientAbstractWindow;
import lsfusion.client.navigator.ClientNavigatorWindow;
import lsfusion.gwt.server.form.navigator.NavigatorActionHandler;
import lsfusion.gwt.server.form.spring.LSFusionDispatchServlet;
import lsfusion.gwt.server.form.convert.ClientNavigatorToGwtConverter;
import lsfusion.gwt.shared.form.actions.navigator.GetNavigatorInfo;
import lsfusion.gwt.shared.form.actions.navigator.GetNavigatorInfoResult;
import lsfusion.gwt.shared.form.view.GNavigatorElement;
import lsfusion.gwt.shared.form.view.window.GAbstractWindow;
import lsfusion.gwt.shared.form.view.window.GNavigatorWindow;
import lsfusion.interop.navigator.RemoteNavigatorInterface;
import net.customware.gwt.dispatch.server.ExecutionContext;
import net.customware.gwt.dispatch.shared.DispatchException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class GetNavigatorInfoHandler extends NavigatorActionHandler<GetNavigatorInfo, GetNavigatorInfoResult> {

    public GetNavigatorInfoHandler(LSFusionDispatchServlet servlet) {
        super(servlet);
    }

    @Override
    public GetNavigatorInfoResult executeEx(GetNavigatorInfo action, ExecutionContext context) throws DispatchException, IOException {
        ClientNavigatorToGwtConverter converter = new ClientNavigatorToGwtConverter();

        RemoteNavigatorInterface remoteNavigator = getRemoteNavigator(action);
        DeSerializer.NavigatorData navigatorData = DeSerializer.deserializeListClientNavigatorElementWithChildren(remoteNavigator.getNavigatorTree());

        GNavigatorElement root = converter.convertOrCast(navigatorData.root);

        ArrayList<GNavigatorWindow> navigatorWindows = new ArrayList<>();
        for (ClientNavigatorWindow window : navigatorData.windows.values()) {
            GNavigatorWindow gWindow = converter.convertOrCast(window);
            navigatorWindows.add(gWindow);
        }

        //getting common windows
        List<ClientAbstractWindow> clientWindows = DeSerializer.deserializeListClientNavigatorWindow(remoteNavigator.getCommonWindows());
        List<GAbstractWindow> windows = new ArrayList<>();
        for (ClientAbstractWindow clientWindow : clientWindows) {
            windows.add((GAbstractWindow) converter.convertOrCast(clientWindow));
        }

        return new GetNavigatorInfoResult(root, navigatorWindows, windows);
    }
}
