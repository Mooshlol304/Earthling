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

import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility for managing and verifying the MooshLib dependency.
 */
public final class MooshLibUtil {

    private static final Logger LOGGER = LoggerFactory.getLogger("MooshLibUtil");
    private static final String MOOSH_LIB_ID = "moosh-lib";

    private MooshLibUtil() {}

    /**
     * Checks if MooshLib is currently loaded in the environment.
     * @return true if the mod is present.
     */
    public static boolean isMooshLibLoaded() {
        return FabricLoader.getInstance().isModLoaded(MOOSH_LIB_ID);
    }

    /**
     * Ensures the mod environment is valid. 
     * Can be called during EarthlingClient initialization.
     */
    public static void validateDependency() {
        if (!isMooshLibLoaded()) {
            LOGGER.error("Critical Failure: moosh-lib is missing!");
            // Standard practice is to let Fabric Loader handle this via fabric.mod.json,
            // but this provides an additional runtime safety check.
            throw new RuntimeException("Earthling requires MooshLib to function correctly.");
        }
        LOGGER.info("MooshLib dependency verified.");
    }
}