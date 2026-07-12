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
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import xyz.moosh.earthling.client.EarthlingClient;
import xyz.moosh.earthling.client.module.ChatPreview;
import xyz.moosh.earthling.client.module.ExpOverlay;
import xyz.moosh.earthling.client.module.PlayerAffiliations;
import xyz.moosh.earthling.client.render.RenderHelper;

public class EarthlingConfigScreen extends Screen {
    private static final int MAX_MSG_CHARS  = 120;
    private static final int SCROLL_H       = 2; // Thinner, cleaner look
    private static final int SCROLL_PAD     = 6;

    private static final int COLOR_BG       = 0xCC0F0F14;
    private static final int COLOR_TITLE    = 0xFF38BDF8;
    private static final int COLOR_SUBTITLE = 0xFF888899;

    private final Screen parent;
    private EditBox townlessMsgField;

    // Fractional Layout State
    private int startY, inputX, inputY, inputW, scrollY;
    private boolean draggingScroll = false;

    public EarthlingConfigScreen(Screen parent) {
        super(Component.literal("Earthling"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int cx = width / 2;

        // Dynamic Scaling (Fractional)
        this.startY  = (int) (height * 0.28);
        int spacing  = (int) (height * 0.075);
        int btnW     = 60;
        int btnH     = 20;

        ChatPreview       preview      = EarthlingClient.getInstance().getModuleManager().get(ChatPreview.class);
        PlayerAffiliations affiliations = EarthlingClient.getInstance().getModuleManager().get(PlayerAffiliations.class);
        ExpOverlay         expOverlay   = EarthlingClient.getInstance().getModuleManager().get(ExpOverlay.class);

        // Toggle Buttons (Aligned to 65% of screen width)
        int toggleX = (int)(width * 0.65) - (btnW / 2);

        addRenderableWidget(Button.builder(Component.literal(preview.isEnabled() ? "§aTRUE" : "§cFALSE"),
                        btn -> { preview.toggle(); btn.setMessage(Component.literal(preview.isEnabled() ? "§aTRUE" : "§cFALSE")); })
                .bounds(toggleX, startY, btnW, btnH).build());

        addRenderableWidget(Button.builder(Component.literal(affiliations.isEnabled() ? "§aTRUE" : "§cFALSE"),
                        btn -> { affiliations.toggle(); btn.setMessage(Component.literal(affiliations.isEnabled() ? "§aTRUE" : "§cFALSE")); })
                .bounds(toggleX, startY + spacing, btnW, btnH).build());

        addRenderableWidget(Button.builder(Component.literal(expOverlay.isEnabled() ? "§aTRUE" : "§cFALSE"),
                        btn -> { expOverlay.toggle(); btn.setMessage(Component.literal(expOverlay.isEnabled() ? "§aTRUE" : "§cFALSE")); })
                .bounds(toggleX, startY + (spacing * 2), btnW, btnH).build());

        // Responsive Input Field
        this.inputW = (int) (width * 0.55); // 55% of screen width
        this.inputX = cx - (inputW / 2);
        this.inputY = startY + (spacing * 3) + 15;

        this.townlessMsgField = new EditBox(font, inputX, inputY, inputW, btnH, Component.literal(""));
        this.townlessMsgField.setMaxLength(MAX_MSG_CHARS);
        this.townlessMsgField.setValue(EarthlingClient.getInstance().getTownlessMessage().get());
        addRenderableWidget(this.townlessMsgField);

        this.scrollY = inputY + btnH + SCROLL_PAD;

        // Navigation
        addRenderableWidget(Button.builder(Component.literal("⬜ Widget HUD"),
                        btn -> minecraft.setScreen(new WidgetHudScreen(this)))
                .bounds(cx - 60, scrollY + 25, 120, 20).build());

        addRenderableWidget(Button.builder(Component.literal("Done"),
                        btn -> onClose())
                .bounds(cx - 40, height - (int)(height * 0.1), 80, 20).build());
    }

// ── Slider Navigation Logic ───────────────────────────────────────────

    private void seekToMouse(double mouseX) {
        String val = townlessMsgField.getValue();
        if (val.isEmpty()) return;

        // Calculate the target character index based on mouse X
        float pct = (float) (mouseX - inputX) / (float) inputW;
        pct = Math.max(0, Math.min(1, pct));
        int targetPos = (int) (pct * val.length());

        // FIX: Moving both the cursor and the highlight position to the SAME
        // spot prevents the blue selection box from appearing.
        townlessMsgField.setCursorPosition(targetPos);
        townlessMsgField.setHighlightPos(targetPos);
    }

    private boolean isOverScroll(double mx, double my) {
        // Slightly taller hit-box for easier clicking
        return mx >= inputX && mx <= inputX + inputW && my >= scrollY - 5 && my <= scrollY + SCROLL_H + 5;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean hasCaptured) {
        if (event.button() == 0 && isOverScroll(event.x(), event.y())) {
            this.draggingScroll = true;
            // Focus the field so user can see the cursor blinking
            this.setFocused(townlessMsgField);
            seekToMouse(event.x());
            return true; // IMPORTANT: Consuming this prevents the EditBox from starting its own selection logic
        }
        return super.mouseClicked(event, hasCaptured);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        if (this.draggingScroll && event.button() == 0) {
            seekToMouse(event.x());
            return true;
        }
        return super.mouseDragged(event, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        this.draggingScroll = false;
        return super.mouseReleased(event);
    }

    // ── Updated Scrubber Render ───────────────────────────────────────────

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float tickDelta) {
        g.fill(0, 0, width, height, COLOR_BG);

        // Proportional Headers
        g.drawCenteredString(font, "Earthling", width / 2, (int)(height * 0.1), COLOR_TITLE);
        g.drawCenteredString(font, "Quality of Life for EarthMC", width / 2, (int)(height * 0.15), COLOR_SUBTITLE);

        // Labels (using fractional positions)
        int labelX = (int)(width * 0.35);
        int spacing = (int) (height * 0.075);
        g.drawString(font, "Chat Preview",       labelX, startY + 6, 0xFFCCCCCC);
        g.drawString(font, "Player Affiliations", labelX, startY + spacing + 6, 0xFFCCCCCC);
        g.drawString(font, "Exp Overlay",         labelX, startY + (spacing * 2) + 6, 0xFFCCCCCC);

        g.drawString(font, "Invite Message:", inputX, inputY - 12, 0xFFAAAAAA);

        // --- NEW WHITE SCRUBBER BAR ---
        // 1. The Track (Very subtle grey line)
        g.fill(inputX, scrollY, inputX + inputW, scrollY + SCROLL_H, 0x44FFFFFF);

        // 2. The Scrubber (Bright White thin bar)
        int total = townlessMsgField.getValue().length();
        float progress = total == 0 ? 0 : (float) townlessMsgField.getCursorPosition() / total;

        int scrubberW = 4; // Thin vertical bar
        int tx = inputX + (int)(progress * (inputW - scrubberW));

        // Draw a small white vertical rectangle as the scrubber
        g.fill(tx, scrollY - 2, tx + scrubberW, scrollY + SCROLL_H + 2, 0xFFFFFFFF);

        // Character counter
        String counter = total + " / " + MAX_MSG_CHARS;
        g.drawString(font, counter, inputX + inputW - font.width(counter), scrollY + 8, 0xFF666666);

        super.render(g, mouseX, mouseY, tickDelta);
    }

    @Override
    public void onClose() {
        EarthlingClient.getInstance().getTownlessMessage().set(townlessMsgField.getValue().trim());
        EarthlingClient.getInstance().getConfigManager().save();
        minecraft.setScreen(parent);
    }
}
//Hey @username, welcome! Need a head start? We offer free gear, housing and help! Interested? Just type '/t join @town'