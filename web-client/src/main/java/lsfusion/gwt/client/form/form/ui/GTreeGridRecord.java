package lsfusion.gwt.client.form.form.ui;

import lsfusion.gwt.shared.form.view.GGroupObject;
import lsfusion.gwt.shared.form.view.GPropertyDraw;
import lsfusion.gwt.shared.form.view.changes.GGroupObjectValue;

import java.util.HashMap;
import java.util.Map;

public class GTreeGridRecord extends GridDataRecord {
    private GGroupObject group;

    public GTreeGridRecord(GGroupObject group, GGroupObjectValue key, HashMap<GPropertyDraw, Object> values) {
        super(key);
        this.group = group;

        for (Map.Entry<GPropertyDraw, Object> e : values.entrySet()) {
            setAttribute(e.getKey().sID, e.getValue());
        }
    }

    public GGroupObject getGroup() {
        return group;
    }
}
