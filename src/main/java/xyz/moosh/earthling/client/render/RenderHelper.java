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

package xyz.moosh.earthling.client.render;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

/**
 * Stateless drawing utilities shared by widgets and modules.
 * All methods are static — never instantiate this class.
 */
public final class RenderHelper {

    private RenderHelper() {}

    private static Font font() {
        return Minecraft.getInstance().font;
    }

    // ── Text ──────────────────────────────────────────────────────────────

    /**
     * Draw a string with an optional drop shadow.
     *
     * @param g      GuiGraphics context
     * @param text   text to draw
     * @param x      screen x
     * @param y      screen y
     * @param color  ARGB color
     * @param shadow whether to draw a shadow
     */
    public static void drawText(GuiGraphics g, String text, int x, int y,
                                int color, boolean shadow) {
        g.drawString(font(), text, x, y, color, shadow);
    }

    public static void drawText(GuiGraphics g, String text, int x, int y, int color) {
        drawText(g, text, x, y, color, true);
    }

    /** @return pixel width of {@code text} using the default font. */
    public static int textWidth(String text) {
        return font().width(text);
    }

    /** @return line height in pixels (includes leading). */
    public static int lineHeight() {
        return font().lineHeight + 1;
    }

    // ── Rectangles ────────────────────────────────────────────────────────

    /** Fill a solid rectangle. */
    public static void fillRect(GuiGraphics g, int x1, int y1, int x2, int y2, int color) {
        g.fill(x1, y1, x2, y2, color);
    }

    /** Draw a 1-pixel border around a rectangle (not filled). */
    public static void drawBorder(GuiGraphics g, int x, int y, int w, int h, int color) {
        g.fill(x,         y,         x + w,     y + 1, color); // top
        g.fill(x,         y + h - 1, x + w,     y + h, color); // bottom
        g.fill(x,         y,         x + 1,     y + h, color); // left
        g.fill(x + w - 1, y,         x + w,     y + h, color); // right
    }

    /** Draw a filled rect with a 1-pixel border. */
    public static void fillRectWithBorder(GuiGraphics g, int x, int y, int w, int h,
                                          int fillColor, int borderColor) {
        fillRect(g, x, y, x + w, y + h, fillColor);
        drawBorder(g, x, y, w, h, borderColor);
    }

    // ── Color helpers ─────────────────────────────────────────────────────

    /** Build an ARGB int from components (each 0-255). */
    public static int argb(int a, int r, int g, int b) {
        return (a & 0xFF) << 24 | (r & 0xFF) << 16 | (g & 0xFF) << 8 | (b & 0xFF);
    }

    /** Return {@code color} with alpha replaced by {@code alpha} (0-255). */
    public static int withAlpha(int color, int alpha) {
        return (color & 0x00FFFFFF) | ((alpha & 0xFF) << 24);
    }
}