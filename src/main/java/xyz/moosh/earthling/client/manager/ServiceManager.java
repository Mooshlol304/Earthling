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

import xyz.moosh.earthling.client.api.IService;
import xyz.moosh.earthling.client.event.EventBus;
import xyz.moosh.earthling.client.network.EarthMCApi;
import xyz.moosh.earthling.client.network.DynmapApi;
import xyz.moosh.earthling.client.network.HttpClient;
import xyz.moosh.earthling.client.service.*;

import java.util.*;

/**
 * Owns the lifecycle of every {@link IService}.
 * Register new services in {@link #init()} only.
 */
public class ServiceManager {

    private final EventBus       eventBus;
    private final List<IService> services = new ArrayList<>();
    private final Map<Class<? extends IService>, IService> byType = new HashMap<>();

    private HttpClient          httpClient;
    private EarthMCApi          earthMCApi;
    private DynmapApi           dynmapApi;
    private EarthMCService      earthMCService;
    private PlayerService       playerService;
    private TownService         townService;
    private NotificationService notificationService;
    private ChatTrackerService  chatTracker;

    public ServiceManager(EventBus eventBus) {
        this.eventBus = eventBus;
    }

    public void init() {
        httpClient = new HttpClient();

        earthMCApi = new EarthMCApi(httpClient);
        dynmapApi  = new DynmapApi(httpClient);

        earthMCService      = register(new EarthMCService(eventBus));
        playerService       = register(new PlayerService(eventBus, earthMCApi, dynmapApi));
        townService         = register(new TownService(eventBus, earthMCApi));
        notificationService = register(new NotificationService(eventBus));
        chatTracker         = register(new ChatTrackerService(eventBus));

        services.forEach(IService::init);
    }

    public void shutdown() {
        List<IService> reversed = new ArrayList<>(services);
        Collections.reverse(reversed);
        reversed.forEach(IService::shutdown);
        if (httpClient != null) httpClient.shutdown();
    }

    private <T extends IService> T register(T service) {
        services.add(service);
        byType.put(service.getClass(), service);
        return service;
    }

    public ChatTrackerService getChatTracker() {
        return chatTracker;
    }

    public EarthMCService      getEarthMCService()      { return earthMCService; }
    public PlayerService       getPlayerService()       { return playerService; }
    public TownService         getTownService()         { return townService; }
    public NotificationService getNotificationService() { return notificationService; }
    public EarthMCApi          getEarthMCApi()          { return earthMCApi; }
    public DynmapApi            getDynmapApi()           { return dynmapApi; }

    @SuppressWarnings("unchecked")
    public <T extends IService> T get(Class<T> type) {
        return (T) byType.get(type);
    }

    public List<IService> getServices() {
        return Collections.unmodifiableList(services);
    }
}