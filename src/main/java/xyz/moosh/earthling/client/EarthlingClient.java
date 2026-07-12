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

package xyz.moosh.earthling.client;

import net.fabricmc.api.ClientModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import xyz.moosh.earthling.client.config.ConfigGroup;
import xyz.moosh.earthling.client.config.ConfigOption;
import xyz.moosh.earthling.client.event.EventBus;
import xyz.moosh.earthling.client.manager.*;

public class EarthlingClient implements ClientModInitializer {

    public static final String MOD_ID = "earthling";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private static EarthlingClient instance;

    private EventBus        eventBus;
    private ServiceManager  serviceManager;
    private ConfigManager   configManager;
    private ModuleManager   moduleManager;
    private WidgetManager   widgetManager;
    private CommandManager  commandManager;

    private ConfigGroup generalConfig;
    private ConfigOption<String> townlessMessage;

    @Override
    public void onInitializeClient() {
        instance = this;
        LOGGER.info("Initialising Earthling...");

        eventBus       = new EventBus();
        serviceManager = new ServiceManager(eventBus);
        configManager  = new ConfigManager();
        moduleManager  = new ModuleManager(eventBus, configManager);
        widgetManager  = new WidgetManager(eventBus, configManager);
        commandManager = new CommandManager();

        serviceManager.init();

        // ── General Config Setup ──────────────────────────────────────────

        generalConfig = new ConfigGroup("general");
        townlessMessage = generalConfig.addText("townlessMessage", "Townless Message",
                "Hey @username, welcome! Need a head start? We offer free gear, housing and help! Interested? Just type '/t join @town'");

        // CRITICAL FIX: You must register the group to the ConfigManager
        // Otherwise, it won't be saved to the .json file!
        configManager.register(generalConfig);

        // ── Initialize Managers ───────────────────────────────────────────

        // moduleManager.init() and widgetManager.init() also call register()
        // internally for their specific groups.
        moduleManager.init();
        widgetManager.init();
        commandManager.init();

        // configManager.load() MUST come after all groups have been registered.
        configManager.load();

        eventBus.init();

        LOGGER.info("Earthling ready. {} module(s) loaded.", moduleManager.getModules().size());
    }

    public static EarthlingClient getInstance() { return instance; }

    public EventBus       getEventBus()       { return eventBus; }
    public ServiceManager getServiceManager() { return serviceManager; }
    public ConfigManager  getConfigManager()  { return configManager; }
    public ModuleManager  getModuleManager()  { return moduleManager; }
    public WidgetManager  getWidgetManager()  { return widgetManager; }
    public CommandManager getCommandManager() { return commandManager; }

    public ConfigGroup getGeneralConfig() { return generalConfig; }

    /** Returns the Townless Invite Message Config Option */
    public ConfigOption<String> getTownlessMessage() { return townlessMessage; }
}