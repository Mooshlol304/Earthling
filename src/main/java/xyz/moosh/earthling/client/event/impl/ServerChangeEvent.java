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

import net.minecraft.client.Minecraft;
import xyz.moosh.earthling.client.event.Event;

/** Fired when the client joins a server or world. */
public class ServerChangeEvent extends Event {
    private final Minecraft client;

    public ServerChangeEvent(Minecraft client) { this.client = client; }

    public Minecraft getClient() { return client; }

    /** @return the server address, or {@code null} in singleplayer. */
    public String getAddress() {
        return client.getCurrentServer() != null
                ? client.getCurrentServer().ip
                : null;
    }
}