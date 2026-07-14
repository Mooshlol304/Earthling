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
import xyz.moosh.earthling.client.event.impl.ServerChangeEvent;
import xyz.moosh.earthling.client.event.impl.ServerDisconnectEvent;
import xyz.moosh.earthling.client.manager.*;
import xyz.moosh.earthling.client.service.EarthMCService;
import xyz.moosh.earthling.client.util.MooshLibUtil;
import xyz.moosh.earthling.client.util.UpdateCheckUtil;

public class EarthlingClient implements ClientModInitializer {

    public static final String MOD_ID = "earthling";
    public static final boolean DEV = false;

    public static final Logger LOGGER = DEV
            ? LoggerFactory.getLogger(MOD_ID)
            : org.slf4j.helpers.NOPLogger.NOP_LOGGER;

    private static EarthlingClient instance;

    private EventBus        eventBus;
    private ServiceManager  serviceManager;
    private ConfigManager   configManager;
    private ModuleManager   moduleManager;
    private WidgetManager   widgetManager;
    private CommandManager  commandManager;

    // Cached so we don't do a lookup on every server event
    private EarthMCService earthMCService;

    private ConfigGroup generalConfig;
    private ConfigOption<String> townlessMessage;

    @Override
    public void onInitializeClient() {
        instance = this;

        MooshLibUtil.validateDependency();

        LOGGER.info("Initialising Earthling...");

        eventBus       = new EventBus();
        serviceManager = new ServiceManager(eventBus);
        configManager  = new ConfigManager();
        moduleManager  = new ModuleManager(eventBus, configManager);
        widgetManager  = new WidgetManager(eventBus, configManager);
        commandManager = new CommandManager();

        serviceManager.init();

        // ServiceManager exposes a typed getter — no string lookup needed
        earthMCService = serviceManager.getEarthMCService();

        // ── General Config Setup ──────────────────────────────────────────

        generalConfig = new ConfigGroup("general");
        townlessMessage = generalConfig.addText("townlessMessage", "Townless Message",
                "Hey @username, welcome! Need a head start? We offer free gear, housing and help! Interested? Just type '/t join @town'");

        configManager.register(generalConfig);

        // ── Initialize Managers ───────────────────────────────────────────

        moduleManager.init();
        widgetManager.init();
        commandManager.init();

        UpdateCheckUtil.init();

        // configManager.load() MUST come after all groups have been registered.
        configManager.load();

        eventBus.init();

        // ── EarthMC Server Guard ──────────────────────────────────────────
        // Subscribe AFTER EarthMCService (subscribed inside serviceManager.init()),
        // so isEarthMC() is already updated when our handlers fire.

        eventBus.subscribe(ServerChangeEvent.class,     this::onServerChange);
        eventBus.subscribe(ServerDisconnectEvent.class, this::onServerDisconnect);

        // Boot state: not on EarthMC yet
        setFeaturesActive(false);

        LOGGER.info("Earthling ready. {} module(s) loaded.", moduleManager.getModules().size());
    }

    // ── EarthMC feature gating ────────────────────────────────────────────

    private void onServerChange(ServerChangeEvent e) {
        // EarthMCService already updated its connected flag for this event
        setFeaturesActive(earthMCService.isEarthMC());
    }

    private void onServerDisconnect(ServerDisconnectEvent e) {
        // ServerChangeEvent for the new server may have already fired and updated
        // EarthMCService before this disconnect event from the old server arrives.
        // Only disable if we're genuinely not on EarthMC.
        if (!earthMCService.isEarthMC()) {
            setFeaturesActive(false);
        }
    }

    /**
     * Enables or disables all Earthling features based on whether the player
     * is connected to EarthMC.
     */
    private void setFeaturesActive(boolean active) {
        if (active) {
            LOGGER.info("Connected to EarthMC – enabling Earthling features.");
            moduleManager.enable();
            widgetManager.enable();
        } else {
            LOGGER.info("Not on EarthMC – disabling Earthling features.");
            moduleManager.disable();
            widgetManager.disable();
        }
    }

    // ── Convenience ───────────────────────────────────────────────────────

    /** Quick check for modules/utils that need to gate themselves. */
    public boolean isOnEarthMC() {
        return earthMCService != null && earthMCService.isEarthMC();
    }

    // ── Getters ───────────────────────────────────────────────────────────

    public static EarthlingClient getInstance() { return instance; }

    public EventBus        getEventBus()        { return eventBus; }
    public ServiceManager  getServiceManager()  { return serviceManager; }
    public ConfigManager   getConfigManager()   { return configManager; }
    public ModuleManager   getModuleManager()   { return moduleManager; }
    public WidgetManager   getWidgetManager()   { return widgetManager; }
    public CommandManager  getCommandManager()  { return commandManager; }
    public EarthMCService  getEarthMCService()  { return earthMCService; }

    public ConfigGroup          getGeneralConfig()   { return generalConfig; }
    public ConfigOption<String> getTownlessMessage() { return townlessMessage; }
}