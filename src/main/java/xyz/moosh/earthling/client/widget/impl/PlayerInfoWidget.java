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

package xyz.moosh.earthling.client.widget.impl;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import xyz.moosh.earthling.client.EarthlingClient;
import xyz.moosh.earthling.client.config.ConfigOption;
import xyz.moosh.earthling.client.event.EventBus;
import xyz.moosh.earthling.client.event.impl.ServerChangeEvent;
import xyz.moosh.earthling.client.event.impl.ServerDisconnectEvent;
import xyz.moosh.earthling.client.model.PlayerInfo;
import xyz.moosh.earthling.client.render.RenderHelper;
import xyz.moosh.earthling.client.service.PlayerService;
import xyz.moosh.earthling.client.widget.Widget;

/**
 * Displays the local player's EarthMC information: town, nation, balance.
 *
 * <pre>
 *  ┌─────────────────────┐
 *  │ MOOSHTHEPRO123      │
 *  │ Town:   Ghent       │
 *  │ Nation: Belgium     │
 *  │ Gold:   23.5g       │
 *  └─────────────────────┘
 * </pre>
 */
public class PlayerInfoWidget extends Widget {

    private static final int PADDING    = 4;
    private static final int ROW_HEIGHT = 10;
    private static final int WIDTH      = 160;

    private static final long REFRESH_MS = 5 * 60_000L; // 5 minutes

    // Config
    private final ConfigOption<Integer> nameColor;
    private final ConfigOption<Integer> labelColor;
    private final ConfigOption<Integer> valueColor;
    private final ConfigOption<Integer> bgColor;

    // State
    private PlayerInfo info        = null;
    private boolean    loading     = false;
    private boolean    fetchFailed = false;
    private long       lastFetchMs = 0;

    public PlayerInfoWidget() {
        super("player_info", "Player Info", 0.02f, 0.42f);
        nameColor  = config.addColor("name_color",  "Name Color",       0xFF55FFFF);
        labelColor = config.addColor("label_color", "Label Color",      0xFFAAAAAA);
        valueColor = config.addColor("value_color", "Value Color",      0xFFFFFFFF);
        bgColor    = config.addColor("bg_color",    "Background Color", 0xAA000000);
    }

    public void init(EventBus eventBus) {
        eventBus.subscribe(ServerChangeEvent.class,     e -> onServerChange());
        eventBus.subscribe(ServerDisconnectEvent.class, e -> clearInfo());
    }

    // ── IWidget ───────────────────────────────────────────────────────────

    @Override public int getWidth()  { return WIDTH; }
    @Override public int getHeight() { return PADDING + ROW_HEIGHT * 4 + PADDING; }

    @Override
    public void tick() {
        long now = System.currentTimeMillis();
        // Fetch if: no data yet, or refresh interval elapsed, and not already loading
        if (!loading && (info == null && !fetchFailed
                || now - lastFetchMs > REFRESH_MS)) {
            fetchInfo();
        }
    }

    @Override
    public void render(GuiGraphics g, float tickDelta) {
        int w = getWidth();
        int h = getHeight();


        if (loading) {
            RenderHelper.drawText(g, "Loading...", PADDING, PADDING, 0xFF888888);
            return;
        }

        if (info == null) {
            String reason = fetchFailed ? "API error" : "No EarthMC data";
            RenderHelper.drawText(g, reason, PADDING, PADDING, 0xFF666666);
            return;
        }

        int y = PADDING;
        RenderHelper.drawText(g, info.name != null ? info.name : "?", PADDING, y, nameColor.get());
        y += ROW_HEIGHT + 2;

        drawRow(g, "Town:",   info.town   != null ? info.town   : "None", y); y += ROW_HEIGHT;
        drawRow(g, "Nation:", info.nation != null ? info.nation : "None", y); y += ROW_HEIGHT;
        drawRow(g, "Gold:",   String.format("%dg", (int) info.balance),        y);
    }

    // ── Internal ──────────────────────────────────────────────────────────

    private void drawRow(GuiGraphics g, String label, String value, int y) {
        RenderHelper.drawText(g, label, PADDING,      y, labelColor.get());
        RenderHelper.drawText(g, value, PADDING + 46, y, valueColor.get());
    }

    private void fetchInfo() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        String name = mc.player.getName().getString();
        loading     = true;
        fetchFailed = false;

        PlayerService playerService = EarthlingClient.getInstance()
                .getServiceManager().get(PlayerService.class);

        if (playerService == null) {
            loading = false;
            return;
        }

        playerService.getPlayer(name).thenAccept(result -> {
            this.info        = result;        // null = player not found on EarthMC
            this.loading     = false;
            this.fetchFailed = false;
            this.lastFetchMs = System.currentTimeMillis();
        }).exceptionally(e -> {
            this.loading     = false;
            this.fetchFailed = true;
            this.lastFetchMs = System.currentTimeMillis();
            EarthlingClient.LOGGER.warn("PlayerInfoWidget fetch failed: {}", e.getMessage());
            return null;
        });
    }

    private void onServerChange() {
        clearInfo();
        lastFetchMs = 0; // trigger immediate fetch
    }

    private void clearInfo() {
        info        = null;
        loading     = false;
        fetchFailed = false;
    }
}