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
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.entity.player.Player;
import xyz.moosh.earthling.client.config.ConfigOption;
import xyz.moosh.earthling.client.event.EventBus;
import xyz.moosh.earthling.client.render.RenderHelper;
import xyz.moosh.earthling.client.widget.Widget;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Displays a list of players near the local player, sorted by distance.
 */
public class NearbyPlayersWidget extends Widget {

    private static final int PADDING    = 4;
    private static final int ROW_HEIGHT = 10;
    private static final int HEADER_H   = 12;
    private static final int WIDTH      = 150;

    // Config
    private final ConfigOption<Integer> radius;
    private final ConfigOption<Integer> maxPlayers;
    private final ConfigOption<Integer> textColor;
    private final ConfigOption<Integer> headerColor;
    private final ConfigOption<Boolean> showSelf;
    private final ConfigOption<Boolean> showHeading;

    // State
    private final List<NearbyEntry> entries = new ArrayList<>();

    public NearbyPlayersWidget() {
        super("nearby_players", "Nearby Players", 0.02f, 0.10f);

        radius      = config.addIntSlider("radius",      "Radius (blocks)", 500, 50, 2000);
        maxPlayers  = config.addIntSlider("max_players", "Max Players", 10, 1, 30);
        textColor   = config.addColor("text_color", "Text Color", 0xFFFFFFFF);
        headerColor = config.addColor("header_color", "Header Color", 0xFF55FFFF);
        showSelf    = config.addToggle("show_self", "Show Self", false);
        showHeading = config.addToggle("show_heading", "Show Heading", true);
    }


    public NearbyPlayersWidget init(EventBus eventBus) {
        return this;
    }

    @Override
    public int getWidth() {
        return WIDTH;
    }

    @Override
    public int getHeight() {
        if (entries.isEmpty()) {
            return ROW_HEIGHT + PADDING;
        }

        int header = showHeading.get() ? HEADER_H : 0;
        return header + entries.size() * ROW_HEIGHT + PADDING;
    }

    @Override
    public void tick() {
        Minecraft mc = Minecraft.getInstance();

        if (mc.player == null || mc.level == null) {
            entries.clear();
            return;
        }

        Player self = mc.player;
        double radiusDouble = radius.get();
        String selfName = self.getName().getString();

        entries.clear();

        for (AbstractClientPlayer other : mc.level.players()) {
            String name = other.getName().getString();

            if (!showSelf.get() && name.equals(selfName))
                continue;

            double dist = self.distanceTo(other);

            if (dist > radiusDouble)
                continue;

            // Calculate direction string (N, NW, etc.)
            String dir = getDirection(self, other);

            entries.add(new NearbyEntry(name, dir, (int) dist));
        }

        entries.sort(Comparator.comparingInt(e -> e.distance));

        if (entries.size() > maxPlayers.get()) {
            entries.subList(maxPlayers.get(), entries.size()).clear();
        }
    }

    @Override
    public void render(GuiGraphics g, float tickDelta) {
        int w = getWidth();

        if (entries.isEmpty()) {
            RenderHelper.drawText(g, "No players nearby", PADDING, 2, 0xFFFF5555);
            return;
        }

        int y = 0;

        if (showHeading.get()) {
            RenderHelper.drawText(g, "Nearby Players", PADDING, 2, headerColor.get());
            y = HEADER_H;
        }

        for (NearbyEntry entry : entries) {
            // Updated string format: "DIR 10m"
            String infoStr = entry.direction + " " + entry.distance + "m";

            RenderHelper.drawText(
                    g,
                    entry.name,
                    PADDING,
                    y,
                    textColor.get()
            );

            // Draw the "DIR 10m" on the right side
            RenderHelper.drawText(
                    g,
                    infoStr,
                    w - PADDING - RenderHelper.textWidth(infoStr),
                    y,
                    0xFFAAAAAA
            );

            y += ROW_HEIGHT;
        }
    }

    /**
     * Calculates the compass direction from 'self' to 'target'.
     */
    private String getDirection(Player self, Player target) {
        double dx = target.getX() - self.getX();
        double dz = target.getZ() - self.getZ();

        // Convert to degrees and adjust for Minecraft coords
        float angle = (float) Math.toDegrees(Math.atan2(dz, dx)) - 90;
        while (angle < 0) angle += 360;
        angle = angle % 360;

        if (angle >= 337.5 || angle < 22.5)  return "S";
        if (angle >= 22.5  && angle < 67.5)  return "SW";
        if (angle >= 67.5  && angle < 112.5) return "W";
        if (angle >= 112.5 && angle < 157.5) return "NW";
        if (angle >= 157.5 && angle < 202.5) return "N";
        if (angle >= 202.5 && angle < 247.5) return "NE";
        if (angle >= 247.5 && angle < 292.5) return "E";
        if (angle >= 292.5 && angle < 337.5) return "SE";

        return "N";
    }

    public ConfigOption<Boolean> getShowHeadingOption() {
        return showHeading;
    }

    // Added 'direction' to the record
    private record NearbyEntry(String name, String direction, int distance) {}
}