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
import xyz.moosh.earthling.client.network.EarthMCApi;
import xyz.moosh.earthling.client.service.PlayerService;
import xyz.moosh.earthling.client.util.ChatUtil;

/**
 * /ert discord <name>
 *
 * Gets the player's linked Discord user ID from EarthMC API, then resolves
 * the actual @username from discordlookup.mesalytic.moe (a public lookup API
 * that needs no token). Falls back to showing the raw ID if lookup fails.
 */
public class DiscordCommand implements ICommand {

    @Override public String getName() { return "discord"; }

    @Override
    public LiteralArgumentBuilder<FabricClientCommandSource> build() {
        return ClientCommandManager.literal("discord")
                .then(ClientCommandManager.argument("name", StringArgumentType.word())
                        .suggests(Suggestions.onlinePlayers())
                        .executes(ctx -> {
                            run(StringArgumentType.getString(ctx, "name"));
                            return 1;
                        }));
    }

    private void run(String name) {
        PlayerService svc = EarthlingClient.getInstance()
                .getServiceManager().getPlayerService();
        if (svc == null) return;

        // Force refresh so we always have the latest discord link
        svc.refreshPlayer(name).thenAccept(info ->
                Minecraft.getInstance().execute(() -> showResult(name, info)));
    }

    private void showResult(String queriedName, PlayerInfo info) {
        if (info == null) {
            ChatUtil.sendMessage("§c" + queriedName + " not found on EarthMC.");
            return;
        }

        String id = info.discordId;
        EarthlingClient.LOGGER.info("[DiscordCommand] discordId for {}: '{}'", info.name, id);

        if (id == null || id.isBlank()) {
            ChatUtil.sendMessage("§f" + info.name + " §7has no Discord linked.");
            return;
        }

        // Show the ID immediately while we try to resolve the username
        ChatUtil.sendMessage("§f" + info.name + " §8→ §7Resolving Discord username...");

        EarthMCApi api = EarthlingClient.getInstance().getServiceManager().getEarthMCApi();
        api.resolveDiscordUsername(id).thenAccept(username ->
                Minecraft.getInstance().execute(() -> {
                    if (username != null && !username.isBlank()) {
                        ChatUtil.sendMessage("§f" + info.name + " §8→ §b@" + username);
                    } else {
                        // Fallback: show raw ID as a Discord mention
                        ChatUtil.sendMessage("§f" + info.name + " §8→ §b<@" + id + "> §8(ID: §7" + id + "§8)");
                    }
                }));
    }
}