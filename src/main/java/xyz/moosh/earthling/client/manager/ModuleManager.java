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

import xyz.moosh.earthling.client.EarthlingClient;
import xyz.moosh.earthling.client.event.EventBus;
import xyz.moosh.earthling.client.module.Category;
import xyz.moosh.earthling.client.module.*;
import xyz.moosh.earthling.client.module.Module;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Owns every {@link Module} instance.
 *
 * <p>To add a new module:
 * <ol>
 *   <li>Create {@code YourModule extends Module}.</li>
 *   <li>Call {@code register(new YourModule())} inside {@link #registerAll()}.</li>
 *   <li>Nothing else.</li>
 * </ol>
 */
public class ModuleManager {

    private final EventBus      eventBus;
    private final ConfigManager configManager;

    private final List<Module>                          modules = new ArrayList<>();
    private final Map<Class<? extends Module>, Module>  byType  = new HashMap<>();
    private final Map<String, Module>                   byId    = new LinkedHashMap<>();

    public ModuleManager(EventBus eventBus, ConfigManager configManager) {
        this.eventBus      = eventBus;
        this.configManager = configManager;
    }

    public void init() {
        registerAll();
        EarthlingClient.LOGGER.info("ModuleManager: {} module(s) registered.", modules.size());
    }

    // ── Module registration ───────────────────────────────────────────────

    private void registerAll() {
        register(new ChatPreview());
        register(new PlayerAffiliations());
        register(new ExpOverlay());
        register(new Translation());
    }

    private void register(Module module) {
        if (byId.containsKey(module.getId())) {
            throw new IllegalStateException("Duplicate module id: " + module.getId());
        }
        module.injectEventBus(eventBus);
        configManager.register(module.getConfig());

        modules.add(module);
        byType.put(module.getClass(), module);
        byId.put(module.getId(), module);
    }

    // ── Queries ───────────────────────────────────────────────────────────

    public List<Module> getModules() {
        return Collections.unmodifiableList(modules);
    }

    public List<Module> getByCategory(Category category) {
        return modules.stream()
                .filter(m -> m.getCategory() == category)
                .collect(Collectors.toList());
    }

    @SuppressWarnings("unchecked")
    public <T extends Module> T get(Class<T> type) {
        return (T) byType.get(type);
    }

    public Optional<Module> get(String id) {
        return Optional.ofNullable(byId.get(id));
    }

    // ── Bulk operations ───────────────────────────────────────────────────

    public void disableAll() {
        modules.forEach(m -> m.setEnabled(false));
    }
}
