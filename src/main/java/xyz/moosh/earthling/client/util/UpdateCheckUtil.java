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

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.Minecraft;
import xyz.moosh.earthling.client.EarthlingClient;
import xyz.moosh.earthling.client.event.impl.ServerChangeEvent;
import xyz.moosh.earthling.client.service.EarthMCService;
import xyz.moosh.earthling.client.service.NotificationService;

import java.io.InputStreamReader;
import java.net.URL;
import java.util.concurrent.CompletableFuture;

public class UpdateCheckUtil {

    // The version variable you requested
    public static final String MOD_VERSION = "dev-1";

    // The URL variable you can change
    public static String updateUrl = "https://github.com/Mooshlol304/Earthling/releases/latest";

    // The raw JSON link
    private static final String UPDATE_JSON_URL = "https://raw.githubusercontent.com/Mooshlol304/Earthling/1.21.11/Update.json";

    /**
     * Listen for server changes and check for updates if on EarthMC.
     */
    public static void init() {
        EarthlingClient.getInstance().getEventBus().subscribe(ServerChangeEvent.class, event -> {
            // Give the ServiceManager a tick to update the connection state
            Minecraft.getInstance().execute(() -> {
                EarthMCService emc = EarthlingClient.getInstance().getServiceManager().get(EarthMCService.class);

                if (emc != null && emc.isEarthMC()) {
                    runUpdateCheck();
                }
            });
        });
    }

    private static void runUpdateCheck() {
        CompletableFuture.runAsync(() -> {
            try {
                URL url = new URL(UPDATE_JSON_URL);
                InputStreamReader reader = new InputStreamReader(url.openStream());
                JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();

                String latestVersion = json.get("version").getAsString();

                // If the JSON contains a specific download URL, we update our variable
                if (json.has("url")) {
                    updateUrl = json.get("url").getAsString();
                }

                if (!MOD_VERSION.equalsIgnoreCase(latestVersion)) {
                    triggerNotification(latestVersion);
                }
            } catch (Exception e) {
                EarthlingClient.LOGGER.error("[Earthling] Failed to fetch update metadata: " + e.getMessage());
            }
        });
    }

    private static void triggerNotification(String latest) {
        Minecraft.getInstance().execute(() -> {
            NotificationService notifier = EarthlingClient.getInstance().getServiceManager().get(NotificationService.class);

            if (notifier != null) {
                notifier.warn("A new version of Earthling is available (" + latest + ")");
                notifier.playPing();
            }

            ChatUtil.sendClickable(
                    "[Earthling] A new version of Earthling is available at " + updateUrl,
                    updateUrl
            );
        });
    }
}