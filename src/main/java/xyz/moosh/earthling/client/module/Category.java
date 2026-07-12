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

package xyz.moosh.earthling.client.module;

/** Logical grouping for modules in the configuration screen. */
public enum Category {
    COMBAT    ("Combat"),
    HUD       ("HUD"),
    CHAT      ("Chat"),
    MAP       ("Map"),
    MINING    ("Mining"),
    ECONOMY   ("Economy"),
    UTILITY   ("Utility"),
    EARTHMC   ("EarthMC");

    private final String displayName;

    Category(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() { return displayName; }
}
