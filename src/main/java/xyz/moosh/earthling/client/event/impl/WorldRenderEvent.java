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

package xyz.moosh.earthling.client.event.impl;

import xyz.moosh.earthling.client.event.Event;

/**
 * Fired after entities are rendered in the world.
 * Context object added in Phase 6 when world rendering features are implemented.
 */
public class WorldRenderEvent extends Event {
    // Intentionally empty for now — world render features come in Phase 6.
    // WorldRenderContext will be added then to avoid pulling in the Fabric
    // rendering API before it is actually needed.
}