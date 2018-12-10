package lsfusion.gwt.server.form.form.handlers;

import lsfusion.gwt.server.form.spring.LSFusionDispatchServlet;
import lsfusion.gwt.server.form.form.spring.FormSessionObject;
import lsfusion.gwt.server.form.convert.GwtToClientConverter;
import lsfusion.gwt.server.form.form.FormServerResponseActionHandler;
import lsfusion.gwt.shared.form.actions.form.SaveUserPreferencesAction;
import lsfusion.gwt.shared.form.actions.form.ServerResponseResult;
import lsfusion.gwt.shared.form.view.GColumnUserPreferences;
import lsfusion.gwt.shared.form.view.GGroupObjectUserPreferences;
import lsfusion.interop.form.ColumnUserPreferences;
import lsfusion.interop.form.GroupObjectUserPreferences;
import net.customware.gwt.dispatch.server.ExecutionContext;
import net.customware.gwt.dispatch.shared.DispatchException;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class SaveUserPreferencesActionHandler extends FormServerResponseActionHandler<SaveUserPreferencesAction> {
    private final static GwtToClientConverter gwtConverter = GwtToClientConverter.getInstance();

    public SaveUserPreferencesActionHandler(LSFusionDispatchServlet servlet) {
        super(servlet);
    }

    @Override
    public ServerResponseResult executeEx(SaveUserPreferencesAction action, ExecutionContext context) throws DispatchException, IOException {
        FormSessionObject form = getFormSessionObject(action.formSessionID);
        GGroupObjectUserPreferences gGroupObjectUP = action.groupObjectUserPreferences;
        
        HashMap<String, ColumnUserPreferences> columnUPMap = new HashMap<>();
        for (Map.Entry<String, GColumnUserPreferences> entry : gGroupObjectUP.getColumnUserPreferences().entrySet()) {
            GColumnUserPreferences gColumnUP = entry.getValue();
            columnUPMap.put(entry.getKey(), new ColumnUserPreferences(gColumnUP.userHide, gColumnUP.userCaption, gColumnUP.userPattern, gColumnUP.userWidth, gColumnUP.userOrder, gColumnUP.userSort, gColumnUP.userAscendingSort));
        }
        GroupObjectUserPreferences groupObjectUP = new GroupObjectUserPreferences(columnUPMap, gGroupObjectUP.getGroupObjectSID(), gwtConverter.convertFont(gGroupObjectUP.getFont()), gGroupObjectUP.getPageSize(), gGroupObjectUP.getHeaderHeight(), gGroupObjectUP.hasUserPreferences());

        return getServerResponseResult(form, form.remoteForm.saveUserPreferences(action.requestIndex, defaultLastReceivedRequestIndex, groupObjectUP, action.forAllUsers, action.completeOverride, action.hiddenProps));
    }
}
