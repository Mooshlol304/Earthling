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
import xyz.moosh.earthling.client.EarthlingClient;
import xyz.moosh.earthling.client.model.*;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Typed wrapper around the EarthMC REST API v4.
 * All mutation lookups use POST with {"query": [...]}.
 */
public class EarthMCApi {

    private static final String BASE = "https://api.earthmc.net/v4";
    private static final Gson   GSON = new GsonBuilder().create();
    private static final String STAFF_URL =
            "https://raw.githubusercontent.com/veyronity/staff/master/staff.json";

    private final HttpClient http;

    public EarthMCApi(HttpClient http) {
        this.http = http;
    }

    // ── Players ───────────────────────────────────────────────────────────

    public CompletableFuture<Resident> getPlayer(String nameOrUuid) {
        // Strip emoji/rank prefixes — Dynmap names look like "🐉☄💠 FG_kuana"
        // Real MC names are always plain ASCII, so just nuke everything else
        String clean = nameOrUuid.replaceAll("[^\\x00-\\x7F]", "").trim();
        if (clean.isEmpty()) clean = nameOrUuid; // fallback just in case

        if (!clean.equals(nameOrUuid))
            EarthlingClient.LOGGER.info("[EarthMCApi] Stripped display name: '{}' -> '{}'", nameOrUuid, clean);

        return fetchPlayerDirect(clean);
    }

    private CompletableFuture<Resident> fetchPlayerDirect(String nameOrUuid) {
        return http.postRaw(BASE + "/players", query(nameOrUuid))
                .thenApply(body -> {
                    EarthlingClient.LOGGER.info("[EarthMCApi] Player response ({}): {}",
                            nameOrUuid, body.length() > 300 ? body.substring(0, 300) + "..." : body);
                    try {
                        JsonArray arr = GSON.fromJson(body, JsonArray.class);
                        if (arr == null || arr.isEmpty()) {
                            EarthlingClient.LOGGER.info("[EarthMCApi] Empty array — player '{}' not found or opted out.", nameOrUuid);
                            return null;
                        }
                        return parseResident(arr.get(0).getAsJsonObject());
                    } catch (Exception e) {
                        EarthlingClient.LOGGER.warn("[EarthMCApi] Parse error for player '{}': {}", nameOrUuid, e.getMessage());
                        return null;
                    }
                }).exceptionally(e -> {
                    EarthlingClient.LOGGER.warn("[EarthMCApi] Network error for player '{}': {}", nameOrUuid, e.getMessage());
                    return null;
                });
    }

    /** Batch lookup — returns all residents found for the given names/UUIDs. */
    public CompletableFuture<List<Resident>> getPlayersBatch(List<String> names) {
        if (names.isEmpty()) return CompletableFuture.completedFuture(new ArrayList<>());
        JsonObject body = new JsonObject();
        JsonArray  q    = new JsonArray();
        names.forEach(q::add);
        body.add("query", q);
        return http.postRaw(BASE + "/players", GSON.toJson(body)).thenApply(response -> {
            List<Resident> result = new ArrayList<>();
            try {
                JsonArray arr = GSON.fromJson(response, JsonArray.class);
                if (arr != null) for (JsonElement el : arr)
                    if (el.isJsonObject()) result.add(parseResident(el.getAsJsonObject()));
            } catch (Exception e) {
                EarthlingClient.LOGGER.warn("[EarthMCApi] Batch player parse error: {}", e.getMessage());
            }
            return result;
        }).exceptionally(e -> new ArrayList<>());
    }

    // ── Towns ─────────────────────────────────────────────────────────────

    public CompletableFuture<Town> getTown(String nameOrUuid) {
        return http.postRaw(BASE + "/towns", query(nameOrUuid))
                .thenApply(body -> {
                    try {
                        JsonArray arr = GSON.fromJson(body, JsonArray.class);
                        if (arr == null || arr.isEmpty()) return null;
                        return parseTown(arr.get(0).getAsJsonObject());
                    } catch (Exception e) {
                        EarthlingClient.LOGGER.warn("[EarthMCApi] Parse error for town '{}': {}", nameOrUuid, e.getMessage());
                        return null;
                    }
                }).exceptionally(e -> null);
    }

