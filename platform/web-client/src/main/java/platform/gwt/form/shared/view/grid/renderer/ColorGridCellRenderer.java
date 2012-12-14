package platform.gwt.form.shared.view.grid.renderer;

import com.google.gwt.dom.client.DivElement;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Style;
import platform.gwt.base.client.EscapeUtils;
import platform.gwt.cellview.client.cell.Cell;

public class ColorGridCellRenderer extends AbstractGridCellRenderer {

    @Override
    public void renderDom(Cell.Context context, DivElement cellElement, Object value) {
        String color = getColorValue(value);

        DivElement div = cellElement.appendChild(Document.get().createDivElement());
        Style divStyle = div.getStyle();
        divStyle.setHeight(16, Style.Unit.PX);
        divStyle.setBorderColor("black");
        divStyle.setBorderWidth(0, Style.Unit.PX);
        divStyle.setColor(color);
        divStyle.setBackgroundColor(color);
        divStyle.setProperty("minHeight", 16, Style.Unit.PX);
        div.setInnerText(EscapeUtils.UNICODE_NBSP);
    }

    @Override
    public void updateDom(DivElement cellElement, Cell.Context context, Object value) {
        String color = getColorValue(value);

        DivElement div = cellElement.getFirstChild().cast();
        div.getStyle().setColor(color);
        div.getStyle().setBackgroundColor(color);
    }

    private String getColorValue(Object value) {
        return value == null ? "" : value.toString();
    }
}
