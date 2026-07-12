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

import net.minecraft.network.chat.Component;
import xyz.moosh.earthling.client.event.Event;

/** Fired when the client receives a game message. Cancel to suppress display. */
public class ChatReceiveEvent extends Event {
    private final Component message;
    private final boolean   overlay;

    public ChatReceiveEvent(Component message, boolean overlay) {
        this.message = message;
        this.overlay = overlay;
    }

    @Override public boolean isCancellable() { return true; }

    public Component getMessage() { return message; }
    /** @return {@code true} if the message targets the action bar. */
    public boolean isOverlay() { return overlay; }
}