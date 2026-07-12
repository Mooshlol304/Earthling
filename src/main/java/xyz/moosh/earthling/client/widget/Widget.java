/*
 * Earthling
 * Copyright (c) 2025 Moosh
 *
 * Earthling is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * Earthling is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Earthling. If not, see
 * <https://www.gnu.org/licenses/>.
 */

package xyz.moosh.earthling.client.widget;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import xyz.moosh.earthling.client.api.IWidget;
import xyz.moosh.earthling.client.config.ConfigGroup;
import xyz.moosh.earthling.client.config.ConfigOption;

/**
 * Base class for all HUD widgets.
 * Positions are stored as fractions (0.0–1.0) of the GUI-scaled screen size
 * so they stay in the same relative position after any window resize.
 *
 * getPosition() self-heals: if a saved config value is outside 0–1 (e.g. old
 * integer pixel values like 4.0 or 40.0), it resets to the constructor default.
 */
public abstract class Widget implements IWidget {

    protected final String      id;
    protected final String      title;
    protected final ConfigGroup config;

    private final ConfigOption<Boolean> visibleOption;
    private final ConfigOption<Float>   scaleOption;
    private final ConfigOption<Float>   posXFrac;
    private final ConfigOption<Float>   posYFrac;

    private final WidgetPosition cachedPosition = new WidgetPosition(0, 0);
    private boolean              visible        = true;

    protected Widget(String id, String title) {
        this(id, title, 0.02f, 0.05f);
    }

    protected Widget(String id, String title, float defaultXFrac, float defaultYFrac) {
        this.id    = id;
        this.title = title;
        config     = new ConfigGroup(id);

        visibleOption = config.addToggle("visible", "Visible", true)
                .onChange(v -> this.visible = v);

        scaleOption = config.addSlider("scale", "Scale", 1.0f, 0.5f, 2.0f);

        posXFrac = config.addSlider("pos_x_frac", "X Position", defaultXFrac, 0f, 1f);
        posYFrac = config.addSlider("pos_y_frac", "Y Position", defaultYFrac, 0f, 1f);
    }

    @Override public abstract int  getWidth();
    @Override public abstract int  getHeight();
    @Override public abstract void render(GuiGraphics g, float tickDelta);
    @Override public abstract void tick();

    @Override public String      getId()     { return id; }
    @Override public String      getTitle()  { return title; }
    @Override public float       getScale()  { return scaleOption.get(); }
    @Override public boolean     isVisible() { return visible; }
    @Override public ConfigGroup getConfig() { return config; }

    @Override
    public WidgetPosition getPosition() {
        int sw = Minecraft.getInstance().getWindow().getGuiScaledWidth();
        int sh = Minecraft.getInstance().getWindow().getGuiScaledHeight();

        float fx = posXFrac.get();
        float fy = posYFrac.get();

        // Self-heal: if saved value is outside 0–1 (e.g. old integer pixels were
        // accidentally saved as fractions like 4.0 or 40.0), reset to the default.
        if (fx < 0f || fx > 1f) {
            fx = posXFrac.getDefaultValue();
            posXFrac.set(fx);
        }
        if (fy < 0f || fy > 1f) {
            fy = posYFrac.getDefaultValue();
            posYFrac.set(fy);
        }

        cachedPosition.setX((int)(fx * sw));
        cachedPosition.setY((int)(fy * sh));
        return cachedPosition;
    }

    @Override
    public void setVisible(boolean visible) {
        this.visible = visible;
        visibleOption.set(visible);
    }

    /**
     * Move the widget to pixel coordinates within a screen of the given size.
     * Converts to fractions and persists through config.
     */
    public void setPositionPixels(int x, int y, int screenW, int screenH) {
        float fx = screenW > 0 ? (float) x / screenW : 0f;
        float fy = screenH > 0 ? (float) y / screenH : 0f;
        posXFrac.set(Math.max(0f, Math.min(1f, fx)));
        posYFrac.set(Math.max(0f, Math.min(1f, fy)));
    }
}