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

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.Minecraft;
import xyz.moosh.earthling.client.EarthlingClient;
import xyz.moosh.earthling.client.api.ICommand;
import xyz.moosh.earthling.client.service.PlayerService;
import xyz.moosh.earthling.client.util.ChatUtil;

/** /er lastseen <name> */
public class LastSeenCommand implements ICommand {

    @Override public String getName() { return "lastseen"; }

    @Override
    public LiteralArgumentBuilder<FabricClientCommandSource> build() {
        return ClientCommandManager.literal("lastseen")
                .then(ClientCommandManager.argument("name", StringArgumentType.word())
                        .suggests(Suggestions.onlinePlayers())
                        .executes(ctx -> { run(StringArgumentType.getString(ctx, "name")); return 1; }));
    }

    private void run(String name) {
        PlayerService svc = EarthlingClient.getInstance().getServiceManager().getPlayerService();
        if (svc == null) return;
        svc.getPlayer(name).thenAccept(info -> Minecraft.getInstance().execute(() -> {
            if (info == null) { ChatUtil.sendMessage("§c" + name + " not found on EarthMC."); return; }
            if (info.isOnline) { ChatUtil.sendMessage("§f" + info.name + " §ais online right now."); return; }
            String seen = info.lastOnline > 0 ? timeAgo(info.lastOnline) : "unknown";
            ChatUtil.sendMessage("§f" + info.name + " §7last seen §f" + seen + "§7.");
        }));
    }

    private String timeAgo(long epochMs) {
        long m = (System.currentTimeMillis() - epochMs) / 60_000;
        if (m < 60)        return m + "m ago";
        if (m < 60 * 24)   return (m / 60) + "h " + (m % 60) + "m ago";
        long d = m / 60 / 24;
        return d + "d " + (m / 60 % 24) + "h ago";
    }
}