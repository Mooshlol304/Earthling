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

/**
 * Stores a widget's screen position and anchor point.
 * Written to disk by {@link xyz.moosh.earthling.client.manager.WidgetManager}.
 */
public class WidgetPosition {

    /** Predefined snap anchors for auto-alignment. */
    public enum Anchor {
        TOP_LEFT, TOP_CENTER, TOP_RIGHT,
        MIDDLE_LEFT, MIDDLE_CENTER, MIDDLE_RIGHT,
        BOTTOM_LEFT, BOTTOM_CENTER, BOTTOM_RIGHT,
        NONE
    }

    private int    x;
    private int    y;
    private Anchor anchor;

    public WidgetPosition(int x, int y) {
        this(x, y, Anchor.NONE);
    }

    public WidgetPosition(int x, int y, Anchor anchor) {
        this.x      = x;
        this.y      = y;
        this.anchor = anchor;
    }

    public int    getX()      { return x; }
    public int    getY()      { return y; }
    public Anchor getAnchor() { return anchor; }

    public void setX(int x)           { this.x = x; }
    public void setY(int y)           { this.y = y; }
    public void setAnchor(Anchor a)   { this.anchor = a; }

    public void move(int dx, int dy) {
        this.x += dx;
        this.y += dy;
    }

    @Override
    public String toString() {
        return "WidgetPosition{x=" + x + ", y=" + y + ", anchor=" + anchor + "}";
    }
}
