package lsfusion.gwt.base.client.ui;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.DivElement;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Style;
import com.google.gwt.user.client.DOM;

import static lsfusion.gwt.base.client.ui.FlexPanel.Justify;

public class FlexPanelImpl {

    private static FlexPanelImpl impl;

    public static FlexPanelImpl get() {
        if (impl == null) {
            impl = GWT.create(FlexPanelImpl.class);
        }
        return impl;
    }

    protected String getDisplayFlexValue() {
        return "flex";
    }

    protected String getDirectionAttrName() {
        return "flexDirection";
    }

    protected String getDirectionValue(boolean vertical) {
        return vertical ? getVertDirectionValue() : getHorzDirectionValue();
    }

    protected String getHorzDirectionValue() {
        return "row";
    }

    protected String getVertDirectionValue() {
        return "column";
    }

    protected String getJustifyContentAttrName() {
        return "justifyContent";
    }

    protected String getJustifyValue(Justify justify) {
        switch (justify) {
            case LEADING: return getLeadingAlignmentValue();
            case CENTER: return getCenterAlignmentValue();
            case TRAILING: return getTrailingAlignmentValue();
        }
        throw new IllegalStateException("Unknown alignment");
    }

    protected String getLeadingAlignmentValue() {
        return "flex-start";
    }

    protected String getCenterAlignmentValue() {
        return "center";
    }

    protected String getTrailingAlignmentValue() {
        return "flex-end";
    }

    protected String getStretchAlignmentValue() {
        return "stretch";
    }

    protected String getAlignAttrName() {
        return "alignSelf";
    }

    protected String getAlignmentValue(GFlexAlignment justify) {
        switch (justify) {
            case LEADING: return getLeadingAlignmentValue();
            case CENTER: return getCenterAlignmentValue();
            case TRAILING: return getTrailingAlignmentValue();
            case STRETCH: return getStretchAlignmentValue();
        }
        throw new IllegalStateException("Unknown alignment");
    }

    protected String getFlexAttrName() {
        return "flex";
    }

    public void setupParentDiv(DivElement parent, boolean vertical, Justify justify) {
        parent.getStyle().setOverflow(Style.Overflow.HIDDEN);
        parent.getStyle().setProperty("display", getDisplayFlexValue());
        parent.getStyle().setProperty(getDirectionAttrName(), getDirectionValue(vertical));
        parent.getStyle().setProperty(getJustifyContentAttrName(), getJustifyValue(justify));
    }

    public void setVisible(DivElement parent, boolean visible) {
        parent.getStyle().setProperty("display", visible ? getDisplayFlexValue() : "none");
    }

    public FlexPanel.LayoutData insertChild(Element parent, Element child, int beforeIndex, GFlexAlignment alignment, double flex, String flexBasis) {
        FlexPanel.LayoutData layoutData = new FlexPanel.LayoutData(child, alignment, flex, flexBasis);

        setFlex(layoutData, child, flex, flexBasis);
        setAlignment(layoutData, child, alignment);

        DOM.insertChild(parent.<com.google.gwt.user.client.Element>cast(), child.<com.google.gwt.user.client.Element>cast(), beforeIndex);

        return layoutData;
    }

    public void removeChild(FlexPanel.LayoutData layoutData) {
        layoutData.child.removeFromParent();
    }

    public void setFlex(FlexPanel.LayoutData layoutData, Element child, double flex, String flexBasis) {
        layoutData.flex = flex;
        layoutData.flexBasis = flexBasis;
        child.getStyle().setProperty(getFlexAttrName(), getFlexValue(flex, flexBasis));
    }

    private String getFlexValue(double flex, String flexBasis) {
        return flex > 0
               ? flex + " 0 " + flexBasis
               : "0 0 auto";
    }

    public void setAlignment(FlexPanel.LayoutData layoutData, Element child, GFlexAlignment alignment) {
        layoutData.alignment = alignment;
        child.getStyle().setProperty(getAlignAttrName(), getAlignmentValue(alignment));
    }
}
