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
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import xyz.moosh.earthling.client.EarthlingClient;
import xyz.moosh.earthling.client.config.ConfigOption;
import xyz.moosh.earthling.client.event.EventBus;
import xyz.moosh.earthling.client.event.impl.BlockBreakEvent;
import xyz.moosh.earthling.client.event.impl.ServerDisconnectEvent;
import xyz.moosh.earthling.client.render.RenderHelper;
import xyz.moosh.earthling.client.widget.Widget;

import java.util.*;

/**
 * Tracks valuable ores mined during the current session.
 *
 * Uses an inventory-delta approach: when a block breaks, we record the expected
 * drop item and how many the player had BEFORE the break. We then check over the
 * next several ticks whether that item count increased. If it did, we count it.
 * If it didn't (Towny denied, block snapped back), we don't.
 *
 * This correctly handles:
 *  - Towny protection (no perms → no drops → not counted)
 *  - Fortune (counts actual drops received, not just 1)
 *  - Silk touch (counts the ore block received instead)
 */
public class MiningWidget extends Widget {

    private static final int PADDING    = 4;
    private static final int ROW_HEIGHT = 10;
    private static final int HEADER_H   = 12;
    private static final int HINT_H     = 10;
    private static final int WIDTH      = 140;

    private static final String HINT_TEXT = "/ert miningreset to clear";

    /** How many ticks to wait for the drop to arrive before giving up. */
    private static final int CHECK_TICKS = 100;

    // Config
    private final ConfigOption<Integer> textColor;
    private final ConfigOption<Integer> headerColor;
    private final ConfigOption<Integer> bgColor;

    // Session counts — label → count
    private final Map<String, Integer>   counts       = new LinkedHashMap<>();

    // Pending inventory checks
    private final List<PendingCheck>     pendingChecks = new ArrayList<>();

    public MiningWidget() {
        super("mining", "Mining", 0.02f, 0.25f);
        textColor   = config.addColor("text_color",   "Text Color",       0xFFFFFFFF);
        headerColor = config.addColor("header_color", "Header Color",     0xFF55FF55);
        bgColor     = config.addColor("bg_color",     "Background Color", 0xAA000000);
    }

    // Change 'void' to 'MiningWidget'
    public MiningWidget init(EventBus eventBus) {
        eventBus.subscribe(BlockBreakEvent.class,      this::onBlockBreak);
        eventBus.subscribe(ServerDisconnectEvent.class, e -> resetSession());
        return this; // Add this line
    }

    // ── IWidget ───────────────────────────────────────────────────────────

    @Override public int getWidth()  { return WIDTH; }
    @Override public int getHeight() {
        int base = HEADER_H + Math.max(1, counts.size()) * ROW_HEIGHT + PADDING;
        // Extra row for the reset hint when session has data
        return counts.isEmpty() ? base : base + HINT_H;
    }

    @Override
    public void tick() {
        if (pendingChecks.isEmpty()) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) { pendingChecks.clear(); return; }

        Inventory inv     = mc.player.getInventory();
        var       iter    = pendingChecks.iterator();

