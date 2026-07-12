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

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.*;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Style;

public final class ChatUtil {

    private static final String PREFIX = "§8[§6Earthling§8] §r";

    private ChatUtil() {}

    /** Send a prefixed message to the local chat HUD */
    public static void sendMessage(String message) {
        Minecraft mc = Minecraft.getInstance();

        if (mc.player != null) {
            mc.player.displayClientMessage(
                    Component.literal(PREFIX + message),
                    false
            );
        }
    }


    /** Send raw component */
    public static void sendMessage(Component message) {
        Minecraft mc = Minecraft.getInstance();

        if (mc.player != null) {
            mc.player.displayClientMessage(
                    message,
                    false
            );
        }
    }


    /**
     * Sends clickable text which opens a URL.
     */
    public static void sendClickable(
            String text,
            String url
    ) {

        Minecraft mc = Minecraft.getInstance();

        if (mc.player == null)
            return;


        Component component =
                Component.literal(PREFIX + text)
                        .setStyle(
                                Style.EMPTY
                                        .withColor(ChatFormatting.AQUA)
                                        .withUnderlined(true)
                                        .withClickEvent(
                                                new ClickEvent.OpenUrl(
                                                        java.net.URI.create(url)
                                                )
                                        )
                        );


        mc.player.displayClientMessage(
                component,
                false
        );
    }


    /** Send command to server */
    public static void sendCommand(String command) {

        Minecraft mc = Minecraft.getInstance();

        if (mc.player != null &&
                mc.getConnection() != null) {

            mc.getConnection()
                    .sendCommand(command);
        }
    }


    /** Strip formatting */
    public static String stripFormatting(String text) {
        return text.replaceAll(
                "§[0-9a-fk-or]",
                ""
        );
    }
}