    /** Batch lookup — returns all towns found for the given names/UUIDs. */
    public CompletableFuture<List<Town>> getTownsBatch(List<String> names) {
        if (names.isEmpty()) return CompletableFuture.completedFuture(new ArrayList<>());
        JsonObject body = new JsonObject();
        JsonArray  q    = new JsonArray();
        names.forEach(q::add);
        body.add("query", q);
        return http.postRaw(BASE + "/towns", GSON.toJson(body)).thenApply(response -> {
            List<Town> result = new ArrayList<>();
            try {
                JsonArray arr = GSON.fromJson(response, JsonArray.class);
                if (arr != null) for (JsonElement el : arr)
                    if (el.isJsonObject()) result.add(parseTown(el.getAsJsonObject()));
            } catch (Exception e) {
                EarthlingClient.LOGGER.warn("[EarthMCApi] Batch town parse error: {}", e.getMessage());
            }
            return result;
        }).exceptionally(e -> new ArrayList<>());
    }

    // ── Nations ───────────────────────────────────────────────────────────

    public CompletableFuture<Nation> getNation(String nameOrUuid) {
        return http.postRaw(BASE + "/nations", query(nameOrUuid))
                .thenApply(body -> {
                    try {
                        JsonArray arr = GSON.fromJson(body, JsonArray.class);
                        if (arr == null || arr.isEmpty()) return null;
                        return parseNation(arr.get(0).getAsJsonObject());
                    } catch (Exception e) {
                        EarthlingClient.LOGGER.warn("[EarthMCApi] Parse error for nation '{}': {}", nameOrUuid, e.getMessage());
                        return null;
                    }
                }).exceptionally(e -> null);
    }

    // ── Online players ────────────────────────────────────────────────────

    /**
     * GET /online — returns UUIDs of all currently online players.
     * UUIDs are used instead of names to avoid issues with emoji/special-char nicknames.
     * Pass these directly into getPlayersBatch() — the API accepts both names and UUIDs.
     */
    public CompletableFuture<List<String>> getOnlinePlayers() {
        return http.getRaw(BASE + "/online").thenApply(body -> {
            List<String> uuids = new ArrayList<>();
            try {
                JsonObject obj     = GSON.fromJson(body, JsonObject.class);
                JsonArray  players = arr(obj, "players");
                if (players != null) for (JsonElement el : players)
                    if (el.isJsonObject()) {
                        String uuid = str(el.getAsJsonObject(), "uuid");
                        if (uuid != null) uuids.add(uuid);
                    }
            } catch (Exception e) {
                EarthlingClient.LOGGER.warn("[EarthMCApi] Failed to parse online players: {}", e.getMessage());
            }
            return uuids;
        }).exceptionally(e -> new ArrayList<>());
    }

    // ── Staff ─────────────────────────────────────────────────────────────

    public CompletableFuture<java.util.Map<String, java.util.List<String>>> getStaff() {
        return http.getRaw(STAFF_URL).thenApply(body -> {
            java.util.Map<String, java.util.List<String>> staff = new java.util.LinkedHashMap<>();
            try {
                JsonObject root = GSON.fromJson(body, JsonObject.class);
                if (root == null) return staff;
                String[] order = { "owner", "admin", "developer", "moderator", "helper" };
                for (String role : order) {
                    if (!root.has(role) || !root.get(role).isJsonArray()) continue;
                    JsonArray array = root.getAsJsonArray(role);
                    java.util.List<String> uuids = new java.util.ArrayList<>();
                    for (JsonElement element : array)
                        if (!element.isJsonNull()) uuids.add(element.getAsString());
                    staff.put(role, uuids);
                }
            } catch (Exception e) {
                EarthlingClient.LOGGER.warn("[EarthMCApi] Failed to parse staff list: {}", e.getMessage());
            }
            return staff;
        }).exceptionally(e -> {
            EarthlingClient.LOGGER.warn("[EarthMCApi] Failed to fetch staff list: {}", e.getMessage());
            return new java.util.LinkedHashMap<>();
        });
    }

    // ── Location ──────────────────────────────────────────────────────────