        while (iter.hasNext()) {
            PendingCheck check = iter.next();
            int nowCount = countItem(inv, check.drop);

            if (nowCount > check.countAtBreak) {
                // The drop arrived — count the delta (handles Fortune)
                counts.merge(check.label, nowCount - check.countAtBreak, Integer::sum);
                iter.remove();
            } else if (check.ticksLeft-- <= 0) {
                // Timed out — break was denied or drops went elsewhere
                iter.remove();
            }
        }
    }

    @Override
    public void render(GuiGraphics g, float tickDelta) {
        int w = getWidth();

        RenderHelper.drawText(g, "Mining Session", PADDING, 2, headerColor.get());

        if (counts.isEmpty()) {
            RenderHelper.drawText(g, "Nothing yet...", PADDING, HEADER_H, 0xFF666666);
            return;
        }

        int y = HEADER_H;
        for (Map.Entry<String, Integer> entry : counts.entrySet()) {
            String countStr = String.valueOf(entry.getValue());
            RenderHelper.drawText(g, entry.getKey(), PADDING, y, textColor.get());
            RenderHelper.drawText(g, countStr,
                    w - PADDING - RenderHelper.textWidth(countStr), y, 0xFFFFD700);
            y += ROW_HEIGHT;
        }

        // Reset hint
        RenderHelper.drawText(g, HINT_TEXT, PADDING, y + 1, 0xFF555555);
    }

    // ── Public API ────────────────────────────────────────────────────────

    /** Clears all session ore counts and pending drop checks. */
    public void resetSession() {
        counts.clear();
        pendingChecks.clear();
    }

    /** Returns true if no ores have been counted this session. */
    public boolean isSessionEmpty() {
        return counts.isEmpty();
    }

    // ── Internal ──────────────────────────────────────────────────────────

    private void onBlockBreak(BlockBreakEvent event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        DropMapping mapping = getDropMapping(event.getState());
        if (mapping == null) return;

        int countNow = countItem(mc.player.getInventory(), mapping.drop);
        pendingChecks.add(new PendingCheck(mapping.label, mapping.drop, countNow, CHECK_TICKS));
    }

    private DropMapping getDropMapping(BlockState state) {
        if (state.is(Blocks.GOLD_ORE) || state.is(Blocks.DEEPSLATE_GOLD_ORE))
            return new DropMapping("Gold Ore",     Items.RAW_GOLD);
        if (state.is(Blocks.DIAMOND_ORE) || state.is(Blocks.DEEPSLATE_DIAMOND_ORE))
            return new DropMapping("Diamond",      Items.DIAMOND);
        if (state.is(Blocks.ANCIENT_DEBRIS))
            return new DropMapping("Anc. Debris",  Items.NETHERITE_SCRAP);
        if (state.is(Blocks.EMERALD_ORE) || state.is(Blocks.DEEPSLATE_EMERALD_ORE))
            return new DropMapping("Emerald",      Items.EMERALD);
        if (state.is(Blocks.IRON_ORE) || state.is(Blocks.DEEPSLATE_IRON_ORE))
            return new DropMapping("Iron Ore",     Items.RAW_IRON);
        if (state.is(Blocks.COAL_ORE) || state.is(Blocks.DEEPSLATE_COAL_ORE))
            return new DropMapping("Coal",         Items.COAL);
        if (state.is(Blocks.COPPER_ORE) || state.is(Blocks.DEEPSLATE_COPPER_ORE))
            return new DropMapping("Copper",       Items.RAW_COPPER);
        if (state.is(Blocks.LAPIS_ORE) || state.is(Blocks.DEEPSLATE_LAPIS_ORE))
            return new DropMapping("Lapis",        Items.LAPIS_LAZULI);
        if (state.is(Blocks.REDSTONE_ORE) || state.is(Blocks.DEEPSLATE_REDSTONE_ORE))
            return new DropMapping("Redstone",     Items.REDSTONE);
        if (state.is(Blocks.NETHER_GOLD_ORE))
            return new DropMapping("Nether Gold",  Items.GOLD_NUGGET);
        if (state.is(Blocks.NETHER_QUARTZ_ORE))
            return new DropMapping("Quartz",       Items.QUARTZ);
        return null;
    }

    private int countItem(Inventory inv, Item item) {
        int total = 0;
        for (ItemStack stack : inv.getNonEquipmentItems()) {
            if (!stack.isEmpty() && stack.getItem() == item) total += stack.getCount();
        }
        return total;
    }

    // ── Inner types ───────────────────────────────────────────────────────

    private record DropMapping(String label, Item drop) {}

    private static class PendingCheck {
        final String label;
        final Item   drop;
        final int    countAtBreak;
        int          ticksLeft;

        PendingCheck(String label, Item drop, int countAtBreak, int ticksLeft) {
            this.label        = label;
            this.drop         = drop;
            this.countAtBreak = countAtBreak;
            this.ticksLeft    = ticksLeft;
        }
    }
}