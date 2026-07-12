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

import java.util.HashMap;
import java.util.Map;

public class ChatChannel {

    private final String name;
    private final int colour;

    public ChatChannel(String name, int colour) {
        this.name = name;
        this.colour = colour;
    }

    public String getName() {
        return name;
    }

    public int getColour() {
        return colour;
    }


    private static final Map<String, ChatChannel> CHANNELS = new HashMap<>();

    public static final ChatChannel GLOBAL =
            register("global", -0x555556);

    public static final ChatChannel TOWN =
            register("town", -0xaa0001);

    public static final ChatChannel NATION =
            register("nation", -0xab);

    public static final ChatChannel LOCAL =
            register("local", -0xa4158e);

    public static final ChatChannel STAFF =
            register("staff", -0x580000);

    public static final ChatChannel TRADE =
            register("trade", -0xaa0001);

    public static final ChatChannel PREMIUM =
            register("premium", -0x3ab04);


    // Languages
    public static final ChatChannel PORTUGUESE =
            register("portuguese", -0xab03ac);

    public static final ChatChannel TURKISH =
            register("turkish", -0xab03ac);

    public static final ChatChannel SWEDISH =
            register("swedish", -0xab03ac);

    public static final ChatChannel GERMAN =
            register("german", -0xab03ac);

    public static final ChatChannel UKRAINIAN =
            register("ukrainian", -0xab03ac);

    public static final ChatChannel CHINESE =
            register("chinese", -0xab03ac);

    public static final ChatChannel FRENCH =
            register("french", -0xab03ac);

    public static final ChatChannel POLISH =
            register("polish", -0xab03ac);

    public static final ChatChannel RUSSIAN =
            register("russian", -0xab03ac);

    public static final ChatChannel SPANISH =
            register("spanish", -0xab03ac);

    public static final ChatChannel DUTCH =
            register("dutch", -0xab03ac);

    public static final ChatChannel JAPANESE =
            register("japanese", -0xab03ac);


    private static ChatChannel register(String name, int colour) {
        ChatChannel channel = new ChatChannel(name, colour);
        CHANNELS.put(name, channel);
        return channel;
    }


    public static ChatChannel get(String name) {

        if(name == null || name.isEmpty()) {
            return GLOBAL;
        }


        ChatChannel channel = CHANNELS.get(
                name.toLowerCase()
        );


        if(channel != null) {
            return channel;
        }


        // Unknown channel fallback
        return new ChatChannel(
                name.toLowerCase(),
                0xFFFFFF
        );
    }
}