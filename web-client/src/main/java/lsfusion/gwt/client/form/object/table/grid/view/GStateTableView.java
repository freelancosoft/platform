package lsfusion.gwt.client.form.object.table.grid.view;

import com.google.gwt.core.client.*;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Style;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Widget;
import lsfusion.gwt.client.ClientMessages;
import lsfusion.gwt.client.base.GwtSharedUtils;
import lsfusion.gwt.client.base.Pair;
import lsfusion.gwt.client.base.jsni.NativeHashMap;
import lsfusion.gwt.client.base.view.DivWidget;
import lsfusion.gwt.client.base.view.FlexPanel;
import lsfusion.gwt.client.base.view.ResizableSimplePanel;
import lsfusion.gwt.client.base.view.SimpleImageButton;
import lsfusion.gwt.client.form.controller.GFormController;
import lsfusion.gwt.client.form.design.GFont;
import lsfusion.gwt.client.form.object.GGroupObjectValue;
import lsfusion.gwt.client.form.object.table.grid.controller.GGridController;
import lsfusion.gwt.client.form.object.table.grid.user.design.GGroupObjectUserPreferences;
import lsfusion.gwt.client.form.object.table.view.GGridPropertyTable;
import lsfusion.gwt.client.form.property.GPropertyDraw;
import lsfusion.gwt.client.form.view.Column;
import lsfusion.gwt.client.view.MainFrame;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;

// view with state, without incremental updates
// flexpanel, since we need to add pagesize widget + attach it to handle events
public abstract class GStateTableView extends FlexPanel implements GTableView {

    protected final GFormController form;
    protected final GGridController grid;

    protected GFont font;

    protected GGroupObjectValue currentKey;

    private long setRequestIndex;

    protected List<GGroupObjectValue> keys;

    protected List<GPropertyDraw> properties = new ArrayList<>();
    protected List<List<GGroupObjectValue>> columnKeys = new ArrayList<>();
    protected List<NativeHashMap<GGroupObjectValue, Object>> captions = new ArrayList<>();
    protected List<NativeHashMap<GGroupObjectValue, Object>> values = new ArrayList<>();
    protected List<List<NativeHashMap<GGroupObjectValue, Object>>> lastAggrs = new ArrayList<>();
    protected List<NativeHashMap<GGroupObjectValue, Object>> readOnlys = new ArrayList<>();
    protected List<NativeHashMap<GGroupObjectValue, Object>> showIfs = new ArrayList<>();
    protected NativeHashMap<GGroupObjectValue, Object> rowBackgroundValues = new NativeHashMap<>();
    protected NativeHashMap<GGroupObjectValue, Object> rowForegroundValues = new NativeHashMap<>();
    protected List<NativeHashMap<GGroupObjectValue, Object>> cellBackgroundValues = new ArrayList<>();
    protected List<NativeHashMap<GGroupObjectValue, Object>> cellForegroundValues = new ArrayList<>();

    protected boolean checkShowIf(int property, GGroupObjectValue columnKey) {
        NativeHashMap<GGroupObjectValue, Object> propertyShowIfs = showIfs.get(property);
        return propertyShowIfs != null && propertyShowIfs.get(columnKey) == null;
    }

    public final native JsArrayMixed clone(JsArrayMixed array) /*-{
        n = array.length;
        var clone = [];
        for(var i = 0; i < n; i++) {
            clone.push(array[i]);
        }
        return clone;
    }-*/;

    public GStateTableView(GFormController form, GGridController grid) {
        super(true);

        this.form = form;
        this.grid = grid;

//        setElement(DOM.createDiv());

        rerender = true;

        drawWidget = new DivWidget();
        addFill(drawWidget);

        initPageSizeWidget();
    }

    private final Label messageLabel = new Label();

