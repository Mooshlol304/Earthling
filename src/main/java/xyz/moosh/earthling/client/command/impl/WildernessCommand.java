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
import xyz.moosh.earthling.client.EarthlingClient;
import xyz.moosh.earthling.client.api.ICommand;
import xyz.moosh.earthling.client.model.Location;
import xyz.moosh.earthling.client.model.Resident;
import xyz.moosh.earthling.client.network.DynmapApi;
import xyz.moosh.earthling.client.network.EarthMCApi;
import xyz.moosh.earthling.client.util.ChatUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.Collections;

public class WildernessCommand implements ICommand {

    private static final int  PAGE_SIZE    = 5;
    private static final long MIN_AGE_MS   = 86_400_000L; // 24 hours

    private static final int LOCATION_BATCH_SIZE = 5;
    private static final long LOCATION_BATCH_DELAY_MS = 250L;

    // Cooldown variables
    private static final long COOLDOWN_MS = 15_000L; // 15 seconds
    private long lastExecutionTime = 0L;

    @Override public String getName() { return "wilderness"; }

    @Override
    public LiteralArgumentBuilder<FabricClientCommandSource> build() {
        return ClientCommandManager.literal("wilderness")
                .executes(ctx -> { run(1); return 1; })
                .then(ClientCommandManager.argument("page", IntegerArgumentType.integer(1))
                        .executes(ctx -> {
                            run(IntegerArgumentType.getInteger(ctx, "page"));
                            return 1;
                        }));
    }