    /** POST /location — returns Towny info at the given world coords, or null if request fails. */
    public CompletableFuture<JsonObject> getLocationInfo(int x, int z) {
        JsonObject body  = new JsonObject();
        JsonArray  query = new JsonArray();
        JsonArray  coord = new JsonArray();
        coord.add(x);
        coord.add(z);
        query.add(coord);
        body.add("query", query);
        return http.postRaw(BASE + "/location", GSON.toJson(body)).thenApply(response -> {
            try {
                JsonArray arr = GSON.fromJson(response, JsonArray.class);
                if (arr != null && !arr.isEmpty() && arr.get(0).isJsonObject())
                    return arr.get(0).getAsJsonObject();
            } catch (Exception ignored) {}
            return null;
        }).exceptionally(e -> null);
    }

    public CompletableFuture<Boolean> isWilderness(int x, int z) {
        return getLocationInfo(x, z).thenApply(location -> {
            // FIX: EarthMC API returns an empty array or [null] for wilderness blocks,
            // which causes getLocationInfo to return null. Therefore, null means it IS wilderness!
            if (location == null) return true;
            if (location.has("town") && !location.get("town").isJsonNull()) return false;
            return true;
        });
    }

    // ── Nearby ────────────────────────────────────────────────────────────

    /** POST /nearby — returns names of towns within radius blocks of the given coords. */
    public CompletableFuture<List<String>> getNearbyTownNames(int x, int z, int radius) {
        JsonObject body   = new JsonObject();
        JsonArray  query  = new JsonArray();
        JsonObject q      = new JsonObject();
        JsonArray  target = new JsonArray();
        target.add(x);
        target.add(z);
        q.addProperty("target_type", "COORDINATE");
        q.add("target", target);
        q.addProperty("search_type", "TOWN");
        q.addProperty("radius", radius);
        query.add(q);
        body.add("query", query);
        return http.postRaw(BASE + "/nearby", GSON.toJson(body)).thenApply(response -> {
            List<String> names = new ArrayList<>();
            try {
                JsonArray outer = GSON.fromJson(response, JsonArray.class);
                if (outer != null && !outer.isEmpty() && outer.get(0).isJsonArray())
                    for (JsonElement el : outer.get(0).getAsJsonArray())
                        if (el.isJsonObject()) {
                            String n = str(el.getAsJsonObject(), "name");
                            if (n != null) names.add(n);
                        }
            } catch (Exception ignored) {}
            return names;
        }).exceptionally(e -> new ArrayList<>());
    }

    // ── Parsers ───────────────────────────────────────────────────────────

    private Resident parseResident(JsonObject o) {
        Resident r   = new Resident();
        r.name       = str(o, "name");
        r.uuid       = str(o, "uuid");
        r.discordId  = str(o, "discord");
        r.isOnline   = bool(nested(o, "status"), "isOnline");
        r.balance    = dbl(nested(o, "stats"),   "balance");

        JsonObject timestamps = nested(o, "timestamps");
        r.lastOnline   = lng(timestamps, "lastOnline");
        r.registeredAt = lng(timestamps, "registered");

        r.fetchedAt  = System.currentTimeMillis();

        JsonObject townObj = nested(o, "town");
        if (townObj != null) r.town = str(townObj, "name");

        JsonObject nationObj = nested(o, "nation");
        if (nationObj != null) r.nation = str(nationObj, "name");

        EarthlingClient.LOGGER.info("[EarthMCApi] Parsed resident: name={} registeredAt={}",
                r.name, r.registeredAt);
        return r;
    }

    private Town parseTown(JsonObject o) {
        Town t      = new Town();
        t.name      = str(o, "name");
        t.fetchedAt = System.currentTimeMillis();

        JsonObject mayorObj = nested(o, "mayor");
        if (mayorObj != null) t.mayor = str(mayorObj, "name");

        JsonObject nationObj = nested(o, "nation");
        if (nationObj != null) t.nation = str(nationObj, "name");

        JsonObject stats  = nested(o, "stats");
        t.gold         = dbl(stats,    "balance");
        t.numChunks    = intVal(stats, "numTownBlocks");
        t.numResidents = intVal(stats, "numResidents");

        JsonObject status = nested(o, "status");
        t.isOpen             = bool(status, "isOpen");
        t.canOutsidersSpawn  = bool(status, "canOutsidersSpawn");
        t.isPublic   = bool(status, "isPublic");
        t.isPeaceful = bool(status, "isNeutral");
        t.isCapital  = bool(status, "isCapital");

        JsonObject coords = nested(o, "coordinates");
        if (coords != null) {
            JsonObject spawn = nested(coords, "spawn");
            if (spawn != null) {
                String world = str(spawn, "world");
                t.spawn = new Location(dbl(spawn, "x"), dbl(spawn, "y"), dbl(spawn, "z"),
                        world != null ? world : "earth");
            }
        }

        JsonArray residents = arr(o, "residents");
        if (residents != null) {
            t.residents = new ArrayList<>();
            for (JsonElement el : residents)
                if (el.isJsonObject()) {
                    String rName = str(el.getAsJsonObject(), "name");
                    if (rName != null) t.residents.add(rName);
                }
        }

        return t;
    }