    public void initPageSizeWidget() {
        FlexPanel messageAndButton = new FlexPanel();
        messageLabel.getElement().getStyle().setPaddingRight(4, Style.Unit.PX);
        messageAndButton.addCentered(messageLabel);

        SimpleImageButton showAllButton = new SimpleImageButton(ClientMessages.Instance.get().formGridPageSizeShowAll());
        showAllButton.addClickHandler(event -> {
            pageSize = Integer.MAX_VALUE / 10; // /10 to prevent Integer overflow because in GroupObjectInstance we use "pageSize * 2"
            this.grid.changePageSize(pageSize);
        });
        messageAndButton.addCentered(showAllButton);

        FlexPanel centeredMessageAndButton = new FlexPanel(true);
        centeredMessageAndButton.addCentered(messageAndButton);
        centeredMessageAndButton.getElement().getStyle().setPadding(2, Style.Unit.PX);

        this.pageSizeWidget = centeredMessageAndButton;
        this.pageSizeWidget.setVisible(false);

        ResizableSimplePanel child = new ResizableSimplePanel();
        child.setWidget(this.pageSizeWidget);
        addStretched(child); // we need to attach pageSize widget to make it work
//
//        add(new ResizableSimplePanel(this.pageSizeWidget)); // we need to attach pageSize widget to make it work
    }

    private final Widget drawWidget;
    protected Element getDrawElement() {
        return drawWidget.getElement();
    }

    private Widget pageSizeWidget;
    protected Widget getPageSizeWidget() {
        return pageSizeWidget;
    }

    protected boolean dataUpdated = false;

    @Override
    public void setCurrentKey(GGroupObjectValue currentKey) {
        setCurrentKey(currentKey, true);
    }

    private void setCurrentKey(GGroupObjectValue currentKey, boolean rendered) {
        this.currentKey = currentKey;
        if (!rendered)
            dataUpdated = true;
    }

    private int pageSize = getDefaultPageSize();

    // should correspond FormInstance.constructor - changePageSize method
    public int getDefaultPageSize() {
        return 1000;
    }

    public int getPageSize() {
        return pageSize;
    }

    public void setPageSize(int pageSize) {
        this.pageSize = pageSize;
    }

    protected boolean isPageSizeHit() {
        return keys != null && keys.size() >= getPageSize();
    }

    @Override
    public void setKeys(ArrayList<GGroupObjectValue> keys) {
        this.keys = keys;

        dataUpdated = true;
    }

    @Override
    public void updateProperty(GPropertyDraw property, ArrayList<GGroupObjectValue> columnKeys, boolean updateKeys, NativeHashMap<GGroupObjectValue, Object> values) {
        int index = properties.indexOf(property);
        if(!updateKeys) {
            if(index < 0) {
                index = GwtSharedUtils.relativePosition(property, form.getPropertyDraws(), this.properties);

                this.captions.add(index, null);
                this.properties.add(index, property);
                this.columnKeys.add(index, null);
                this.values.add(index, null);
                this.readOnlys.add(index, null);
                this.showIfs.add(index, null);
                this.cellBackgroundValues.add(index, null);
                this.cellForegroundValues.add(index, null);

                List<NativeHashMap<GGroupObjectValue, Object>> list = new ArrayList<>();
                for (int i = 0; i < property.lastReaders.size(); i++)
                    list.add(null);
                lastAggrs.add(index, list);
            }
            this.columnKeys.set(index, columnKeys);
        } else
            assert index >= 0;

        NativeHashMap<GGroupObjectValue, Object> valuesMap = this.values.get(index);
        if (updateKeys && valuesMap != null) {
            valuesMap.putAll(values);
        } else {
            NativeHashMap<GGroupObjectValue, Object> pvalues = new NativeHashMap<>();
            pvalues.putAll(values);
            this.values.set(index, pvalues);
        }

        dataUpdated = true;
    }

    @Override
    public void updatePropertyCaptions(GPropertyDraw property, NativeHashMap<GGroupObjectValue, Object> values) {
        this.captions.set(properties.indexOf(property), values);

        dataUpdated = true;
    }

    @Override
    public void updateLoadings(GPropertyDraw propertyDraw, NativeHashMap<GGroupObjectValue, Object> values) {
    }

    @Override
    public void updatePropertyFooters(GPropertyDraw property, NativeHashMap<GGroupObjectValue, Object> values) {
    }

