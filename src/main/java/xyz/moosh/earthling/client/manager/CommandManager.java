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

package xyz.moosh.earthling.client.manager;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.network.chat.Component;
import xyz.moosh.earthling.client.EarthlingClient;
import xyz.moosh.earthling.client.api.ICommand;
import xyz.moosh.earthling.client.command.impl.*;
import xyz.moosh.earthling.client.module.Translation;

import java.util.*;

public class CommandManager {

    private static final String ROOT  = "ert";
    private static final String ALIAS = "earthling";

    private final List<ICommand> commands = new ArrayList<>();

    public void init() {
        registerAll();

        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            // Register the main command trees
            dispatcher.register(buildTree(ROOT));
            dispatcher.register(buildTree(ALIAS));

            // REGISTER HIDDEN COMMAND HERE
            dispatcher.register(ClientCommandManager.literal("ert_internal_translate")
                    .then(ClientCommandManager.argument("text", StringArgumentType.greedyString())
                            .executes(ctx -> {
                                String text = StringArgumentType.getString(ctx, "text");
                                Translation mod = EarthlingClient.getInstance().getModuleManager().get(Translation.class);
                                if (mod != null) {
                                    mod.translateAndSend(text);
                                }
                                return 1;
                            })));
        });
    }

    private void registerAll() {
        register(new NearbyCommand());
        register(new PlayerCommand());
        register(new DiscordCommand());
        register(new LastSeenCommand());
        register(new GotoCommand());
        register(new LocateCommand());
        register(new CalculateCommand());
        register(new TownlessCommand());
        register(new GoldCommand());
        register(new WildernessCommand());
        register(new StaffOnlineCommand());
        register(new MiningResetCommand());
    }

    private void register(ICommand cmd) { commands.add(cmd); }

    private LiteralArgumentBuilder<FabricClientCommandSource> buildTree(String root) {
        LiteralArgumentBuilder<FabricClientCommandSource> node = ClientCommandManager.literal(root);
        node.then(ClientCommandManager.literal("help").executes(ctx -> { sendHelp(ctx.getSource()); return 1; }));
        commands.forEach(cmd -> node.then(cmd.build()));
        node.executes(ctx -> { sendHelp(ctx.getSource()); return 1; });
        return node;
    }

    private void sendHelp(FabricClientCommandSource src) {
        src.sendFeedback(Component.literal("§8[§6Earthling§8] §6Commands §8(alias: §6/earthling§8):"));
        src.sendFeedback(Component.literal("  §e/ert nearby §7[radius]"));
        src.sendFeedback(Component.literal("  §e/ert player §7<name>"));
        src.sendFeedback(Component.literal("  §e/ert discord §7<name>"));
        src.sendFeedback(Component.literal("  §e/ert lastseen §7<name>"));
        src.sendFeedback(Component.literal("  §e/ert goto §7<town>"));
        src.sendFeedback(Component.literal("  §e/ert locate §7<town>"));
        src.sendFeedback(Component.literal("  §e/ert calculate §7<x> <z>"));
        src.sendFeedback(Component.literal("  §e/ert townless"));
        src.sendFeedback(Component.literal("  §e/ert gold §7toIngots|fromIngots"));
        src.sendFeedback(Component.literal("  §e/ert wilderness"));
        src.sendFeedback(Component.literal("  §e/ert staffonline"));
        src.sendFeedback(Component.literal("  §e/ert miningreset"));
    }

    public List<ICommand> getCommands() { return Collections.unmodifiableList(commands); }
}