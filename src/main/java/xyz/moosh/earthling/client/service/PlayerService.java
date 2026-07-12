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

package xyz.moosh.earthling.client.service;

import xyz.moosh.earthling.client.EarthlingClient;
import xyz.moosh.earthling.client.event.EventBus;
import xyz.moosh.earthling.client.event.impl.ServerDisconnectEvent;
import xyz.moosh.earthling.client.model.Location;
import xyz.moosh.earthling.client.model.PlayerInfo;
import xyz.moosh.earthling.client.model.Resident;
import xyz.moosh.earthling.client.network.ApiCache;
import xyz.moosh.earthling.client.network.EarthMCApi;
import xyz.moosh.earthling.client.network.DynmapApi;

import java.util.concurrent.CompletableFuture;

/**
 * Provides cached player lookups.
 * 60s TTL — good enough for display without hammering the API.
 */
public class PlayerService extends Service {

    private static final long CACHE_TTL_MS = 60_000L;

    private final EarthMCApi           api;
    private final DynmapApi dynmap;
    private final ApiCache<PlayerInfo> cache = new ApiCache<>(CACHE_TTL_MS);

    public PlayerService(EventBus eventBus, EarthMCApi api, DynmapApi dynmap) {
        super("player_service", eventBus);
        this.api = api;
        this.dynmap = dynmap;
    }

    @Override
    public void init() {
        eventBus.subscribe(ServerDisconnectEvent.class, e -> cache.clear());
    }

    // ── Public API ────────────────────────────────────────────────────────

    public CompletableFuture<PlayerInfo> getPlayer(String name) {
        PlayerInfo cached = cache.get(name);
        if (cached != null) return CompletableFuture.completedFuture(cached);

        return api.getPlayer(name)
                .thenCombine(dynmap.getPlayers(), (resident, locations) -> {

                    if (resident == null)
                        return null;

                    PlayerInfo info = toPlayerInfo(resident);

                    Location loc = locations.get(resident.name);

                    info.mapLocation = locations.get(resident.name.toLowerCase());

                    if (info.mapLocation == null && resident.isOnline) {
                        EarthlingClient.LOGGER.debug(
                                "[PlayerService] '{}' is online but not visible on Dynmap.",
                                resident.name
                        );
                    }

                    cache.put(name, info);

                    return info;
                });
    }

    public CompletableFuture<PlayerInfo> refreshPlayer(String name) {
        cache.invalidate(name);
        return getPlayer(name);
    }

    public PlayerInfo getCachedPlayer(String name) {
        return cache.get(name);
    }

    // ── Internal ──────────────────────────────────────────────────────────

    private PlayerInfo toPlayerInfo(Resident r) {
        PlayerInfo info = new PlayerInfo();

        info.name         = r.name;
        info.uuid         = r.uuid;
        info.town         = r.town;
        info.nation       = r.nation;
        info.discordId    = r.discordId;
        info.balance      = r.balance;
        info.isOnline     = r.isOnline;
        info.lastOnline   = r.lastOnline;
        info.registeredAt = r.registeredAt; // Ensure this is mapped
        info.fetchedAt    = r.fetchedAt;

        info.mapLocation = null;

        return info;
    }
}