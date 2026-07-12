package xyz.moosh.earthling.client.api;

import net.minecraft.client.gui.GuiGraphics;
import xyz.moosh.earthling.client.config.ConfigGroup;
import xyz.moosh.earthling.client.widget.WidgetPosition;

public interface IWidget {
    String         getId();
    String         getTitle();
    int            getWidth();
    int            getHeight();
    WidgetPosition getPosition();
    float          getScale();
    boolean        isVisible();
    void           setVisible(boolean visible);
    ConfigGroup    getConfig();
    void           render(GuiGraphics guiGraphics, float tickDelta);
    void           tick();
}