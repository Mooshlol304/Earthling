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

import net.minecraft.client.Minecraft;
import xyz.moosh.earthling.client.EarthlingClient;
import xyz.moosh.earthling.client.event.EventBus;
import xyz.moosh.earthling.client.event.impl.HudRenderEvent;
import xyz.moosh.earthling.client.event.impl.TickEvent;
import xyz.moosh.earthling.client.widget.Widget;
import xyz.moosh.earthling.client.widget.impl.MiningWidget;
import xyz.moosh.earthling.client.widget.impl.NearbyPlayersWidget;
import xyz.moosh.earthling.client.widget.impl.PlayerInfoWidget;
import xyz.moosh.earthling.client.screen.WidgetHudScreen;

import java.util.*;

/**
 * Owns every {@link Widget}. Handles rendering, ticking, and visibility.
 * Window-resize handling is eliminated — Widget now stores fractional positions
 * so getPosition() always returns correct pixel coords for the current screen size.
 */
public class WidgetManager {

    private final EventBus      eventBus;
    private final ConfigManager configManager;

    private final List<Widget>          widgets = new ArrayList<>();
    private final Map<String, Widget>   byId    = new LinkedHashMap<>();
    private final Map<Class<?>, Widget> byType  = new HashMap<>();

    public WidgetManager(EventBus eventBus, ConfigManager configManager) {
        this.eventBus      = eventBus;
        this.configManager = configManager;
    }

    public void init() {
        registerAll();
        eventBus.subscribe(HudRenderEvent.class, this::onHudRender);
        eventBus.subscribe(TickEvent.class,       this::onTick);
        EarthlingClient.LOGGER.info("WidgetManager: {} widget(s) registered.", widgets.size());
    }

    // ── Registration ──────────────────────────────────────────────────────

    private void registerAll() {
        MiningWidget mining = new MiningWidget();
        mining.init(eventBus);
        register(mining);

        PlayerInfoWidget playerInfo = new PlayerInfoWidget();
        playerInfo.init(eventBus);
        register(playerInfo);

        register(new NearbyPlayersWidget());
    }

    private void register(Widget widget) {
        if (byId.containsKey(widget.getId()))
            throw new IllegalStateException("Duplicate widget id: " + widget.getId());
        configManager.register(widget.getConfig());
        widgets.add(widget);
        byId.put(widget.getId(), widget);
        byType.put(widget.getClass(), widget);
    }

    // ── Event handlers ────────────────────────────────────────────────────

    private void onHudRender(HudRenderEvent e) {
        Minecraft mc = Minecraft.getInstance();

        if (mc.getDebugOverlay().showDebugScreen()) return;

        if (mc.screen instanceof WidgetHudScreen) return;

        int sw = mc.getWindow().getGuiScaledWidth();
        int sh = mc.getWindow().getGuiScaledHeight();

        for (Widget widget : widgets) {
            if (!widget.isVisible()) continue;

            // getPosition() computes pixels from fractions — always correct after resize
            int   x     = widget.getPosition().getX();
            int   y     = widget.getPosition().getY();
            float scale = widget.getScale();

            // Clamp to screen bounds (safety net for edge cases)
            x = Math.max(0, Math.min(sw - Math.max(1, (int)(widget.getWidth()  * scale)), x));
            y = Math.max(0, Math.min(sh - Math.max(1, (int)(widget.getHeight() * scale)), y));

            var pose = e.getGuiGraphics().pose();
            pose.pushMatrix();
            pose.translate(x, y);
            if (scale != 1.0f) pose.scale(scale, scale);
            widget.render(e.getGuiGraphics(), e.getTickDelta());
            pose.popMatrix();
        }
    }

    private void onTick(TickEvent e) {
        for (Widget widget : widgets) widget.tick();
    }

    // ── Queries ───────────────────────────────────────────────────────────

    public List<Widget> getWidgets()               { return Collections.unmodifiableList(widgets); }
    public Optional<Widget> get(String id)         { return Optional.ofNullable(byId.get(id)); }
    @SuppressWarnings("unchecked")
    public <T extends Widget> T get(Class<T> type) { return (T) byType.get(type); }
}