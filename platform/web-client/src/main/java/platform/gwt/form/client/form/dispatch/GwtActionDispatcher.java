package platform.gwt.form.client.form.dispatch;

import com.allen_sauer.gwt.log.client.Log;
import com.google.gwt.user.client.rpc.AsyncCallback;
import platform.gwt.base.client.GwtClientUtils;
import platform.gwt.form.client.ErrorHandlingCallback;
import platform.gwt.form.client.form.ui.dialog.DialogBoxHelper;
import platform.gwt.form.client.log.GLog;
import platform.gwt.form.shared.actions.form.ServerResponseResult;
import platform.gwt.form.shared.view.actions.*;

public abstract class GwtActionDispatcher implements GActionDispatcher {
    private boolean dispatchingPaused;

    private ServerResponseResult currentResponse = null;
    Object[] currentActionResults = null;
    private int currentActionIndex = -1;

    public void dispatchResponse(ServerResponseResult response) {
        assert response != null;

        try {
            Object[] actionResults = null;
            GAction[] actions = response.actions;
            if (actions != null) {
                int beginIndex;
                if (dispatchingPaused) {
                    beginIndex = currentActionIndex + 1;
                    actionResults = currentActionResults;

                    currentActionIndex = -1;
                    currentActionResults = null;
                    currentResponse = null;
                    dispatchingPaused = false;
                } else {
                    beginIndex = 0;
                    actionResults = new Object[actions.length];
                }

                for (int i = beginIndex; i < actions.length; i++) {
                    GAction action = actions[i];
                    Object dispatchResult;
                    try {
                        //для неподдерживаемых action'ов присылается null-ссылка, чтобы сохранить порядок результатов выполнения action'ов
                        dispatchResult = action == null ? null : action.dispatch(this);
                    } catch (Exception ex) {
                        Log.error("Error dispatching gwt client action: ", ex);
                        throwInServerInvocation(ex);
                        break;
                    }

                    if (dispatchingPaused) {
                        currentResponse = response;
                        currentActionResults = actionResults;
                        currentActionIndex = i;
                        return;
                    }

                    actionResults[i] = dispatchResult;
                }
            }

            if (response.resumeInvocation) {
                continueServerInvocation(actionResults, new ErrorHandlingCallback<ServerResponseResult>() {
                    @Override
                    public void success(ServerResponseResult response) {
                        dispatchResponse(response);
                    }
                });
            } else {
                postDispatchResponse(response);
            }
        } catch (Exception e) {
            handleDispatchException(e);
        }
    }

    protected void postDispatchResponse(ServerResponseResult response) {
        assert !response.resumeInvocation && !response.pendingRemoteChanges;
    }

    protected void handleDispatchException(Exception e) {
        DialogBoxHelper.showMessageBox(true, "Error", e.getMessage(), null);
        Log.error("Error dispatching ServerResponseResult: ", e);
    }

    protected abstract void throwInServerInvocation(Exception ex);

    protected abstract void continueServerInvocation(Object[] actionResults, AsyncCallback<ServerResponseResult> callback) ;

    protected final void pauseDispatching() {
        dispatchingPaused = true;
    }

    public void continueDispatching() {
        continueDispatching(null);
    }

    public void continueDispatching(Object currentActionResult) {
        currentActionResults[currentActionIndex] = currentActionResult;
        dispatchResponse(currentResponse);
    }

    @Override
    public void execute(GFormAction action) {
    }

    @Override
    public void execute(GReportAction action) {
    }

    public void execute(GDialogAction action) {
    }
        @Override
    public Object execute(GChooseClassAction action) {
        return null;
    }

    @Override
    public void execute(GMessageAction action) {
        pauseDispatching();
        DialogBoxHelper.showMessageBox(false, action.caption, action.message, new DialogBoxHelper.CloseCallback() {
            @Override
            public void closed(DialogBoxHelper.OptionType chosenOption) {
                continueDispatching();
            }
        });
    }

    @Override
    public int execute(GConfirmAction action) {
        return 0;
    }

    @Override
    public void execute(GLogMessageAction action) {
        if (action.failed) {
            GLog.error(action.message, action.data, action.titles);
        } else {
            GLog.message(action.message);
        }
    }

    @Override
    public void execute(GRunPrintReportAction action) {
    }

    @Override
    public void execute(GRunOpenInExcelAction action) {
    }

    @Override
    public void execute(GHideFormAction action) {
    }

    @Override
    public void execute(GProcessFormChangesAction action) {
    }

    @Override
    public Object execute(GRequestUserInputAction action) {
        return null;
    }

    @Override
    public void execute(GAsyncResultAction action) {
    }

    @Override
    public void execute(GLogOutAction action) {
        GwtClientUtils.logout();
    }
}
