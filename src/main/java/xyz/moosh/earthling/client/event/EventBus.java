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

package xyz.moosh.earthling.client.event;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphics;
import xyz.moosh.earthling.client.EarthlingClient;
import xyz.moosh.earthling.client.event.impl.*;

import java.util.*;
import java.util.function.Consumer;

/**
 * Central event bus. Modules and services subscribe to typed events here.
 * Bridges Fabric's event system into Earthling events so the rest of the
 * codebase never imports Fabric event APIs directly.
 *
 * <pre>
 *   eventBus.subscribe(TickEvent.class, e -> doSomething());
 * </pre>
 */
public class EventBus {

    private final Map<Class<? extends Event>, List<EventListener<? extends Event>>> listeners
            = new LinkedHashMap<>();




    // ── Subscribe / Unsubscribe ───────────────────────────────────────────

    public <T extends Event> EventListener<T> subscribe(Class<T> eventClass, Consumer<T> listener) {
        EventListener<T> wrapped = new EventListener<>(listener);
        listeners.computeIfAbsent(eventClass, k -> new ArrayList<>()).add(wrapped);
        return wrapped;
    }

    public <T extends Event> void unsubscribe(Class<T> eventClass, EventListener<T> listener) {
        List<EventListener<? extends Event>> list = listeners.get(eventClass);
        if (list != null) list.remove(listener);
    }

    // ── Post ──────────────────────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    public <T extends Event> T post(T event) {
        List<EventListener<? extends Event>> list = listeners.get(event.getClass());
        if (list == null || list.isEmpty()) return event;
        for (EventListener<? extends Event> listener : list) {
            ((EventListener<T>) listener).accept(event);
            if (event.isCancellable() && event.isCancelled()) break;
        }
        return event;
    }

    // ── Fabric bridge ─────────────────────────────────────────────────────

    /**
     * Register against Fabric. Must be called after all subscribers are ready.
     * HudRenderCallback is deprecated in newer Fabric API but is the correct
     * choice for Fabric API versions bundled with 1.21.1.
     */
    @SuppressWarnings("deprecation")
    public void init() {
        ClientTickEvents.END_CLIENT_TICK.register(client ->
                post(new TickEvent(client)));

        HudRenderCallback.EVENT.register(
                (GuiGraphics guiGraphics, DeltaTracker deltaTracker) ->
                        post(new HudRenderEvent(guiGraphics,
                                deltaTracker.getGameTimeDeltaTicks())));

// 1. Intercept Player Chat (Standard chat)
        net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents.ALLOW_CHAT.register(
                (message, signedMessage, sender, params, receptionTimestamp) -> {
                    ChatReceiveEvent event = post(new ChatReceiveEvent(message, false));
                    return !event.isCancelled();
                });

// 2. Intercept Game/System Chat (EarthMC uses this for Global, Nation, etc.)
        net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents.ALLOW_GAME.register(
                (message, overlay) -> {
                    ChatReceiveEvent event = post(new ChatReceiveEvent(message, overlay));
                    return !event.isCancelled();
                });

        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            String address = client.getCurrentServer() != null
                    ? client.getCurrentServer().ip : "unknown";
            EarthlingClient.LOGGER.debug("Connected to server: {}", address);
            post(new ServerChangeEvent(client));
        });

        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) ->
                post(new ServerDisconnectEvent(client)));
    }

    // ── Inner type ────────────────────────────────────────────────────────

    public static class EventListener<T extends Event> {
        private final Consumer<T> delegate;
        public EventListener(Consumer<T> delegate) { this.delegate = delegate; }
        public void accept(T event) { delegate.accept(event); }
    }
}