package platform.gwt.form.shared.view.grid.renderer;

import com.google.gwt.dom.client.DivElement;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Style;
import com.google.gwt.dom.client.TextAreaElement;
import platform.gwt.base.client.EscapeUtils;
import platform.gwt.cellview.client.cell.Cell;
import platform.gwt.form.shared.view.GPropertyDraw;

public class TextGridCellRenderer extends AbstractGridCellRenderer {
    private GPropertyDraw property;

    public TextGridCellRenderer(GPropertyDraw property) {
        this.property = property;
    }

    @Override
    public void renderDom(platform.gwt.cellview.client.cell.Cell.Context context, DivElement cellElement, Object value) {
        Style divStyle = cellElement.getStyle();
        divStyle.setPaddingRight(4, Style.Unit.PX);
        divStyle.setPaddingLeft(4, Style.Unit.PX);
        divStyle.setHeight(100, Style.Unit.PCT);
        divStyle.setProperty("lineHeight", "normal"); // избегаем наследования от td,
                                                      // ибо в случае с textarea внутри это приводит к увеличению высоты

        TextAreaElement textArea = cellElement.appendChild(Document.get().createTextAreaElement());
        textArea.setTabIndex(-1);

        Style textareaStyle = textArea.getStyle();
        textareaStyle.setBorderWidth(0, Style.Unit.PX);
        textareaStyle.setBackgroundColor("transparent");
        textareaStyle.setPadding(0, Style.Unit.PX);
        textareaStyle.setWidth(100, Style.Unit.PCT);
        textareaStyle.setHeight(100, Style.Unit.PCT);
        textareaStyle.setOverflowY(Style.Overflow.HIDDEN);
        textareaStyle.setProperty("pointerEvents", "none");
        textareaStyle.setProperty("resize", "none");

        if (property.font != null) {
            textareaStyle.setProperty("font", property.font.getFullFont());
        }

        updateTextArea(textArea, value);
    }

    @Override
    public void updateDom(DivElement cellElement, Cell.Context context, Object value) {
        updateTextArea(cellElement.getFirstChild().<TextAreaElement>cast(), value);
    }

    private void updateTextArea(TextAreaElement textArea, Object value) {
        if (value == null) {
            textArea.setValue(EscapeUtils.UNICODE_NBSP);
            textArea.getParentElement().setTitle("");
        } else {
            textArea.setValue((String) value);
            textArea.getParentElement().setTitle((String) value);
        }
    }
}