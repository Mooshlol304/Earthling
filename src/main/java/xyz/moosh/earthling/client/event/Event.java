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

/**
 * Base class for all Earthling events.
 * Events that support cancellation should override {@link #isCancellable()} to return {@code true}.
 */
public abstract class Event {

    private boolean cancelled = false;

    /** @return {@code true} if this event can be cancelled by a listener. */
    public boolean isCancellable() {
        return false;
    }

    /**
     * Cancel this event.
     * Has no effect if {@link #isCancellable()} returns {@code false}.
     */
    public void cancel() {
        if (isCancellable()) this.cancelled = true;
    }

    public boolean isCancelled() {
        return cancelled;
    }
}
