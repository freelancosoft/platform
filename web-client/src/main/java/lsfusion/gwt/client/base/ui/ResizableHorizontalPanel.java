package lsfusion.gwt.client.base.ui;

import com.google.gwt.user.client.ui.*;
import lsfusion.gwt.client.base.Dimension;

import static lsfusion.gwt.client.base.GwtClientUtils.calculateStackMaxPreferredSize;

public class ResizableHorizontalPanel extends HorizontalPanel implements RequiresResize, ProvidesResize, HasMaxPreferredSize {
    @Override
    public void onResize() {
        if (!visible) {
            return;
        }
        for (Widget child : this) {
            if (child instanceof RequiresResize) {
                ((RequiresResize) child).onResize();
            }
        }
    }

    boolean visible = true;
    @Override
    public void setVisible(boolean visible) {
        if (this.visible != visible) {
            this.visible = visible;
            super.setVisible(visible);
        }
    }

    @Override
    public Dimension getMaxPreferredSize() {
        return calculateStackMaxPreferredSize(this.iterator(), false);
    }
}
