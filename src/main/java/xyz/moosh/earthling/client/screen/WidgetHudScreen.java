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

package xyz.moosh.earthling.client.screen;



import net.minecraft.client.gui.GuiGraphics;

import net.minecraft.client.gui.components.Button;

import net.minecraft.client.gui.screens.Screen;

import net.minecraft.network.chat.Component;

import org.lwjgl.glfw.GLFW;

import xyz.moosh.earthling.client.EarthlingClient;

import xyz.moosh.earthling.client.manager.ConfigManager;

import xyz.moosh.earthling.client.manager.WidgetManager;

import xyz.moosh.earthling.client.render.RenderHelper;

import xyz.moosh.earthling.client.widget.Widget;

import xyz.moosh.earthling.client.widget.impl.NearbyPlayersWidget;



import java.util.List;



/**

 * Full-screen overlay for repositioning widgets by dragging.

 * Drag sets fractional positions via setPositionPixels(), so positions

 * remain correct after any window resize or GUI scale change.

 */

public class WidgetHudScreen extends Screen {



    private static final int COLOR_BG = 0x88000000;

    private static final int COLOR_BORDER_IDLE = 0xFF555555;

    private static final int COLOR_BORDER_HOVER = 0xFF00AAFF;

    private static final int COLOR_BORDER_DRAG = 0xFF55FF55;

    private static final int COLOR_HIDDEN = 0x44FF5555;

    private static final int HEADER_H = 14;



    private final WidgetManager widgetManager;

    private final ConfigManager configManager;

    private final Screen parent;



    private Widget dragging = null;

    private int dragOffsetX = 0;

    private int dragOffsetY = 0;



    private boolean prevLeft = false;

    private boolean prevRight = false;



    public WidgetHudScreen(Screen parent) {

        super(Component.literal("Widget HUD"));

        this.parent = parent;

        this.widgetManager = EarthlingClient.getInstance().getWidgetManager();

        this.configManager = EarthlingClient.getInstance().getConfigManager();

    }



    @Override

    protected void init() {

        addRenderableWidget(Button.builder(

                Component.literal("Done"), btn -> onClose()

        ).bounds(width / 2 - 50, height - 28, 100, 20).build());

    }



    @Override

    public void onClose() {

        configManager.save();

        minecraft.setScreen(parent);

    }



    @Override public boolean isPauseScreen() { return false; }



// ── Render + input ────────────────────────────────────────────────────



    @Override

