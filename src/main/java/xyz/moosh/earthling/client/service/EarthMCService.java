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

import xyz.moosh.earthling.client.event.EventBus;
import xyz.moosh.earthling.client.event.impl.ServerChangeEvent;
import xyz.moosh.earthling.client.event.impl.ServerDisconnectEvent;

import java.util.Set;

/**
 * Single source of truth for whether the client is connected to EarthMC.
 *
 * <p>This is the <em>only</em> class that knows EarthMC hostnames.
 * Modules never call {@code server.contains("earthmc")} directly; they call
 * {@link #isEarthMC()} instead.
 *
 * <pre>
 *   if (!earthMCService.isEarthMC()) return;
 * </pre>
 */
public class EarthMCService extends Service {

    private static final Set<String> EARTHMC_HOSTS = Set.of(
            "play.earthmc.net",
            "earthmc.net"
    );

    private boolean connected          = false;
    private boolean receivedServerChange = false;
    private String  currentServer      = null;

    public EarthMCService(EventBus eventBus) {
        super("earthmc", eventBus);
    }

    @Override
    public void init() {
        eventBus.subscribe(ServerChangeEvent.class,     this::onServerChange);
        eventBus.subscribe(ServerDisconnectEvent.class, this::onDisconnect);
    }

    // ── Event handlers ────────────────────────────────────────────────────

    private void onServerChange(ServerChangeEvent e) {
        receivedServerChange = true;

        String address = e.getAddress();
        currentServer  = address;
        connected      = address != null && isEarthMCAddress(address);

        if (connected) {
            log("Connected to EarthMC ({})", address);
        }
    }

    private void onDisconnect(ServerDisconnectEvent e) {
        if (receivedServerChange) {
            // Trailing disconnect from a server-switch — new connection state is
            // already set by onServerChange, so just clear the flag and do nothing.
            receivedServerChange = false;
        } else {
            // True disconnect (main menu, timeout, kick, etc.) — fully reset.
            connected     = false;
            currentServer = null;
        }
    }

    // ── Public API ────────────────────────────────────────────────────────

    /** @return {@code true} if currently connected to an EarthMC server. */
    public boolean isEarthMC() {
        return connected;
    }

    /** @return the raw server address, or {@code null} if not connected. */
    public String getCurrentServer() {
        return currentServer;
    }

    // ── Internal ──────────────────────────────────────────────────────────

    private boolean isEarthMCAddress(String address) {
        // Strip port if present
        String host = address.contains(":") ? address.substring(0, address.indexOf(':')) : address;
        host = host.toLowerCase();
        return EARTHMC_HOSTS.contains(host) || host.endsWith(".earthmc.net");
    }
}