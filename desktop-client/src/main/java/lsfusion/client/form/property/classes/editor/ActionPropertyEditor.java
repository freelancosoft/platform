package lsfusion.client.form.property.classes.editor;

import lsfusion.client.form.property.cell.PropertyTableCellEditor;
import lsfusion.client.form.property.classes.renderer.ActionPropertyRenderer;
import lsfusion.client.form.property.ClientPropertyDraw;

import java.awt.*;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.util.EventObject;

public class ActionPropertyEditor implements PropertyEditor {
    private ActionPropertyRenderer editorComponent;
    private PropertyTableCellEditor tableEditor;

    public ActionPropertyEditor(ClientPropertyDraw property) {
        //рисуем эдитор так же, как рендерер
        editorComponent = new ActionPropertyRenderer(property);
        editorComponent.updateRenderer(true, true, true);

        editorComponent.getComponent().addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                tableEditor.stopCellEditingLater();
            }
        });
    }

    public void setTableEditor(PropertyTableCellEditor tableEditor) {
        this.tableEditor = tableEditor;
    }

    public Component getComponent(Point tableLocation, Rectangle cellRectangle, EventObject editEvent) {
        return editorComponent.getComponent();
    }

    public Object getCellEditorValue() {
        return true;
    }

    @Override
    public boolean stopCellEditing() {
        return true;
    }
}
