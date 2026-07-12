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

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.network.chat.Component;
import xyz.moosh.earthling.client.EarthlingClient;
import xyz.moosh.earthling.client.api.ICommand;
import xyz.moosh.earthling.client.widget.impl.MiningWidget;

public class MiningResetCommand implements ICommand {

    @Override
    public String getName() { return "miningreset"; }

    @SuppressWarnings("SpellCheckingInspection")
    @Override
    public LiteralArgumentBuilder<FabricClientCommandSource> build() {
        return ClientCommandManager.literal("miningreset")
                .executes(ctx -> {
                    MiningWidget widget = EarthlingClient.getInstance()
                            .getWidgetManager()
                            .get(MiningWidget.class);

                    if (widget == null || !widget.isVisible()) {
                        ctx.getSource().sendFeedback(Component.literal(
                                "§8[§6Earthling§8] §cMining widget is not enabled."));
                        return 0;
                    }

                    if (widget.isSessionEmpty()) {
                        ctx.getSource().sendFeedback(Component.literal(
                                "§8[§6Earthling§8] §cNo active mining session to reset."));
                        return 0;
                    }

                    widget.resetSession();
                    ctx.getSource().sendFeedback(Component.literal(
                            "§8[§6Earthling§8] §aMining session cleared."));
                    return 1;
                });
    }
}