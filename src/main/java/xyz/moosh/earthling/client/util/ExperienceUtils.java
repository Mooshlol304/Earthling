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

package xyz.moosh.earthling.client.util;

import net.minecraft.world.entity.player.Player;

/**
 * Utility to calculate XP points from the level progress.
 */
public class ExperienceUtils {

    /**
     * Calculates the points earned towards the next level only.
     * e.g., if you are level 20 and 50% to 21, this returns the XP points for that 50%.
     */
    public static int getXpProgressPoints(Player player) {
        return (int) Math.round(getExpToNextLevel(player.experienceLevel) * player.experienceProgress);
    }

    /**
     * Total XP needed to complete the CURRENT level and reach the next one.
     */
    public static int getExpToNextLevel(int level) {
        if (level >= 30) return level * 9 - 158;
        if (level >= 15) return level * 5 - 38;
        return level * 2 + 7;
    }
}