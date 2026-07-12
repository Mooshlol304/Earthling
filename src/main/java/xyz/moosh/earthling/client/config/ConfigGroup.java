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

package xyz.moosh.earthling.client.config;

import java.util.*;

/**
 * A named group of {@link ConfigOption}s belonging to one module or widget.
 * Modules declare options in their constructor via the builder methods.
 *
 * <pre>
 *   config.addToggle("enabled", "Enabled", true);
 *   config.addColor("color",   "Colour",  0xFFFFFFFF);
 *   config.addSlider("scale",  "Scale",   1.0f, 0.5f, 2.0f);
 * </pre>
 */
public class ConfigGroup {

    private final String                     id;
    private final List<ConfigOption<?>>      options = new ArrayList<>();
    private final Map<String, ConfigOption<?>> byKey = new LinkedHashMap<>();

    public ConfigGroup(String id) {
        this.id = id;
    }

    // ── Typed builder helpers ─────────────────────────────────────────────

    public ConfigOption<Boolean> addToggle(String key, String label, boolean defaultValue) {
        return register(new ConfigOption<>(key, label, ConfigType.BOOLEAN, defaultValue));
    }

    public ConfigOption<Integer> addColor(String key, String label, int defaultArgb) {
        return register(new ConfigOption<>(key, label, ConfigType.COLOR, defaultArgb));
    }

    public ConfigOption<Float> addSlider(String key, String label, float defaultValue,
                                         float min, float max) {
        return register(new ConfigOption<>(key, label, ConfigType.FLOAT, defaultValue)
                .range(min, max));
    }

    public ConfigOption<Integer> addIntSlider(String key, String label, int defaultValue,
                                              int min, int max) {
        return register(new ConfigOption<>(key, label, ConfigType.INTEGER, defaultValue)
                .range(min, max));
    }

    public ConfigOption<Integer> addInt(String key, String label, int defaultValue) {
        return register(new ConfigOption<>(key, label, ConfigType.INTEGER, defaultValue));
    }

    public ConfigOption<String> addText(String key, String label, String defaultValue) {
        return register(new ConfigOption<>(key, label, ConfigType.STRING, defaultValue));
    }

    public <E extends Enum<E>> ConfigOption<E> addEnum(String key, String label, E defaultValue) {
        return register(new ConfigOption<>(key, label, ConfigType.ENUM, defaultValue));
    }

    // ── Internal ──────────────────────────────────────────────────────────

    private <T> ConfigOption<T> register(ConfigOption<T> option) {
        options.add(option);
        byKey.put(option.getKey(), option);
        return option;
    }

    // ── Accessors ─────────────────────────────────────────────────────────

    public String getId() { return id; }

    public List<ConfigOption<?>> getOptions() {
        return Collections.unmodifiableList(options);
    }

    @SuppressWarnings("unchecked")
    public <T> ConfigOption<T> get(String key) {
        return (ConfigOption<T>) byKey.get(key);
    }

    @SuppressWarnings("unchecked")
    public <T> T getValue(String key) {
        ConfigOption<T> opt = (ConfigOption<T>) byKey.get(key);
        if (opt == null) throw new IllegalArgumentException("Unknown config key in group '"
                + id + "': " + key);
        return opt.get();
    }
}