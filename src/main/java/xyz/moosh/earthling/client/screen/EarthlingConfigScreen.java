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
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import xyz.moosh.earthling.client.EarthlingClient;
import xyz.moosh.earthling.client.config.ConfigOption;
import xyz.moosh.earthling.client.manager.ModuleManager;
import xyz.moosh.earthling.client.module.*;
import xyz.moosh.earthling.client.module.Module; // Explicitly import your Module class

import java.util.ArrayList;
import java.util.List;

public class EarthlingConfigScreen extends Screen {
    private static final int COLOR_BG       = 0xEE0F0F14;
    private static final int COLOR_TITLE    = 0xFF38BDF8;
    private static final int COLOR_SUBTITLE = 0xFF888899;
    private static final int MAX_MSG_CHARS  = 120;

    private final Screen parent;
    private EditBox townlessMsgField;

    private double scrollAmount = 0;
    private int totalContentHeight = 0;
    private final List<ScrollableElement> scrollableElements = new ArrayList<>();

    public EarthlingConfigScreen(Screen parent) {
        super(Component.literal("Earthling"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        this.scrollableElements.clear();
        this.clearWidgets();

        int cx = width / 2;
        int listWidth = (int) (width * 0.6);
        int leftAlign = cx - (listWidth / 2);
        int toggleX = cx + (listWidth / 2) - 60;
        int currentY = 10;
        int itemHeight = 30;

        // --- FETCH MODULES ---
        ModuleManager mm = EarthlingClient.getInstance().getModuleManager();
        ChatPreview preview = mm.get(ChatPreview.class);
        PlayerAffiliations affiliations = mm.get(PlayerAffiliations.class);
        ExpOverlay expOverlay = mm.get(ExpOverlay.class);
        Translation translation = mm.get(Translation.class);

        // 1. Module Toggles
        if (preview != null) {
            addModuleRow("Chat Preview", preview, leftAlign, toggleX, currentY);
            currentY += itemHeight;
        }

        if (affiliations != null) {
            addModuleRow("Player Affiliations", affiliations, leftAlign, toggleX, currentY);
            currentY += itemHeight;
        }

        if (expOverlay != null) {
            addModuleRow("Exp Overlay", expOverlay, leftAlign, toggleX, currentY);
            currentY += itemHeight;
        }

        if (translation != null) {
            addModuleRow("Translation System", translation, leftAlign, toggleX, currentY);
            currentY += itemHeight;

            // 2. Translation Language Selector (Inside Translation check)
            final TranslationLanguage[] languages = TranslationLanguage.values();
            ConfigOption<TranslationLanguage> langOpt = translation.getConfig().get("target_lang");

            Button langBtn = Button.builder(Component.literal("§f" + langOpt.get().toString()), btn -> {
                int next = (langOpt.get().ordinal() + 1) % languages.length;
                langOpt.set(languages[next]);
                btn.setMessage(Component.literal("§f" + languages[next].toString()));
            }).bounds(toggleX - 40, currentY, 100, 20).build();

            scrollableElements.add(new ScrollableElement("Target Language", leftAlign, currentY, langBtn));
            currentY += itemHeight + 10;
        }

        // 3. Townless Message Input
        currentY += 15;
        this.townlessMsgField = new EditBox(font, leftAlign, currentY + 15, listWidth, 20, Component.literal(""));
        this.townlessMsgField.setMaxLength(MAX_MSG_CHARS);
        this.townlessMsgField.setValue(EarthlingClient.getInstance().getTownlessMessage().get());

        scrollableElements.add(new ScrollableElement("Invite Message:", leftAlign, currentY, townlessMsgField));
        currentY += 60;

        // 4. Navigation
        Button widgetBtn = Button.builder(Component.literal("⬜ Widget HUD"),
                        btn -> minecraft.setScreen(new WidgetHudScreen(this)))
                .bounds(cx - 60, currentY, 120, 20).build();
        scrollableElements.add(new ScrollableElement(null, 0, currentY, widgetBtn));
        currentY += 40;

        this.totalContentHeight = currentY;

        // Register all widgets
        scrollableElements.forEach(e -> {
            if (e.widget != null) addRenderableWidget(e.widget);
        });

        // Fixed footer button
        addRenderableWidget(Button.builder(Component.literal("Done"), btn -> onClose())
                .bounds(cx - 40, height - 30, 80, 20).build());
    }

    private void addModuleRow(String label, Module module, int lx, int tx, int y) {
        Button b = Button.builder(Component.literal(module.isEnabled() ? "§aTRUE" : "§cFALSE"), btn -> {
            module.setEnabled(!module.isEnabled());
            btn.setMessage(Component.literal(module.isEnabled() ? "§aTRUE" : "§cFALSE"));
        }).bounds(tx, y, 60, 20).build();

        scrollableElements.add(new ScrollableElement(label, lx, y, b));
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        this.scrollAmount = Mth.clamp(scrollAmount - (verticalAmount * 20), 0, Math.max(0, totalContentHeight - (height / 2)));
        return true;
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float tickDelta) {
        g.fill(0, 0, width, height, COLOR_BG);

        g.drawCenteredString(font, "Earthling", width / 2, 15, COLOR_TITLE);
        g.drawCenteredString(font, "Quality of Life for EarthMC", width / 2, 28, COLOR_SUBTITLE);

        int viewTop = 50;
        int viewBottom = height - 40;

        g.enableScissor(0, viewTop, width, viewBottom);

        for (ScrollableElement e : scrollableElements) {
            int renderedY = (int) (viewTop + e.relY - scrollAmount);

            if (renderedY > viewTop - 30 && renderedY < viewBottom + 10) {
                if (e.label != null) {
                    g.drawString(font, e.label, e.lx, renderedY + 6, 0xFFCCCCCC);
                }
                if (e.widget != null) {
                    e.widget.setY(renderedY);
                    e.widget.visible = true;
                }
            } else {
                if (e.widget != null) e.widget.visible = false;
            }
        }

        g.disableScissor();

        if (totalContentHeight > (viewBottom - viewTop)) {
            int scrollbarX = width - 6;
            g.fill(scrollbarX, viewTop, width - 2, viewBottom, 0x22FFFFFF);

            double viewRatio = (double)(viewBottom - viewTop) / totalContentHeight;
            int barHeight = (int) ((viewBottom - viewTop) * viewRatio);
            int barPos = (int) (viewTop + (scrollAmount * viewRatio));
            g.fill(scrollbarX, barPos, width - 2, barPos + barHeight, 0xAA38BDF8);
        }

        super.render(g, mouseX, mouseY, tickDelta);
    }

    @Override
    public void onClose() {
        EarthlingClient.getInstance().getTownlessMessage().set(townlessMsgField.getValue().trim());
        EarthlingClient.getInstance().getConfigManager().save();
        minecraft.setScreen(parent);
    }

    private static class ScrollableElement {
        String label;
        int lx, relY;
        AbstractWidget widget;

        public ScrollableElement(String label, int lx, int relY, AbstractWidget widget) {
            this.label = label;
            this.lx = lx;
            this.relY = relY;
            this.widget = widget;
        }
    }
}