    public void render(GuiGraphics g, int mouseX, int mouseY, float tickDelta) {

// ── Mouse ────────────────────────────────────────────────────────

        long win = GLFW.glfwGetCurrentContext();

        boolean left = GLFW.glfwGetMouseButton(win, GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;

        boolean right = GLFW.glfwGetMouseButton(win, GLFW.GLFW_MOUSE_BUTTON_RIGHT) == GLFW.GLFW_PRESS;



        if (left && !prevLeft) { // left just pressed

            Widget hit = hitTest(mouseX, mouseY);

            if (hit != null) {

                // Clicking the Heading: ON/OFF button
                if (hit instanceof NearbyPlayersWidget nearby) {

                    int wx = hit.getPosition().getX();
                    int wy = hit.getPosition().getY();
                    int ww = scaledW(hit);

                    String text = nearby.getShowHeadingOption().get()
                            ? "Heading: ON"
                            : "Heading: OFF";

                    int textWidth = font.width(text);

                    int buttonX = wx + ww - textWidth - 4;
                    int buttonY = wy + 3;

                    if (mouseX >= buttonX &&
                            mouseX <= buttonX + textWidth &&
                            mouseY >= buttonY &&
                            mouseY <= buttonY + font.lineHeight) {

                        nearby.getShowHeadingOption().set(
                                !nearby.getShowHeadingOption().get()
                        );

                        configManager.save();
                        return; // Don't start dragging.
                    }
                }

                dragging = hit;

                dragOffsetX = mouseX - hit.getPosition().getX();
                dragOffsetY = mouseY - hit.getPosition().getY();
            }
        }



        if (left && dragging != null) { // dragging — update position

            int nx = Math.max(0, Math.min(width - scaledW(dragging), mouseX - dragOffsetX));

            int ny = Math.max(0, Math.min(height - scaledH(dragging) - HEADER_H, mouseY - dragOffsetY));

// setPositionPixels converts to fractions immediately → resize-safe

            dragging.setPositionPixels(nx, ny, width, height);

        }



        if (!left && dragging != null) { // released — save

            dragging = null;

            configManager.save();

        }



        if (right && !prevRight) {
            Widget hit = hitTest(mouseX, mouseY);
            if (hit != null) {
                hit.setVisible(!hit.isVisible());
            }
        }



        prevLeft = left;

        prevRight = right;



// ── Draw ─────────────────────────────────────────────────────────

        g.fill(0, 0, width, height, COLOR_BG);

        g.drawCenteredString(font,

                "§bWidget HUD §7— drag to reposition · right-click to show/hide",

                width / 2, 6, 0xFFFFFFFF);



        for (Widget widget : widgetManager.getWidgets()) {

            int wx = widget.getPosition().getX();

            int wy = widget.getPosition().getY();

            int ww = scaledW(widget);

            int wh = scaledH(widget) + HEADER_H;



            boolean isHovered = dragging == null && isOver(mouseX, mouseY, wx, wy, ww, wh);

            boolean isDragging = dragging == widget;

            boolean isHidden = !widget.isVisible();



            g.fill(wx, wy, wx + ww, wy + wh, isHidden ? COLOR_HIDDEN : 0x66000000);



            int border = isDragging ? COLOR_BORDER_DRAG : isHovered ? COLOR_BORDER_HOVER : COLOR_BORDER_IDLE;

            RenderHelper.drawBorder(g, wx, wy, ww, wh, border);



            String label = widget.getTitle() + (isHidden ? " §c[hidden]" : "");

            g.drawString(font, label, wx + 3, wy + 3,

                    isHovered || isDragging ? 0xFFFFFFFF : 0xFFAAAAAA, false);


            if (isHovered && widget instanceof NearbyPlayersWidget nearby) {

                String state = nearby.getShowHeadingOption().get() ? "Heading: ON" : "Heading: OFF";

                g.drawString(
                        font,
                        state,
                        wx + ww - font.width(state) - 4,
                        wy + 3,
                        nearby.getShowHeadingOption().get()
                                ? 0xFF55FF55
                                : 0xFFFF5555,
                        false
                );
            }

            if (!isHidden) {

                var pose = g.pose();

                pose.pushMatrix();

                pose.translate(wx, wy + HEADER_H);

                float scale = widget.getScale();

                if (scale != 1.0f) pose.scale(scale, scale);

                widget.render(g, tickDelta);

                pose.popMatrix();

            }

        }



        super.render(g, mouseX, mouseY, tickDelta);

    }



// ── Helpers ───────────────────────────────────────────────────────────



    private Widget hitTest(double mx, double my) {

        List<Widget> list = widgetManager.getWidgets();

        for (int i = list.size() - 1; i >= 0; i--) {

            Widget w = list.get(i);

            int wx = w.getPosition().getX();

            int wy = w.getPosition().getY();

            if (isOver(mx, my, wx, wy, scaledW(w), scaledH(w) + HEADER_H)) return w;

        }

        return null;

    }



    private boolean isOver(double mx, double my, int x, int y, int w, int h) {

        return mx >= x && mx < x + w && my >= y && my < y + h;

    }



    private int scaledW(Widget w) { return Math.max((int)(w.getWidth() * w.getScale()), 60); }

    private int scaledH(Widget w) { return Math.max((int)(w.getHeight() * w.getScale()), 14); }

}