    private void run(int page) {
        // Cooldown check
        long currentTime = System.currentTimeMillis();
        long timeElapsed = currentTime - lastExecutionTime;

        if (timeElapsed < COOLDOWN_MS) {
            long secondsLeft = (COOLDOWN_MS - timeElapsed + 999) / 1000;
            ChatUtil.sendMessage("§c§lHold on! §cYou can only run this scan every 15 seconds. Please wait §e" + secondsLeft + "s§c.");
            return;
        }

        DynmapApi dynmap = EarthlingClient.getInstance().getServiceManager().getDynmapApi();
        EarthMCApi api   = EarthlingClient.getInstance().getServiceManager().getEarthMCApi();

        if (dynmap == null || api == null) return;

        // Update the execution time now that the validation passed and the scan is officially starting
        lastExecutionTime = currentTime;

        ChatUtil.sendMessage("§7Scanning wilderness players...");

        AtomicInteger totalDynmap       = new AtomicInteger(0);
        AtomicInteger totalResidents    = new AtomicInteger(0);
        AtomicInteger skippedLocation   = new AtomicInteger(0);
        AtomicInteger skippedAge        = new AtomicInteger(0);
        AtomicInteger evaluatedWild     = new AtomicInteger(0);
        AtomicInteger droppedInTown     = new AtomicInteger(0);

        dynmap.getPlayers().thenCompose(players -> {
            if (players.isEmpty()) {
                return CompletableFuture.completedFuture(new ArrayList<PlayerResult>());
            }

            totalDynmap.set(players.size());

            Map<String, Location> lowerCasePlayers = players.entrySet().stream()
                    .collect(Collectors.toMap(
                            e -> e.getKey().toLowerCase(),
                            Map.Entry::getValue,
                            (loc1, loc2) -> loc1
                    ));

            List<String> names = new ArrayList<>(players.keySet());

            // FIX: Chunk the massive player list into safe batches of 50 to avoid API truncation
            List<List<String>> chunks = new ArrayList<>();
            for (int i = 0; i < names.size(); i += 50) {
                chunks.add(names.subList(i, Math.min(i + 50, names.size())));
            }

            List<CompletableFuture<List<Resident>>> batchFutures = chunks.stream()
                    .map(api::getPlayersBatch)
                    .collect(Collectors.toList());

            // Combine all batched requests together
            return CompletableFuture.allOf(batchFutures.toArray(new CompletableFuture[0]))
                    .thenCompose(v -> {
                        List<Resident> residents = batchFutures.stream()
                                .flatMap(f -> f.getNow(new ArrayList<>()).stream())
                                .collect(Collectors.toList());

                        totalResidents.set(residents.size());
                        List<PlayerResult> results = Collections.synchronizedList(new ArrayList<>());
                        List<ResidentLocation> toCheck = new ArrayList<>();

                        for (Resident resident : residents) {
                            if (resident.name == null) continue;

                            Location loc = lowerCasePlayers.get(resident.name.toLowerCase());

                            if (loc == null) {
                                skippedLocation.incrementAndGet();
                                continue;
                            }

                            boolean oldEnough =
                                    System.currentTimeMillis() - resident.registeredAt > MIN_AGE_MS;

                            if (!oldEnough) {
                                skippedAge.incrementAndGet();
                                continue;
                            }

                            evaluatedWild.incrementAndGet();
                            toCheck.add(new ResidentLocation(resident, loc));
                        }

                        CompletableFuture<Void> chain = CompletableFuture.completedFuture(null);

                        for (ResidentLocation entry : toCheck) {

                            chain = chain.thenCompose(done ->
                                    api.isWilderness((int) entry.location.x, (int) entry.location.z)
                                            .thenAccept(wild -> {
                                                if (wild) {
                                                    results.add(new PlayerResult(entry.resident.name, entry.location));
                                                } else {
                                                    droppedInTown.incrementAndGet();
                                                }
                                            })
                                            // 350 ms between every request
                                            .thenCompose(x -> sleep(350))
                            );
                        }

                        return chain.thenApply(done -> new ArrayList<>(results));
                    });
        }).thenAccept(results ->
                Minecraft.getInstance().execute(() -> {
                    ChatUtil.sendMessage("§8┌── §6§lWilderness Diagnostic Summary §8──┐");
                    ChatUtil.sendMessage("§8│ §7Dynmap Online:    §e" + totalDynmap.get());
                    ChatUtil.sendMessage("§8│ §7API Matches:       §e" + totalResidents.get());
                    ChatUtil.sendMessage("§8│ §cSkipped (No Loc):  §c" + skippedLocation.get());
                    ChatUtil.sendMessage("§8│ §cSkipped (New Alt): §c" + skippedAge.get());
                    ChatUtil.sendMessage("§8│ §7Checked Claims:   §e" + evaluatedWild.get());
                    ChatUtil.sendMessage("§8│ §cDropped (In Town): §c" + droppedInTown.get());
                    ChatUtil.sendMessage("§8└─── Total Results: §a" + results.size() + " §8───┘");

                    show(results, page);
                })
        ).exceptionally(e -> {
            Minecraft.getInstance().execute(() ->
                    ChatUtil.sendMessage("§cWilderness scan failed: " + e.getMessage()));
            return null;
        });
    }

    private void show(List<PlayerResult> players, int page) {
        if (players.isEmpty()) {
            ChatUtil.sendMessage("§cNo wilderness players found matching criteria.");
            return;
        }

        int start = (page - 1) * PAGE_SIZE;
        if (start >= players.size()) {
            ChatUtil.sendMessage("§cNo players on page " + page + ".");
            return;
        }

        int end = Math.min(start + PAGE_SIZE, players.size());
        ChatUtil.sendMessage("§6Wilderness Players §8(" + players.size() + " total, page " + page + ")");

        for (int i = start; i < end; i++) {
            PlayerResult p = players.get(i);
            ChatUtil.sendMessage("§e⚔ §f" + p.name + " §8• §7(" + (int) p.location.x + ", " + (int) p.location.z + ")");
            ChatUtil.sendClickable("§7  ↗ View on Map",
                    "https://map.earthmc.net/?zoom=4&x=" + (int) p.location.x + "&z=" + (int) p.location.z);
        }

        if (end < players.size()) {
            ChatUtil.sendMessage("§7More: §e/ert wilderness " + (page + 1));
        }
    }

    private CompletableFuture<Void> sleep(long millis) {
        return CompletableFuture.runAsync(() -> {
            try {
                Thread.sleep(millis);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
    }
    private record ResidentLocation(Resident resident, Location location) {}
    private record PlayerResult(String name, Location location) {}
}