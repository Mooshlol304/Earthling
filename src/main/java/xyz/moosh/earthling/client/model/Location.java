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

public class Location {
    public final double x;
    public final double y;
    public final double z;
    public final String world;

    public Location(double x, double y, double z, String world) {
        this.x = x; this.y = y; this.z = z; this.world = world;
    }

    public Location(double x, double y, double z) {
        this(x, y, z, "world");
    }

    public double distanceTo(Location other) {
        double dx = this.x - other.x, dz = this.z - other.z;
        return Math.sqrt(dx * dx + dz * dz);
    }

    @Override public String toString() {
        return String.format("(%.0f, %.0f, %.0f)", x, y, z);
    }
}
