package lsfusion.gwt.shared.form.actions.form;

import lsfusion.gwt.shared.base.actions.NumberResult;

public class CountRecords extends FormRequestIndexCountingAction<NumberResult> {
    public int groupObjectID;

    public CountRecords() {}

    public CountRecords(int groupObjectID) {
        this.groupObjectID = groupObjectID;
    }
}
