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
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import xyz.moosh.earthling.client.EarthlingClient;
import xyz.moosh.earthling.client.api.ICommand;
import xyz.moosh.earthling.client.model.Town;
import xyz.moosh.earthling.client.network.EarthMCApi;
import xyz.moosh.earthling.client.service.TownService;
import xyz.moosh.earthling.client.util.ChatUtil;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

public class LocateCommand implements ICommand {

    private static volatile List<String> townNameCache = new ArrayList<>();
    private static volatile long cacheTimestamp = 0;
    private static final long CACHE_TTL_MS = 10 * 60_000L;
    private static volatile boolean fetchingNames = false;

    @Override public String getName() { return "locate"; }

    @Override
    public LiteralArgumentBuilder<FabricClientCommandSource> build() {
        return ClientCommandManager.literal("locate")
                .then(ClientCommandManager.argument("town", StringArgumentType.greedyString())
                        .suggests(townSuggestions())
                        .executes(ctx -> {
                            // We pass the source to the run method to avoid NPEs later
                            run(ctx.getSource(), StringArgumentType.getString(ctx, "town").trim());
                            return 1;
                        }));
    }

    private static SuggestionProvider<FabricClientCommandSource> townSuggestions() {
        return (ctx, builder) -> {
            long now = System.currentTimeMillis();
            if (!fetchingNames && (townNameCache.isEmpty() || now - cacheTimestamp > CACHE_TTL_MS)) {
                fetchingNames = true;
                EarthMCApi api = EarthlingClient.getInstance().getServiceManager().getEarthMCApi();
                api.getAllTownNames().thenAccept(names -> {
                    townNameCache = names;
                    cacheTimestamp = System.currentTimeMillis();
                    fetchingNames = false;
                });
            }

            String remaining = builder.getRemaining().toLowerCase();
            townNameCache.stream()
                    .filter(name -> name.toLowerCase().startsWith(remaining))
                    .sorted()
                    .forEach(builder::suggest);

            return builder.buildFuture();
        };
    }

    private void run(FabricClientCommandSource source, String townName) {
        TownService svc = EarthlingClient.getInstance().getServiceManager().getTownService();
        if (svc == null) return;

        svc.getTown(townName).thenAccept(town ->
                Minecraft.getInstance().execute(() -> {
                    if (town == null) {
                        ChatUtil.sendMessage("§cTown §f" + townName + " §cnot found.");
                        return;
                    }

                    if (town.spawn == null) {
                        // Removed manual prefix here
                        ChatUtil.sendMessage("§b" + town.name + " §7has no recorded spawn.");
                        return;
                    }

                    int x = (int) town.spawn.x;
                    int z = (int) town.spawn.z;

                    String urlString = String.format("https://map.earthmc.net/?zoom=4&x=%d&z=%d", x, z);

                    ClickEvent click = new ClickEvent.OpenUrl(URI.create(urlString));
                    HoverEvent hover = new HoverEvent.ShowText(Component.literal("§7Open in browser:\n§f" + urlString));

                    Component mapLink = Component.literal("§b[Click for Map]")
                            .withStyle(style -> style
                                    .withClickEvent(click)
                                    .withHoverEvent(hover));

                    // Removed manual prefix here as well
                    ChatUtil.sendMessage("§f" + town.name + " §7is located at §e" + x + "§7, §e" + z);

                    if (Minecraft.getInstance().player != null) {
                        source.sendFeedback(mapLink);
                    }
                })
        );
    }
}