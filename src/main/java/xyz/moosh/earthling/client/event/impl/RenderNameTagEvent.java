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

import java.util.ArrayList;
import java.util.List;

public class RenderNameTagEvent extends Event {
    private final String playerName;
    private final List<Component> extraLines = new ArrayList<>();

    public RenderNameTagEvent(String playerName) {
        this.playerName = playerName;
    }

    public String getPlayerName() {
        return playerName;
    }

    public void addAboveName(Component comp) {
        // Adds to the top so highest lines appear highest in game
        extraLines.add(0, comp);
    }

    public List<Component> getExtraLines() {
        return extraLines;
    }
}