    private Nation parseNation(JsonObject o) {
        Nation n    = new Nation();
        n.name      = str(o, "name");
        n.fetchedAt = System.currentTimeMillis();

        JsonObject kingObj = nested(o, "king");
        if (kingObj != null) n.king = str(kingObj, "name");

        JsonObject capitalObj = nested(o, "capital");
        if (capitalObj != null) n.capital = str(capitalObj, "name");

        JsonObject stats  = nested(o, "stats");
        n.gold         = dbl(stats,    "balance");
        n.numTowns     = intVal(stats, "numTowns");
        n.numResidents = intVal(stats, "numResidents");

        return n;
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    private String query(String value) {
        JsonObject body = new JsonObject();
        JsonArray  arr  = new JsonArray();
        arr.add(value);
        body.add("query", arr);
        return GSON.toJson(body);
    }

    private JsonObject nested(JsonObject o, String key) {
        if (o == null || !o.has(key) || !o.get(key).isJsonObject()) return null;
        return o.getAsJsonObject(key);
    }

    private JsonArray arr(JsonObject o, String key) {
        if (o == null || !o.has(key) || !o.get(key).isJsonArray()) return null;
        return o.getAsJsonArray(key);
    }

    private String str(JsonObject o, String key) {
        if (o == null || !o.has(key) || o.get(key).isJsonNull()) return null;
        return o.get(key).getAsString();
    }

    private boolean bool(JsonObject o, String key) {
        if (o == null || !o.has(key)) return false;
        return o.get(key).getAsBoolean();
    }

    private double dbl(JsonObject o, String key) {
        if (o == null || !o.has(key)) return 0.0;
        return o.get(key).getAsDouble();
    }

    private long lng(JsonObject o, String key) {
        if (o == null || !o.has(key)) return 0L;
        return o.get(key).getAsLong();
    }

    private int intVal(JsonObject o, String key) {
        if (o == null || !o.has(key)) return 0;
        return o.get(key).getAsInt();
    }

    // ── All towns (for autocomplete) ──────────────────────────────────────

    /**
     * GET /towns — returns names of every registered town.
     * Used to populate autocomplete for /ert goto.
     * Response is large (~3000 entries) so callers should cache aggressively.
     */
    public CompletableFuture<List<String>> getAllTownNames() {
        return http.getRaw(BASE + "/towns").thenApply(body -> {
            List<String> names = new ArrayList<>();
            try {
                JsonArray arr = GSON.fromJson(body, JsonArray.class);
                if (arr != null) for (JsonElement el : arr)
                    if (el.isJsonObject()) {
                        String n = str(el.getAsJsonObject(), "name");
                        if (n != null) names.add(n);
                    }
            } catch (Exception e) {
                EarthlingClient.LOGGER.warn("[EarthMCApi] getAllTownNames parse error: {}", e.getMessage());
            }
            return names;
        }).exceptionally(e -> new ArrayList<>());
    }

    // ── Discord username lookup ───────────────────────────────────────────

    /**
     * Resolves a Discord user ID to a username via discordlookup.mesalytic.moe.
     * This is a public third-party API — requires no token.
     * Returns the username string (e.g. "mooshbrixa"), or null on failure.
     */
    public CompletableFuture<String> resolveDiscordUsername(String userId) {
        return http.getRaw("https://discordlookup.mesalytic.moe/v1/user/" + userId)
                .thenApply(body -> {
                    try {
                        JsonObject obj = GSON.fromJson(body, JsonObject.class);
                        if (obj != null && obj.has("username")
                                && !obj.get("username").isJsonNull())
                            return obj.get("username").getAsString();
                    } catch (Exception ignored) {}
                    return null;
                })
                .exceptionally(e -> null);
    }
}