    @Override
    public void updateLastValues(GPropertyDraw property, int index, NativeHashMap<GGroupObjectValue, Object> values) {
        this.lastAggrs.get(properties.indexOf(property)).set(index, values);

        dataUpdated = true;
    }

    @Override
    public void removeProperty(GPropertyDraw property) {
        int index = properties.indexOf(property);
        properties.remove(index);
        columnKeys.remove(index);
        captions.remove(index);
        lastAggrs.remove(index);
        values.remove(index);
        readOnlys.remove(index);
        showIfs.remove(index);

        dataUpdated = true;
    }

    private boolean rerender;
    protected void rerender() {
        rerender = true;
    }
    private boolean updateState;

    public void updateView(boolean dataUpdated, Boolean updateState) {
        if(updateState != null)
            this.updateState = updateState;

        if(dataUpdated || rerender) {
            updateView();
            updatePageSizeState(isPageSizeHit());
            rerender = false;
        }

        updateRendererState(this.updateState); // update state with server response
    }

    protected abstract void updateView();
    protected void updatePageSizeState(boolean hit) {
        messageLabel.setText(ClientMessages.Instance.get().formGridPageSizeHit(keys == null ? getPageSize() : keys.size() )); //need to show current objects size
        getPageSizeWidget().setVisible(hit);
    }
    protected abstract Element getRendererAreaElement();

    private long lastRendererDropped = 0;
    protected void updateRendererState(boolean set) {
        Runnable setFilter = () -> setOpacity(set, getRendererAreaElement());

        if(set) {
            long wasRendererDropped = lastRendererDropped;
            Scheduler.get().scheduleFixedDelay(() -> {
                if(wasRendererDropped == lastRendererDropped) // since set and drop has different timeouts
                    setFilter.run();
                return false;
            }, (int) MainFrame.updateRendererStateSetTimeout);
        } else {
            lastRendererDropped++;
            setFilter.run();
        }
    }

    @Override
    public void update(Boolean updateState) {
        updateView(dataUpdated, updateState);

        dataUpdated = false;
    }

    @Override
    public boolean isNoColumns() {
        return properties.isEmpty();
    }

    @Override
    public long getSetRequestIndex() {
        return setRequestIndex;
    }

    @Override
    public void setSetRequestIndex(long index) {
        setRequestIndex = index;
    }

    // ignore for now
    @Override
    public void focusProperty(GPropertyDraw propertyDraw) {

    }

    @Override
    public boolean changePropertyOrders(LinkedHashMap<GPropertyDraw, Boolean> value, boolean alreadySet) {
        return false;
    }

    @Override
    public void updateRowBackgroundValues(NativeHashMap<GGroupObjectValue, Object> values) {
        rowBackgroundValues = values;
    }
    
    public Object getRowBackgroundColor(GGroupObjectValue key) {
        return rowBackgroundValues.get(key);
    }

    @Override
    public void updateRowForegroundValues(NativeHashMap<GGroupObjectValue, Object> values) {
        rowForegroundValues = values;
    }

    public Object getRowForegroundColor(GGroupObjectValue key) {
        return rowForegroundValues.get(key);
    }

    @Override
    public void updateCellBackgroundValues(GPropertyDraw propertyDraw, NativeHashMap<GGroupObjectValue, Object> values) {
        this.cellBackgroundValues.set(properties.indexOf(propertyDraw), values);

        this.dataUpdated = true;
    }

    @Override
    public void updateCellForegroundValues(GPropertyDraw propertyDraw, NativeHashMap<GGroupObjectValue, Object> values) {
        this.cellForegroundValues.set(properties.indexOf(propertyDraw), values);

        this.dataUpdated = true;
    }

    @Override
    public void updateImageValues(GPropertyDraw propertyDraw, NativeHashMap<GGroupObjectValue, Object> values) {

    }

    @Override
    public void updateShowIfValues(GPropertyDraw property, NativeHashMap<GGroupObjectValue, Object> values) {
        this.showIfs.set(properties.indexOf(property), values);

        this.dataUpdated = true;
    }

