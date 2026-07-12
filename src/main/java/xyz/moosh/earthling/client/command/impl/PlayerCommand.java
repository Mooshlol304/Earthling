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
import xyz.moosh.earthling.client.model.PlayerInfo;
import xyz.moosh.earthling.client.service.PlayerService;
import xyz.moosh.earthling.client.util.ChatUtil;

/** /er player <name> */
public class PlayerCommand implements ICommand {

    @Override public String getName() { return "player"; }

    @Override
    public LiteralArgumentBuilder<FabricClientCommandSource> build() {
        return ClientCommandManager.literal("player")
                .then(ClientCommandManager.argument("name", StringArgumentType.word())
                        .suggests(Suggestions.onlinePlayers())
                        .executes(ctx -> { run(StringArgumentType.getString(ctx, "name")); return 1; }));
    }

    private void run(String name) {
        PlayerService svc = EarthlingClient.getInstance().getServiceManager().getPlayerService();
        if (svc == null) return;
        ChatUtil.sendMessage("§7Looking up §f" + name + "§7...");
        svc.getPlayer(name).thenAccept(info -> Minecraft.getInstance().execute(() -> {
            if (info == null) { ChatUtil.sendMessage("§c" + name + " not found on EarthMC."); return; }
            String status  = info.isOnline ? "§aOnline" : "§cOffline";
            String town    = info.town   != null ? info.town   : "—";
            String nation  = info.nation != null ? info.nation : "—";
            long   ago     = info.lastOnline > 0 ? (System.currentTimeMillis() - info.lastOnline) / 60000 : -1;
            String lastStr = info.isOnline ? "" : (ago >= 0 ? "§8 • §7" + fmtAgo(ago) : "");
            ChatUtil.sendMessage("§b" + info.name + " §8[" + status + "§8]" + lastStr);
            ChatUtil.sendMessage("§7Town: §f" + town + " §8• §7Nation: §f" + nation
                    + " §8• §7Gold: §6" + (int) info.balance + "g");

            if (info.mapLocation != null) {
                ChatUtil.sendMessage(
                        "§7Map Location: §f"
                                + (int) info.mapLocation.x + ", "
                                + (int) info.mapLocation.y + ", "
                                + (int) info.mapLocation.z);
            } else {
                ChatUtil.sendMessage("§7Map Location: §cUnknown");
            }
        }));
    }

    private String fmtAgo(long mins) {
        if (mins < 60)               return mins + "m ago";
        if (mins < 60 * 24)          return (mins / 60) + "h ago";
        return (mins / 60 / 24) + "d ago";
    }
}