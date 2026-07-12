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

package xyz.moosh.earthling.client.command.impl;

import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.Minecraft;

import java.util.ArrayList;
import java.util.List;

/**
 * Shared suggestion providers for client commands.
 *
 * Uses getConnection().getOnlinePlayers() so ALL tab-list players are suggested,
 * not just those in render range (which is usually nobody on EarthMC).
 *
 * Note: uses explicit lambda info -> info.getProfile().name() rather than a
 * method reference because type inference fails on GameProfile::getName in some
 * MC versions. If getName() still doesn't compile in your version of authlib,
 * change it to info.getProfile().name() (record accessor).
 */
public final class Suggestions {

    private Suggestions() {}

    public static SuggestionProvider<FabricClientCommandSource> onlinePlayers() {
        return (ctx, builder) -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc.getConnection() == null) return builder.buildFuture();

            String          remaining = builder.getRemaining().toLowerCase();
            List<String>    names     = new ArrayList<>();

            for (var info : mc.getConnection().getOnlinePlayers()) {
                try {
                    String name = info.getProfile().name();
                    if (name != null && name.toLowerCase().startsWith(remaining)) {
                        names.add(name);
                    }
                } catch (Exception ignored) {}
            }

            names.sort(String::compareToIgnoreCase);
            names.forEach(builder::suggest);
            return builder.buildFuture();
        };
    }
}