    @Override
    public void updateReadOnlyValues(GPropertyDraw propertyDraw, NativeHashMap<GGroupObjectValue, Object> values) {
        this.readOnlys.set(properties.indexOf(propertyDraw), values);

        this.dataUpdated = true;
    }

    @Override
    public GGroupObjectValue getSelectedKey() {
        return currentKey; // for executing actions used for wysiwyg
    }

    protected boolean isCurrentKey(GGroupObjectValue object){
        return Objects.equals(object, getSelectedKey());
    }

    @Override
    public GPropertyDraw getCurrentProperty() {
        if(!properties.isEmpty())
            return properties.get(0);
        return null;
    }

    @Override
    public GGroupObjectValue getCurrentColumnKey() {
        if(!properties.isEmpty())
            return columnKeys.get(0).get(0);
        return null;
    }

    @Override
    public int getSelectedRow() {
        return -1;
    }

    @Override
    public void modifyGroupObject(GGroupObjectValue key, boolean add, int position) {

    }

    @Override
    public void runGroupReport() {
    }

    @Override
    public Object getSelectedValue(GPropertyDraw property, GGroupObjectValue columnKey) {
        return null;
    }

    @Override
    public List<Pair<Column, String>> getFilterColumns() {
        List<Pair<Column, String>> result = new ArrayList<>();
        for(int i=0,size=properties.size();i<size;i++) {
            GPropertyDraw property = properties.get(i);
            NativeHashMap<GGroupObjectValue, Object> propertyCaptions = captions.get(i);
            List<GGroupObjectValue> columns = columnKeys.get(i);
            for (GGroupObjectValue column : columns)
                result.add(GGridPropertyTable.getFilterColumn(propertyCaptions, property, column));
        }
        return result;
    }

    @Override
    public boolean hasUserPreferences() {
        return false;
    }

    @Override
    public boolean containsProperty(GPropertyDraw property) {
        return properties.indexOf(property) >= 0;
    }

    @Override
    public LinkedHashMap<GPropertyDraw, Boolean> getUserOrders(List<GPropertyDraw> propertyDrawList) {
        throw new UnsupportedOperationException();
    }

    @Override
    public GGroupObjectUserPreferences getCurrentUserGridPreferences() {
        return null;
    }

    @Override
    public GGroupObjectUserPreferences getGeneralGridPreferences() {
        return null;
    }

    protected long changeGroupObject(GGroupObjectValue value, boolean rendered) {
        setCurrentKey(value, rendered);
        return form.changeGroupObject(grid.groupObject, value);
    }

    protected Object getJsValue(GPropertyDraw property, GGroupObjectValue rowKey, GGroupObjectValue columnKey) {
        return GSimpleStateTableView.convertToJSValue(property, getValue(property, GGroupObjectValue.getFullKey(rowKey, columnKey)));
    }

    protected Object getValue(GPropertyDraw property, GGroupObjectValue fullKey) {
        return values.get(properties.indexOf(property)).get(fullKey);
    }

    protected boolean isReadOnly(GPropertyDraw property, GGroupObjectValue rowKey, GGroupObjectValue columnKey) {
        if(property.isReadOnly())
            return true;

        NativeHashMap<GGroupObjectValue, Object> readOnlyValues = readOnlys.get(properties.indexOf(property));
        if(readOnlyValues == null)
            return false;

        return readOnlyValues.get(GGroupObjectValue.getFullKey(rowKey, columnKey)) != null;
    }

    protected Object getCellBackground(GPropertyDraw property, GGroupObjectValue rowKey, GGroupObjectValue columnKey) {
        NativeHashMap<GGroupObjectValue, Object> cellBackground = cellBackgroundValues.get(properties.indexOf(property));
        if(cellBackground == null)
            return null;

        return cellBackground.get(GGroupObjectValue.getFullKey(rowKey, columnKey));
    }

    protected Object getBackground(GPropertyDraw property, GGroupObjectValue rowKey, GGroupObjectValue columnKey) {
        Object cellBackground = getCellBackground(property, rowKey, columnKey);
        return cellBackground == null ? property.background : cellBackground;
    }

