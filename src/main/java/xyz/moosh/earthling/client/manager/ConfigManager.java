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

package xyz.moosh.earthling.client.manager;

import com.google.gson.*;
import net.fabricmc.loader.api.FabricLoader;
import xyz.moosh.earthling.client.EarthlingClient;
import xyz.moosh.earthling.client.config.ConfigGroup;
import xyz.moosh.earthling.client.config.ConfigOption;

import java.io.*;
import java.nio.file.*;
import java.util.*;

/**
 * Persists all module and widget config to {@code .minecraft/config/earthling.json}.
 *
 * <p>Every {@link ConfigGroup} is serialised as a flat JSON object keyed by
 * {@code "<groupId>.<optionKey>"}. This keeps the file human-readable and
 * avoids nested structures that are hard to merge.
 */
public class ConfigManager {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private final Path configFile;
    private JsonObject root = new JsonObject();

    /** Groups registered by ModuleManager / WidgetManager for persistence. */
    private final List<ConfigGroup> registeredGroups = new ArrayList<>();

    public ConfigManager() {
        this.configFile = FabricLoader.getInstance()
                .getConfigDir()
                .resolve("earthling.json");
    }

    // ── Registration ──────────────────────────────────────────────────────

    /**
     * Register a config group so its values are loaded/saved automatically.
     * Called by ModuleManager and WidgetManager during {@code init()}.
     */
    public void register(ConfigGroup group) {
        registeredGroups.add(group);
    }

    // ── Load / Save ───────────────────────────────────────────────────────

    public void load() {
        if (!Files.exists(configFile)) {
            EarthlingClient.LOGGER.info("No config file found, using defaults.");
            return;
        }

        try (Reader reader = Files.newBufferedReader(configFile)) {
            root = GSON.fromJson(reader, JsonObject.class);
            if (root == null) root = new JsonObject();
            applyToGroups();
            EarthlingClient.LOGGER.info("Config loaded from {}", configFile);
        } catch (IOException | JsonParseException e) {
            EarthlingClient.LOGGER.error("Failed to load config: {}", e.getMessage());
        }
    }

    public void save() {
        collectFromGroups();
        try {
            Files.createDirectories(configFile.getParent());
            try (Writer writer = Files.newBufferedWriter(configFile)) {
                GSON.toJson(root, writer);
            }
        } catch (IOException e) {
            EarthlingClient.LOGGER.error("Failed to save config: {}", e.getMessage());
        }
    }

    // ── Internal ──────────────────────────────────────────────────────────

    private void applyToGroups() {
        for (ConfigGroup group : registeredGroups) {
            for (ConfigOption<?> opt : group.getOptions()) {
                String key = flatKey(group.getId(), opt.getKey());
                if (!root.has(key)) continue;

                JsonElement el = root.get(key);
                try {
                    applyElement(opt, el);
                } catch (Exception e) {
                    EarthlingClient.LOGGER.warn("Could not apply config key '{}': {}", key, e.getMessage());
                }
            }
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private void applyElement(ConfigOption<?> opt, JsonElement el) {
        switch (opt.getType()) {
            case BOOLEAN -> opt.set(el.getAsBoolean());
            case INTEGER, COLOR -> opt.set(el.getAsInt());
            case FLOAT   -> opt.set(el.getAsFloat());
            case STRING  -> opt.set(el.getAsString());
            case ENUM    -> {
                // Enum options store the name as a string; re-parse at runtime
                String name = el.getAsString();
                Object def = opt.getDefaultValue();
                if (def instanceof Enum<?> e) {
                    Enum found = Enum.valueOf((Class<Enum>) e.getClass(), name);
                    ((ConfigOption) opt).set(found);
                }
            }
        }
    }

    private void collectFromGroups() {
        root = new JsonObject();
        for (ConfigGroup group : registeredGroups) {
            for (ConfigOption<?> opt : group.getOptions()) {
                String key = flatKey(group.getId(), opt.getKey());
                Object val = opt.get();
                if (val instanceof Boolean b)  root.addProperty(key, b);
                else if (val instanceof Number n) root.addProperty(key, n);
                else if (val instanceof Enum<?> e) root.addProperty(key, e.name());
                else root.addProperty(key, String.valueOf(val));
            }
        }
    }

    private String flatKey(String groupId, String optionKey) {
        return groupId + "." + optionKey;
    }
}
