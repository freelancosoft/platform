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

    @Override
    public Dimension getMaxPreferredSize() {
        return adjustMaxPreferredSize(GwtClientUtils.calculateMaxPreferredSize(getWidget(1))); // assuming that there are only 2 widgets, and the second is the main widget
    }

    public Dimension adjustMaxPreferredSize(Dimension dimension) {
        return GwtClientUtils.enlargeDimension(dimension,
                2,
                GwtClientUtils.getAllMargins(getElement()));
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