    protected Object getCellForeground(GPropertyDraw property, GGroupObjectValue rowKey, GGroupObjectValue columnKey) {
        NativeHashMap<GGroupObjectValue, Object> cellForeground = cellForegroundValues.get(properties.indexOf(property));
        if(cellForeground == null)
            return null;

        return cellForeground.get(GGroupObjectValue.getFullKey(rowKey, columnKey));
    }

    protected Object getForeground(GPropertyDraw property, GGroupObjectValue rowKey, GGroupObjectValue columnKey) {
        Object cellForeground = getCellForeground(property, rowKey, columnKey);
        return cellForeground == null ? property.foreground : cellForeground;
    }
    // utils

    protected JsArray<JavaScriptObject> convertToObjectsString(JsArray<JsArrayString> array) {
        JsArrayString columns = array.get(0);
        JsArray<JavaScriptObject> convert = JavaScriptObject.createArray().cast();
        for(int i=1;i<array.length();i++) {
            WrapperObject object = JavaScriptObject.createObject().cast();
            JsArrayString values = array.get(i);
            for(int j=0;j<columns.length();j++) {
                object.putValue(columns.get(j), fromString(values.get(j)));
            }
            convert.push(object);
        }
        return convert;
    }

    protected JsArray<JavaScriptObject> convertToObjectsMixed(JsArray<JsArray<JavaScriptObject>> array) {
        JsArray<JavaScriptObject> columns = array.get(0); // actually strings
        JsArray<JavaScriptObject> convert = JavaScriptObject.createArray().cast();
        for(int i=1;i<array.length();i++) {
            WrapperObject object = JavaScriptObject.createObject().cast();
            JsArray<JavaScriptObject> values = array.get(i);
            for(int j=0;j<columns.length();j++) {
                object.putValue(toString(columns.get(j)), values.get(j));
            }
            convert.push(object);
        }
        return convert;
    }

    protected static void setOpacity(boolean updateState, Element element) {
        if (updateState) {
            element.getStyle().setProperty("filter", "opacity(0.5)");
        } else {
            //there is a bug with position:fixed and opacity parameter
            element.getStyle().setProperty("filter", "");
        }
    }

    static class WrapperObject extends JavaScriptObject {
        protected WrapperObject() {
        }

        protected native final JsArrayString getKeys() /*-{
            return Object.keys(this);
        }-*/;
        protected native final JsArrayString getArrayString(String string) /*-{
            return this[string];
        }-*/;
        protected native final JsArrayInteger getArrayInteger(String string) /*-{
            return this[string];
        }-*/;
        protected native final JsArrayMixed getArrayMixed(String string) /*-{
            return this[string];
        }-*/;
        protected native final void putValue(String key, JavaScriptObject object) /*-{
            this[key] = object;
        }-*/;
        protected native final JavaScriptObject getValue(String key) /*-{
            return this[key];
        }-*/;
    }

    protected static native boolean hasKey(JavaScriptObject object, String key) /*-{
        return object[key] !== undefined;
    }-*/;
    protected static native JavaScriptObject getValue(JavaScriptObject object, String key) /*-{
        return object[key];
    }-*/;

    protected static native JavaScriptObject fromString(String string) /*-{
        return string;
    }-*/;
    protected static native String toString(JavaScriptObject string) /*-{
        return string;
    }-*/;
    protected static native JavaScriptObject fromDouble(double d) /*-{
        return d;
    }-*/;
    protected static native double toDouble(JavaScriptObject d) /*-{
        return d;
    }-*/;
    protected static native JavaScriptObject fromBoolean(boolean b) /*-{
        return b;
    }-*/;
    protected static native boolean toBoolean(JavaScriptObject b) /*-{
        return b;
    }-*/;
    protected static native <T> JavaScriptObject fromObject(T object) /*-{
        return object;
    }-*/;
    protected static native <T> T toObject(JavaScriptObject object) /*-{
        return object;
    }-*/;
}
