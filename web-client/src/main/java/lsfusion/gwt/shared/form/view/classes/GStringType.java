package lsfusion.gwt.shared.form.view.classes;

import lsfusion.gwt.client.form.MainFrameMessages;
import lsfusion.gwt.shared.form.view.GExtInt;
import lsfusion.gwt.shared.form.view.GFont;
import lsfusion.gwt.shared.form.view.GPropertyDraw;
import lsfusion.gwt.shared.form.view.filter.GCompare;
import lsfusion.gwt.client.form.grid.EditManager;
import lsfusion.gwt.client.form.grid.editor.AbstractGridCellEditor;
import lsfusion.gwt.client.form.grid.editor.GridCellEditor;
import lsfusion.gwt.client.form.grid.renderer.GridCellRenderer;
import lsfusion.gwt.client.form.grid.renderer.StringGridCellRenderer;
import lsfusion.gwt.client.form.grid.renderer.TextGridCellRenderer;

import java.text.ParseException;

import static java.lang.Math.pow;
import static java.lang.Math.round;

public class GStringType extends GDataType {

    public boolean blankPadded;
    public boolean caseInsensitive;
    public boolean rich;
    protected GExtInt length = new GExtInt(50);

    @Override
    public GCompare[] getFilterCompares() {
        return GCompare.values();
    }

    @Override
    public Object parseString(String s, String pattern) throws ParseException {
        return s;
    }

    @Override
    public GCompare getDefaultCompare() {
        return caseInsensitive ? GCompare.CONTAINS : GCompare.EQUALS;
    }

    public GStringType() {}

    public GStringType(int length) {
        this(new GExtInt(length), false, true, false);
    }

    public GStringType(GExtInt length, boolean caseInsensitive, boolean blankPadded, boolean rich) {

        this.blankPadded = blankPadded;
        this.caseInsensitive = caseInsensitive;
        this.rich = rich;
        this.length = length;
    }

    @Override
    public int getDefaultCharWidth() {
        if(length.isUnlimited()) {
            return 15;
        } else {
            int lengthValue = length.getValue();
            return lengthValue <= 12 ? lengthValue : (int) round(12 + pow(lengthValue - 12, 0.7));
        }
    }

    @Override
    public int getDefaultHeight(GFont font) {
        if (length.isUnlimited()) {
            return super.getDefaultHeight(font) * 4;
        }
        return super.getDefaultHeight(font);
    }

    @Override
    public GridCellRenderer createGridCellRenderer(GPropertyDraw property) {
        if (length.isUnlimited()) {
            return new TextGridCellRenderer(property, rich);
        }
        return new StringGridCellRenderer(property, !blankPadded);
    }

    @Override
    public GridCellEditor visit(GTypeVisitor visitor) {
        return (GridCellEditor) visitor.visit(this);
    }

    @Override
    public GridCellEditor createGridCellEditor(EditManager editManager, GPropertyDraw editProperty) {
        return AbstractGridCellEditor.createGridCellEditor(this, editManager, editProperty, length, rich, blankPadded);
    }

    @Override
    public String toString() {
        return MainFrameMessages.Instance.get().typeStringCaption() + 
                (caseInsensitive ? " " + MainFrameMessages.Instance.get().typeStringCaptionRegister() : "") + 
                (blankPadded ? " " + MainFrameMessages.Instance.get().typeStringCaptionPadding() : "") + 
                (rich ? " rich" : "") + 
                "(" + length + ")";
    }
}
