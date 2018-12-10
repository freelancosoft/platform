package lsfusion.gwt.shared.form.view.classes.link;

import lsfusion.gwt.client.form.MainFrameMessages;
import lsfusion.gwt.shared.form.view.classes.GTypeVisitor;
import lsfusion.gwt.client.form.grid.editor.GridCellEditor;

public class GJSONLinkType extends GLinkType {
    @Override
    public String toString() {
        return MainFrameMessages.Instance.get().typeJSONFileLinkCaption();
    }

    @Override
    public GridCellEditor visit(GTypeVisitor visitor) {
        return (GridCellEditor) visitor.visit(this);
    }
}