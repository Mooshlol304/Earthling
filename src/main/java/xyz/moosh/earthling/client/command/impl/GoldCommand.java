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
import xyz.moosh.earthling.client.api.ICommand;
import xyz.moosh.earthling.client.util.ChatUtil;

/**
 * /er gold toIngots <blocks> [extra_ingots]
 *   → total gold ingots (1 block = 9 ingots)
 *
 * /er gold fromIngots <ingots>
 *   → how many blocks and leftover ingots
 */
public class GoldCommand implements ICommand {

    @Override public String getName() { return "gold"; }

    @Override
    public LiteralArgumentBuilder<FabricClientCommandSource> build() {
        return ClientCommandManager.literal("gold")
                .executes(ctx -> { sendUsage(); return 1; })

                // toIngots <blocks> [ingots]
                .then(ClientCommandManager.literal("toIngots")
                        .executes(ctx -> { sendUsage(); return 1; })
                        .then(ClientCommandManager.argument("blocks", IntegerArgumentType.integer(0))
                                .executes(ctx -> {
                                    toIngots(IntegerArgumentType.getInteger(ctx, "blocks"), 0);
                                    return 1;
                                })
                                .then(ClientCommandManager.argument("ingots", IntegerArgumentType.integer(0))
                                        .executes(ctx -> {
                                            toIngots(IntegerArgumentType.getInteger(ctx, "blocks"),
                                                     IntegerArgumentType.getInteger(ctx, "ingots"));
                                            return 1;
                                        }))))

                // fromIngots <ingots>
                .then(ClientCommandManager.literal("fromIngots")
                        .executes(ctx -> { sendUsage(); return 1; })
                        .then(ClientCommandManager.argument("ingots", IntegerArgumentType.integer(0))
                                .executes(ctx -> {
                                    fromIngots(IntegerArgumentType.getInteger(ctx, "ingots"));
                                    return 1;
                                })));
    }

    private void toIngots(int blocks, int extraIngots) {
        int total = blocks * 9 + extraIngots;
        ChatUtil.sendMessage("§f" + blocks + " blocks §8+ §f" + extraIngots
                + " ingots §8= §6" + total + " gold ingots");
    }

    private void fromIngots(int total) {
        int blocks  = total / 9;
        int ingots  = total % 9;
        ChatUtil.sendMessage("§6" + total + " ingots §8= §f" + blocks
                + " blocks §8+ §f" + ingots + " ingots");
    }

    private void sendUsage() {
        ChatUtil.sendMessage("§7Usage: §f/er gold toIngots §e<blocks> §7[ingots]");
        ChatUtil.sendMessage("§7       §f/er gold fromIngots §e<ingots>");
    }
}
