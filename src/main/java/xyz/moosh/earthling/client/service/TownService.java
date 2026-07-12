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

import xyz.moosh.earthling.client.event.EventBus;
import xyz.moosh.earthling.client.event.impl.ServerDisconnectEvent;
import xyz.moosh.earthling.client.model.Town;
import xyz.moosh.earthling.client.network.ApiCache;
import xyz.moosh.earthling.client.network.EarthMCApi;

import java.util.concurrent.CompletableFuture;

/**
 * Provides cached town lookups for the rest of the mod.
 *
 * <p>Cache TTL is 2 minutes — town data changes less frequently than player data.
 *
 * <pre>
 *   townService.getTown("Ghent")
 *       .thenAccept(town -> renderTownWidget(town));
 * </pre>
 */
public class TownService extends Service {

    private static final long CACHE_TTL_MS = 120_000L; // 2 min

    private final EarthMCApi        api;
    private final ApiCache<Town>    cache = new ApiCache<>(CACHE_TTL_MS);

    public TownService(EventBus eventBus, EarthMCApi api) {
        super("town_service", eventBus);
        this.api = api;
    }

    @Override
    public void init() {
        eventBus.subscribe(ServerDisconnectEvent.class, e -> cache.clear());
    }

    // ── Public API ────────────────────────────────────────────────────────

    /**
     * Look up a town by name, returning a cached result if available.
     *
     * @param name town name (case-insensitive)
     * @return future resolving to {@link Town}, or {@code null} if unknown
     */
    public CompletableFuture<Town> getTown(String name) {
        Town cached = cache.get(name);
        if (cached != null) return CompletableFuture.completedFuture(cached);

        return api.getTown(name).thenApply(town -> {
            if (town == null) return null;
            cache.put(name, town);
            return town;
        });
    }

    /** Force a fresh lookup, bypassing the cache. */
    public CompletableFuture<Town> refreshTown(String name) {
        cache.invalidate(name);
        return getTown(name);
    }

    /** @return cached town without triggering a network request, or {@code null}. */
    public Town getCachedTown(String name) {
        return cache.get(name);
    }
}
