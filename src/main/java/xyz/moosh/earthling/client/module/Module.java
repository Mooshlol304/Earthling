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

package xyz.moosh.earthling.client.module;

import xyz.moosh.earthling.client.api.IModule;
import xyz.moosh.earthling.client.config.ConfigGroup;
import xyz.moosh.earthling.client.config.ConfigOption;
import xyz.moosh.earthling.client.event.EventBus;

/**
 * Base class for all gameplay feature modules.
 *
 * <h3>Conventions</h3>
 * <ul>
 *   <li>Subclass name format: {@code AutoHudModule}, {@code ExperienceTextModule}, etc.</li>
 *   <li>Declare config options in the constructor via {@code config.addXxx(…)}.</li>
 *   <li>Subscribe to events in {@link #onEnable()} and unsubscribe in {@link #onDisable()}.</li>
 *   <li>Never reference another module directly — use services.</li>
 * </ul>
 *
 * <pre>
 * public class ExampleModule extends Module {
 *
 *     private final ConfigOption<Boolean> showLabel;
 *     private EventBus.EventListener<TickEvent> tickListener;
 *
 *     public ExampleModule() {
 *         super("example", "Example", Category.HUD);
 *         showLabel = config.addToggle("show_label", "Show Label", true);
 *     }
 *
 *     {@literal @}Override public void onEnable() {
 *         tickListener = eventBus.subscribe(TickEvent.class, e -> tick());
 *     }
 *
 *     {@literal @}Override public void onDisable() {
 *         eventBus.unsubscribe(TickEvent.class, tickListener);
 *     }
 * }
 * </pre>
 */
public abstract class Module implements IModule {

    protected final String       id;
    protected final String       name;
    protected final Category     category;
    protected final ConfigGroup  config;
    protected       EventBus     eventBus;  // injected by ModuleManager

    // The 'enabled' toggle is always the first option in every module's config
    private final ConfigOption<Boolean> enabledOption;
    private boolean enabled = false;

    protected Module(String id, String name, Category category) {
        this.id       = id;
        this.name     = name;
        this.category = category;
        this.config   = new ConfigGroup(id);

        enabledOption = config.addToggle("enabled", "Enabled", false)
                .onChange(this::setEnabled);
    }

    // ── Lifecycle ─────────────────────────────────────────────────────────

    /** Called by the module when it needs to subscribe to events. */
    @Override
    public abstract void onEnable();

    /** Called by the module when it needs to unsubscribe from events. */
    @Override
    public abstract void onDisable();

    // ── Enable toggle ─────────────────────────────────────────────────────

    @Override
    public void setEnabled(boolean enabled) {
        if (this.enabled == enabled) return;
        this.enabled = enabled;
        enabledOption.set(enabled);    // keep config in sync
        if (enabled) onEnable();
        else         onDisable();
    }

    public void toggle() {
        setEnabled(!enabled);
    }

    // ── IModule ───────────────────────────────────────────────────────────

    @Override public String      getId()       { return id; }
    @Override public String      getName()     { return name; }
    @Override public Category    getCategory() { return category; }
    @Override public boolean     isEnabled()   { return enabled; }
    @Override public ConfigGroup getConfig()   { return config; }

    // ── Internal: called by ModuleManager after construction ──────────────

    public void injectEventBus(EventBus eventBus) {
        this.eventBus = eventBus;
    }
}
