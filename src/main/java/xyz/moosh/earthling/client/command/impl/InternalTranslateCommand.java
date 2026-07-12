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

import com.mojang.brigadier.CommandDispatcher;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import xyz.moosh.earthling.client.EarthlingClient;
import xyz.moosh.earthling.client.module.Translation;

import static com.mojang.brigadier.arguments.StringArgumentType.greedyString;

public class InternalTranslateCommand {
    public static void register(CommandDispatcher<FabricClientCommandSource> dispatcher) {
        dispatcher.register(ClientCommandManager.literal("ert_internal_translate")
                .then(ClientCommandManager.argument("text", greedyString())
                        .executes(context -> {
                            String text = context.getArgument("text", String.class);
                            Translation module = EarthlingClient.getInstance().getModuleManager().get(Translation.class);
                            if (module != null) {
                                module.translateAndSend(text);
                            }
                            return 1;
                        })));
    }
}