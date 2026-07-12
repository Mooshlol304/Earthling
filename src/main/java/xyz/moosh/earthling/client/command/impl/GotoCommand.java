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
import xyz.moosh.earthling.client.EarthlingClient;
import xyz.moosh.earthling.client.api.ICommand;
import xyz.moosh.earthling.client.model.Town;
import xyz.moosh.earthling.client.network.EarthMCApi;
import xyz.moosh.earthling.client.service.TownService;
import xyz.moosh.earthling.client.util.ChatUtil;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * /ert goto <town>
 *
 * - Tab-completes from the full EarthMC town list (cached 10 min).
 * - Checks if the town is a capital to allow /n spawn <nation>.
 * - Checks canOutsidersSpawn (not isOpen) to determine if you can /t spawn there.
 * - If neither applies, finds the nearest town where it IS true.
 */
public class GotoCommand implements ICommand {

    // Town name cache for autocomplete
    private static volatile List<String> townNameCache   = new ArrayList<>();
    private static volatile long         cacheTimestamp  = 0;
    private static final long            CACHE_TTL_MS    = 10 * 60_000L; // 10 min
    private static volatile boolean      fetchingNames   = false;

    @Override public String getName() { return "goto"; }

    @Override
    public LiteralArgumentBuilder<FabricClientCommandSource> build() {
        return ClientCommandManager.literal("goto")
                .then(ClientCommandManager.argument("town", StringArgumentType.greedyString())
                        .suggests(townSuggestions())
                        .executes(ctx -> {
                            run(StringArgumentType.getString(ctx, "town").trim());
                            return 1;
                        }));
    }

    // ── Suggestions ───────────────────────────────────────────────────────

    private static SuggestionProvider<FabricClientCommandSource> townSuggestions() {
        return (ctx, builder) -> {
            long now = System.currentTimeMillis();

            // Refresh cache in background if stale
            if (!fetchingNames && (townNameCache.isEmpty() || now - cacheTimestamp > CACHE_TTL_MS)) {
                fetchingNames = true;
                EarthMCApi api = EarthlingClient.getInstance().getServiceManager().getEarthMCApi();
                api.getAllTownNames().thenAccept(names -> {
                    townNameCache  = names;
                    cacheTimestamp = System.currentTimeMillis();
                    fetchingNames  = false;
                    EarthlingClient.LOGGER.info("[GotoCommand] Cached {} town names.", names.size());
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

    // ── Command logic ─────────────────────────────────────────────────────

    private void run(String townName) {
        TownService svc = EarthlingClient.getInstance().getServiceManager().getTownService();
        if (svc == null) return;

        svc.getTown(townName).thenAccept(town ->
                Minecraft.getInstance().execute(() -> {
                    if (town == null) {
                        ChatUtil.sendMessage("§cTown §f" + townName + " §cnot found.");
                        return;
                    }
                    showTown(town);
                }));
    }

    private void showTown(Town town) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        double px = mc.player.getX();
        double pz = mc.player.getZ();

        // Header (Adds a gold [Capital] tag next to the town name if applicable)
        String capitalTag = town.isCapital ? " §6[Capital]" : "";
        ChatUtil.sendMessage("§b" + town.name + capitalTag
                + (town.nation != null ? " §8(§f" + town.nation + "§8)" : "")
                + " §8• §7Mayor: §f" + nvl(town.mayor, "?")
                + " §8• §7Pop: §f" + town.numResidents);

        if (town.spawn == null) {
            ChatUtil.sendMessage("§7Spawn: §cunknown");
            return;
        }

        double dist = dist2d(px, pz, town.spawn.x, town.spawn.z);
        String dir  = bearing(px, pz, town.spawn.x, town.spawn.z);

        ChatUtil.sendMessage("§7Spawn: §f(" + (int) town.spawn.x + ", " + (int) town.spawn.z
                + ") §8• §e" + (int) dist + "m " + dir);

        // 1. Check if the town is a nation capital
        if (town.isCapital && town.nation != null) {
            ChatUtil.sendMessage("§aThis town is a capital! §8• §7/n spawn " + town.nation);
        }
        // 2. Check if normal players can teleport directly to the town
        else if (town.canOutsidersSpawn) {
            ChatUtil.sendMessage("§aOutsiders can spawn here §8• §7/t spawn " + town.name);
        }
        // 3. Fallback: Find alternative nearby towns
        else {
            ChatUtil.sendMessage("§cOutsiders cannot spawn here. §7Finding nearest alternative...");
            findNearestSpawnable(town.spawn.x, town.spawn.z, town.name, px, pz);
        }
    }

    private void findNearestSpawnable(double destX, double destZ, String skipTown,
                                      double playerX, double playerZ) {
        EarthMCApi api = EarthlingClient.getInstance().getServiceManager().getEarthMCApi();

        api.getNearbyTownNames((int) destX, (int) destZ, 3000)
                .thenCompose(names -> {
                    List<String> filtered = names.stream()
                            .filter(n -> !n.equalsIgnoreCase(skipTown))
                            .limit(15)
                            .collect(Collectors.toList());
                    return api.getTownsBatch(filtered);
                })
                .thenAccept(towns -> Minecraft.getInstance().execute(() -> {
                    Optional<Town> nearest = towns.stream()
                            .filter(t -> t.canOutsidersSpawn && t.spawn != null)
                            .min(Comparator.comparingDouble(
                                    t -> dist2d(destX, destZ, t.spawn.x, t.spawn.z)));

                    if (nearest.isEmpty()) {
                        ChatUtil.sendMessage("§7No spawnable towns found within 3km of destination.");
                        return;
                    }

                    Town t   = nearest.get();
                    double d = dist2d(playerX, playerZ, t.spawn.x, t.spawn.z);
                    String dir = bearing(playerX, playerZ, t.spawn.x, t.spawn.z);
                    ChatUtil.sendMessage("§7Nearest spawnable: §b" + t.name
                            + " §8• §7/t spawn " + t.name
                            + " §8• §e" + (int) d + "m " + dir + " §7from you");
                }));
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    private double dist2d(double x1, double z1, double x2, double z2) {
        double dx = x2 - x1, dz = z2 - z1;
        return Math.sqrt(dx * dx + dz * dz);
    }

    private String bearing(double fx, double fz, double tx, double tz) {
        double angle = Math.toDegrees(Math.atan2(tx - fx, -(tz - fz)));
        if (angle < 0) angle += 360;
        String[] dirs = {"N", "NE", "E", "SE", "S", "SW", "W", "NW", "N"};
        return dirs[(int) Math.round(angle / 45) % 8];
    }

    private String nvl(String s, String fallback) { return s != null ? s : fallback; }
}