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
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import xyz.moosh.earthling.client.EarthlingClient;
import xyz.moosh.earthling.client.event.impl.ServerChangeEvent;
import xyz.moosh.earthling.client.service.NotificationService;

import java.io.InputStreamReader;
import java.net.URI;
import java.net.URL;
import java.util.concurrent.CompletableFuture;

public class UpdateCheckUtil {

    public static final String MOD_VERSION = "1.0";
    public static String updateUrl = "https://modrinth.com/mod/earthling#download";
    private static final String JSON_METADATA_URL = "https://raw.githubusercontent.com/Mooshlol304/Earthling/1.21.11/Update.json";

    public static void init() {
        EarthlingClient.getInstance().getEventBus().subscribe(ServerChangeEvent.class, event -> {
            Minecraft.getInstance().execute(UpdateCheckUtil::performUpdateCheck);
        });
    }

    private static void performUpdateCheck() {
        CompletableFuture.runAsync(() -> {
            try {
                URL url = URI.create(JSON_METADATA_URL).toURL();
                try (InputStreamReader reader = new InputStreamReader(url.openStream())) {
                    JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
                    String remoteVersion = json.get("version").getAsString();

                    if (json.has("url")) {
                        updateUrl = json.get("url").getAsString();
                    }

                    if (!MOD_VERSION.equalsIgnoreCase(remoteVersion)) {
                        sendCleanNotification(remoteVersion);
                    }
                }
            } catch (Exception e) {
                EarthlingClient.LOGGER.error("[Earthling] Update check failed: {}", e.getMessage());
            }
        });
    }

    private static void sendCleanNotification(String remoteVersion) {
        Minecraft.getInstance().execute(() -> {
            NotificationService notifier = EarthlingClient.getInstance().getServiceManager().get(NotificationService.class);
            if (notifier != null) notifier.playPing();

            MutableComponent message = Component.literal(
                            "§8[§6Earthling§8] §7A new version (§6" + remoteVersion + "§7) is available! ")
                    .append(Component.literal("§b§n[Download Here]")
                            .withStyle(style -> style
                                    .withClickEvent(new ClickEvent.OpenUrl(URI.create(updateUrl)))
                                    .withHoverEvent(new HoverEvent.ShowText(Component.literal("§7Click to open GitHub")))));

            ChatUtil.sendMessage(message);
        });
    }
}