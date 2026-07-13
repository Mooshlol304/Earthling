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

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import xyz.moosh.earthling.client.EarthlingClient;
import xyz.moosh.earthling.client.api.ICommand;
import xyz.moosh.earthling.client.model.Resident;
import xyz.moosh.earthling.client.network.EarthMCApi;
import xyz.moosh.earthling.client.util.ChatUtil;
import xyz.moosh.earthling.client.screen.EarthlingConfigScreen;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class TownlessCommand implements ICommand {
    private static final int BATCH_SIZE = 100;

    @Override public String getName() { return "townless"; }

    @Override
    public LiteralArgumentBuilder<FabricClientCommandSource> build() {
        return ClientCommandManager.literal("townless")
                .executes(ctx -> { run(); return 1; })
                .then(ClientCommandManager.literal("open_config")
                        .executes(ctx -> {
                            Minecraft mc = Minecraft.getInstance();
                            mc.execute(() -> mc.setScreen(new EarthlingConfigScreen(mc.screen)));
                            return 1;
                        }))
                .then(ClientCommandManager.literal("invite_internal")
                        .then(ClientCommandManager.argument("name", StringArgumentType.string())
                                .then(ClientCommandManager.argument("town", StringArgumentType.string())
                                        .executes(ctx -> {
                                            String target = StringArgumentType.getString(ctx, "name");
                                            String town = StringArgumentType.getString(ctx, "town");
                                            performInvite(target, town);
                                            return 1;
                                        }))));
    }

    private void run() {
        EarthMCApi api = EarthlingClient.getInstance().getServiceManager().getEarthMCApi();
        ChatUtil.sendMessage("§7Fetching townless players...");

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        String myName = mc.player.getName().getString();

        api.getOnlinePlayers().thenCompose(allNames -> {
            List<CompletableFuture<List<Resident>>> batches = new ArrayList<>();
            for (int i = 0; i < allNames.size(); i += BATCH_SIZE) {
                List<String> batch = allNames.subList(i, Math.min(i + BATCH_SIZE, allNames.size()));
                batches.add(api.getPlayersBatch(new ArrayList<>(batch)));
            }
            return CompletableFuture.allOf(batches.toArray(new CompletableFuture[0]))
                    .thenApply(v -> batches.stream()
                            .flatMap(f -> f.getNow(new ArrayList<>()).stream())
                            .toList());
        }).thenAccept(residents -> mc.execute(() -> {
            List<String> names = residents.stream()
                    .filter(r -> r.town == null || r.town.isBlank())
                    .map(r -> r.name).sorted(String.CASE_INSENSITIVE_ORDER).toList();

            if (names.isEmpty()) {
                ChatUtil.sendMessage("§7No townless players found.");
                return;
            }

            String myTown = residents.stream().filter(r -> r.name.equalsIgnoreCase(myName))
                    .map(r -> r.town).findFirst().orElse("NoTown");

            ChatUtil.sendMessage("§bTownless §8(" + names.size() + "/" + residents.size() + "):");

            MutableComponent list = Component.empty();
            for (int i = 0; i < names.size(); i++) {
                list.append(createPlayerComponent(names.get(i), myTown));
                if (i < names.size() - 1) list.append(Component.literal("§7, "));
            }

            // HUD Message display for 1.21 Mojmap
            mc.gui.getChat().addMessage(list);

            String current = EarthlingClient.getInstance().getTownlessMessage().get();
            String defMsg = EarthlingClient.getInstance().getTownlessMessage().getDefaultValue();

            if (current.equals(defMsg)) {
                // Fixed: New Record syntax for 1.21 ClickEvent
                MutableComponent hint = Component.literal("§8Change The Townless Message at the Config!")
                        .withStyle(style -> style.withClickEvent(new ClickEvent.RunCommand("/ert townless open_config")));
                mc.gui.getChat().addMessage(hint);
            }
        }));
    }

    private MutableComponent createPlayerComponent(String name, String town) {
        // Fix for 1.21 ClickEvent record
        ClickEvent click = new ClickEvent.RunCommand("/ert townless invite_internal " + name + " " + town);

        // Fix for 1.21 HoverEvent record (Based on your provided decompile)
        HoverEvent hover = new HoverEvent.ShowText(Component.literal("§aInvite " + name + " to town"));

        return Component.literal("§f" + name).withStyle(style -> style
                .withClickEvent(click)
                .withHoverEvent(hover));
    }

    private void performInvite(String target, String town) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        mc.player.connection.sendCommand("t add " + target);

        new Thread(() -> {
            try {
                Thread.sleep(500);
                mc.execute(() -> {
                    if (mc.player != null) {
                        String raw = EarthlingClient.getInstance().getTownlessMessage().get();
                        String formatted = raw.replace("@username", target).replace("@town", town);
                        mc.player.connection.sendCommand("msg " + target + " " + formatted);
                    }
                });
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                EarthlingClient.LOGGER.warn("[TownlessCommand] Invite thread interrupted for {}", target);
            }
        }).start();
    }
}