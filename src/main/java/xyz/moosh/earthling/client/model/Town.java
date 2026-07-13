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

import java.util.List;

/** Represents an EarthMC town. Pure data. */
public class Town {
    public String       name;
    public String       mayor;
    public String       nation;
    public Location     spawn;
    public double       gold;
    public int          numResidents;
    public int          numChunks;
    public boolean      isOpen;
    public boolean      canOutsidersSpawn;
    public boolean      isPeaceful;
    public boolean      isPublic;
    public boolean      isCapital;
    public List<String> residents;
    public long         fetchedAt;

    public Town() {}
}