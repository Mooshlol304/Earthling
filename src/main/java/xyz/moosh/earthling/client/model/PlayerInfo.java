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

package xyz.moosh.earthling.client.model;

/**
 * Aggregated view of a player combining Resident data with optional extras
 * like Discord ID. Built by PlayerService from API responses.
 */
public class PlayerInfo {
    public String   name;
    public String   uuid;
    public String   town;
    public String   nation;
    public String   discordId;   // may be null
    public double   balance;
    public boolean  isOnline;
    public long     lastOnline;
    public long     registeredAt; // Added field
    public Location mapLocation;
    public long     fetchedAt;

    public PlayerInfo() {}
}