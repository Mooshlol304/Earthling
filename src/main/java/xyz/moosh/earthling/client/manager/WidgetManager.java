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

package xyz.moosh.earthling.client.manager;

import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import org.joml.Matrix3x2fStack;
import xyz.moosh.earthling.client.event.EventBus;
import xyz.moosh.earthling.client.widget.Widget;
import xyz.moosh.earthling.client.widget.impl.*;
import xyz.moosh.earthling.client.screen.WidgetHudScreen;

import java.util.*;

public class WidgetManager {

    private final EventBus eventBus;
    private final ConfigManager configManager;
    private final List<Widget> widgets = new ArrayList<>();
    private final Map<Class<?>, Widget> byType = new HashMap<>();
    private final Map<String, Widget> byId = new LinkedHashMap<>();
    private boolean featuresActive = false;

    public WidgetManager(EventBus eventBus, ConfigManager configManager) {
        this.eventBus = eventBus;
        this.configManager = configManager;
    }

    public void init() {
        registerAll();

        // Hook into Fabric render loop
        HudRenderCallback.EVENT.register((guiGraphics, deltaTracker) ->
                this.renderActual(guiGraphics, deltaTracker.getGameTimeDeltaPartialTick(false))
        );

        eventBus.subscribe(xyz.moosh.earthling.client.event.impl.TickEvent.class, this::onTick);
    }

    public void enable()  { this.featuresActive = true; }
    public void disable() { this.featuresActive = false; }

    private void registerAll() {
        register(new MiningWidget().init(eventBus));
        register(new PlayerInfoWidget().init(eventBus));
        register(new NearbyPlayersWidget());
    }

    private void register(Widget widget) {
        widgets.add(widget);
        byId.put(widget.getId(), widget);
        byType.put(widget.getClass(), widget);
        configManager.register(widget.getConfig());
    }

    private void renderActual(GuiGraphics g, float tickDelta) {
        if (!featuresActive) return;

        Minecraft mc = Minecraft.getInstance();

        // Avoid drawing over the editor or when UI is hidden
        if (mc.options.hideGui || mc.screen instanceof WidgetHudScreen) return;

        int sw = g.guiWidth();
        int sh = g.guiHeight();

        for (Widget widget : widgets) {
            if (!widget.isVisible()) continue;

            int x = widget.getPosition().getX();
            int y = widget.getPosition().getY();
            float scale = widget.getScale();

            // Clamping to screen bounds
            x = Math.max(0, Math.min(sw - (int)(widget.getWidth() * scale), x));
            y = Math.max(0, Math.min(sh - (int)(widget.getHeight() * scale), y));

            Matrix3x2fStack pose = g.pose();
            pose.pushMatrix();
            pose.translate((float)x, (float)y);

            if (scale != 1.0f) {
                pose.scale(scale, scale);
            }

            widget.render(g, tickDelta);
            pose.popMatrix();
        }
    }

    private void onTick(xyz.moosh.earthling.client.event.impl.TickEvent e) {
        if (!featuresActive) return;
        for (Widget widget : widgets) widget.tick();
    }

    @SuppressWarnings("unchecked")
    public <T extends Widget> T get(Class<T> type) { return (T) byType.get(type); }
    public List<Widget> getWidgets() { return Collections.unmodifiableList(widgets); }
}