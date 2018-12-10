package lsfusion.gwt.shared.form.actions.navigator;

import lsfusion.gwt.shared.form.view.GNavigatorElement;
import lsfusion.gwt.shared.form.view.window.GAbstractWindow;
import lsfusion.gwt.shared.form.view.window.GNavigatorWindow;
import net.customware.gwt.dispatch.shared.Result;

import java.util.ArrayList;
import java.util.List;

public class GetNavigatorInfoResult implements Result {
    public GNavigatorElement root;

    public ArrayList<GNavigatorWindow> navigatorWindows;

    public GAbstractWindow log;
    public GAbstractWindow status;
    public GAbstractWindow forms;

    public GetNavigatorInfoResult() {
    }

    public GetNavigatorInfoResult(GNavigatorElement root, ArrayList<GNavigatorWindow> navigatorWindows, List<GAbstractWindow> commonWindows) {
        this.root = root;
        this.navigatorWindows = navigatorWindows;

        log = commonWindows.get(0);
        status = commonWindows.get(1);
        forms = commonWindows.get(2);
    }
}
