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

import java.util.function.Consumer;

/**
 * A single configurable value belonging to a {@link ConfigGroup}.
 *
 * @param <T> the value type
 */
public class ConfigOption<T> {

    private final String     key;
    private final String     label;
    private final ConfigType type;
    private final T          defaultValue;
    private       T          value;

    private Number      min;
    private Number      max;
    private Consumer<T> changeListener;

    public ConfigOption(String key, String label, ConfigType type, T defaultValue) {
        this.key          = key;
        this.label        = label;
        this.type         = type;
        this.defaultValue = defaultValue;
        this.value        = defaultValue;
    }

    // ── Fluent builder helpers ────────────────────────────────────────────

    public ConfigOption<T> range(Number min, Number max) {
        this.min = min;
        this.max = max;
        return this;
    }

    public ConfigOption<T> onChange(Consumer<T> listener) {
        this.changeListener = listener;
        return this;
    }

    // ── Value access ──────────────────────────────────────────────────────

    public T get() { return value; }

    @SuppressWarnings("unchecked")
    public void set(Object newValue) {
        this.value = (T) newValue;
        if (changeListener != null) changeListener.accept(this.value);
    }

    public void reset() { set(defaultValue); }

    // ── Getters ───────────────────────────────────────────────────────────

    public String     getKey()          { return key; }
    public String     getLabel()        { return label; }
    public ConfigType getType()         { return type; }
    public T          getDefaultValue() { return defaultValue; }
    public Number     getMin()          { return min; }
    public Number     getMax()          { return max; }
}
