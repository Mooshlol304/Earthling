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

package xyz.moosh.earthling.client.command.impl;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import xyz.moosh.earthling.client.api.ICommand;
import xyz.moosh.earthling.client.util.ChatUtil;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/** /er nearby [radius] */
public class NearbyCommand implements ICommand {

    @Override public String getName() { return "nearby"; }

    @Override
    public LiteralArgumentBuilder<FabricClientCommandSource> build() {
        return ClientCommandManager.literal("nearby")
                .executes(ctx -> { run(500); return 1; })
                .then(ClientCommandManager.argument("radius", IntegerArgumentType.integer(1, 5000))
                        .executes(ctx -> { run(IntegerArgumentType.getInteger(ctx, "radius")); return 1; }));
    }

    private void run(int radius) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;

        String selfName = mc.player.getName().getString();

        List<AbstractClientPlayer> nearby = mc.level.players().stream()
                .filter(p -> !p.getName().getString().equals(selfName))
                .filter(p -> mc.player.distanceTo(p) <= radius)
                .sorted(Comparator.comparingDouble(p -> mc.player.distanceTo(p)))
                .collect(Collectors.toList());

        if (nearby.isEmpty()) {
            ChatUtil.sendMessage("§7No players within §e" + radius + "m§7.");
            return;
        }

        StringBuilder sb = new StringBuilder("§bNearby §8(§e" + radius + "m§8): ");
        for (int i = 0; i < nearby.size(); i++) {
            if (i > 0) sb.append("§8  ");
            sb.append("§f").append(nearby.get(i).getName().getString())
                    .append(" §7").append((int) mc.player.distanceTo(nearby.get(i))).append("m");
        }
        ChatUtil.sendMessage(sb.toString());
    }
}