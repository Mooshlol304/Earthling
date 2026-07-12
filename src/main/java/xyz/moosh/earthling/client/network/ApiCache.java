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

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Simple time-to-live cache keyed by a string.
 * Used by {@link EarthMCApi} to avoid hammering the API on every request.
 *
 * <pre>
 *   ApiCache&lt;Town&gt; cache = new ApiCache&lt;&gt;(30_000); // 30 s TTL
 *   cache.put("Ghent", town);
 *   cache.get("Ghent"); // returns town if still fresh, null if stale
 * </pre>
 *
 * @param <V> cached value type
 */
public class ApiCache<V> {

    private final long ttlMs;

    private final Map<String, Entry<V>> store = new LinkedHashMap<>() {
        // Evict the oldest entry when the map exceeds MAX_SIZE
        private static final int MAX_SIZE = 512;
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, Entry<V>> eldest) {
            return size() > MAX_SIZE;
        }
    };

    public ApiCache(long ttlMs) {
        this.ttlMs = ttlMs;
    }

    /** Store a value. Overwrites any existing entry for the same key. */
    public void put(String key, V value) {
        store.put(key.toLowerCase(), new Entry<>(value, System.currentTimeMillis()));
    }

    /**
     * Retrieve a cached value.
     *
     * @return the value, or {@code null} if absent or expired.
     */
    public V get(String key) {
        Entry<V> entry = store.get(key.toLowerCase());
        if (entry == null) return null;
        if (System.currentTimeMillis() - entry.timestamp > ttlMs) {
            store.remove(key.toLowerCase());
            return null;
        }
        return entry.value;
    }

    /** Remove a specific key. */
    public void invalidate(String key) {
        store.remove(key.toLowerCase());
    }

    /** Wipe the entire cache. */
    public void clear() {
        store.clear();
    }

    /** @return number of (possibly stale) entries currently held. */
    public int size() {
        return store.size();
    }

    // ── Internal ──────────────────────────────────────────────────────────

    private record Entry<V>(V value, long timestamp) {}
}
