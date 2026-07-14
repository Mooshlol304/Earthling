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
import xyz.moosh.earthling.client.event.impl.*;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public class EventBus {

    private final Map<Class<? extends Event>, List<EventListener<? extends Event>>> listeners = new ConcurrentHashMap<>();

    public EventBus() {
        System.out.println("[Earthling] EventBus Initialized.SNAPSHOT enabled.");
    }

    public <T extends Event> EventListener<T> subscribe(Class<T> eventClass, Consumer<T> listener) {
        EventListener<T> wrapped = new EventListener<>(listener);
        listeners.computeIfAbsent(eventClass, k -> new CopyOnWriteArrayList<>()).add(wrapped);
        return wrapped;
    }

    public <T extends Event> void unsubscribe(Class<T> eventClass, EventListener<T> listener) {
        List<EventListener<? extends Event>> list = listeners.get(eventClass);
        if (list != null) list.remove(listener);
    }

    @SuppressWarnings("unchecked")
    public <T extends Event> T post(T event) {
        String eventName = event.getClass().getSimpleName();

        for (Map.Entry<Class<? extends Event>, List<EventListener<? extends Event>>> entry : listeners.entrySet()) {
            if (entry.getKey().getSimpleName().equals(eventName)) {
                for (EventListener<? extends Event> listener : entry.getValue()) {
                    try {
                        ((EventListener<T>) listener).accept(event);
                        if (event.isCancellable() && event.isCancelled()) return event;
                    } catch (Exception ignored) {}
                }
            }
        }
        return event;
    }

    @SuppressWarnings("deprecation")
    public void init() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> post(new TickEvent(client)));

        // Fabric bridge registration with modern parameter names for 1.21
        HudRenderCallback.EVENT.register((guiGraphics, deltaTracker) -> {
            post(new HudRenderEvent(guiGraphics, deltaTracker.getGameTimeDeltaTicks()));
        });

        ClientReceiveMessageEvents.ALLOW_CHAT.register((message, signed, sender, params, ts) -> {
            return !post(new ChatReceiveEvent(message, false)).isCancelled();
        });

        ClientReceiveMessageEvents.ALLOW_GAME.register((message, overlay) -> {
            return !post(new ChatReceiveEvent(message, overlay)).isCancelled();
        });

        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> post(new ServerChangeEvent(client)));
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> post(new ServerDisconnectEvent(client)));
    }

    public static class EventListener<T extends Event> {
        private final Consumer<T> delegate;
        public EventListener(Consumer<T> delegate) { this.delegate = delegate; }
        public void accept(T event) { delegate.accept(event); }
    }
}