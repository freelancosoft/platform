package lsfusion.gwt.client.base.view;

import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Widget;
import lsfusion.gwt.client.base.Dimension;
import lsfusion.gwt.client.base.EscapeUtils;
import lsfusion.gwt.client.base.GwtClientUtils;

public class CaptionPanel extends FlexPanel {
    protected final Label legend;
    protected final DivWidget centeredLineWidget;

    public CaptionPanel(String caption) {
        super(true);

        addStyleName("captionPanel");

        FlexPanel legendWrapper = new FlexPanel(true);
        legendWrapper.setStyleName("captionLegendContainerPanel");
        GwtClientUtils.setZeroZIndex(legendWrapper.getElement()); // since in captionCenteredLine we're using -1 z-index (we can't set captionPanelLegend z-index 1 since it will be above dialogs blocking masks)

        legend = new Label();
        legend.setStyleName("captionPanelLegend");
        legendWrapper.add(legend);

        centeredLineWidget = new DivWidget();
        centeredLineWidget.setStyleName("captionCenteredLine");
        legendWrapper.add(centeredLineWidget);

        add(legendWrapper, GFlexAlignment.STRETCH);

        assert caption != null;
        setCaption(caption);
    }
    public CaptionPanel(String caption, Widget content) {
        this(caption);

        addFillFlex(content, null);
    }

    private boolean notNullCaption;
    private boolean notEmptyCaption;
    public void setCaption(String caption) {
        legend.setText(EscapeUtils.unicodeEscape(caption != null ? caption : ""));

        // incremental update
        boolean notNullCaption = caption != null;
        if(this.notNullCaption != notNullCaption) {
            if(notNullCaption)
                legend.addStyleName("notNullCaptionPanelLegend");
            else
                legend.removeStyleName("notNullCaptionPanelLegend");
            centeredLineWidget.setVisible(notNullCaption);
            this.notNullCaption = notNullCaption;
        }

        boolean notEmptyCaption = caption != null && !caption.isEmpty();
        if(this.notEmptyCaption != notEmptyCaption) {
            if(notEmptyCaption)
                legend.addStyleName("notEmptyCaptionPanelLegend");
            else
                legend.removeStyleName("notEmptyCaptionPanelLegend");
            this.notEmptyCaption = notEmptyCaption;
        }
    }
}
