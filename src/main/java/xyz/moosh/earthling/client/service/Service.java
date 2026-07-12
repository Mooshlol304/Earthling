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

package xyz.moosh.earthling.client.service;

import xyz.moosh.earthling.client.EarthlingClient;
import xyz.moosh.earthling.client.api.IService;
import xyz.moosh.earthling.client.event.EventBus;

/**
 * Base class for long-running background services.
 * Services are initialised once at startup and shut down when the client closes.
 * They are the correct place to hold shared state that multiple modules may need.
 */
public abstract class Service implements IService {

    protected final String   id;
    protected final EventBus eventBus;

    protected Service(String id, EventBus eventBus) {
        this.id       = id;
        this.eventBus = eventBus;
    }

    @Override public String getId() { return id; }

    // Subclasses override init() and shutdown() as needed.
    @Override public void init()     {}
    @Override public void shutdown() {}

    protected void log(String msg, Object... args) {
        EarthlingClient.LOGGER.debug("[{}] " + msg, id, args);
    }
}
