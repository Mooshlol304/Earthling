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

package xyz.moosh.earthling.client.network;

import com.google.gson.*;
import xyz.moosh.earthling.client.model.Location;
import xyz.moosh.earthling.client.EarthlingClient;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Fetches live player positions from the EarthMC Dynmap tile server.
 * Results are cached for 5 seconds to avoid hammering the endpoint.
 */
public class DynmapApi {

    private static final String URL  = "https://map.earthmc.net/tiles/players.json";
    private static final Gson   GSON = new Gson();

    private final HttpClient http;
    private final ApiCache<Map<String, Location>> cache = new ApiCache<>(5000);

    public DynmapApi(HttpClient http) {
        this.http = http;
    }

    public CompletableFuture<Map<String, Location>> getPlayers() {
        Map<String, Location> cached = cache.get("players");
        if (cached != null) {
            EarthlingClient.LOGGER.debug("[Dynmap] Using cached player list ({} players).", cached.size());
            return CompletableFuture.completedFuture(cached);
        }

        return http.getRaw(URL).thenApply(body -> {
            Map<String, Location> players = new HashMap<>();
            JsonObject root = GSON.fromJson(body, JsonObject.class);
            JsonArray array = root.getAsJsonArray("players");

            if (array == null) return players;

            for (JsonElement element : array) {
                JsonObject obj = element.getAsJsonObject();
                String name  = obj.get("name").getAsString();
                double x     = obj.get("x").getAsDouble();
                double y     = obj.get("y").getAsDouble();
                double z     = obj.get("z").getAsDouble();
                String world = obj.get("world").getAsString();
                players.put(name.toLowerCase(), new Location(x, y, z, world));
            }

            EarthlingClient.LOGGER.info("[Dynmap] Loaded {} visible players.", players.size());
            cache.put("players", players);
            return players;
        });
    }
}