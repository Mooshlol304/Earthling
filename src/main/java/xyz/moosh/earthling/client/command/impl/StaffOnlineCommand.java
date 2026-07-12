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
import net.minecraft.client.Minecraft;

import xyz.moosh.earthling.client.EarthlingClient;
import xyz.moosh.earthling.client.api.ICommand;
import xyz.moosh.earthling.client.model.Resident;
import xyz.moosh.earthling.client.network.EarthMCApi;
import xyz.moosh.earthling.client.util.ChatUtil;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;


public class StaffOnlineCommand implements ICommand {


    private static final List<String> ROLE_ORDER = List.of(
            "owner",
            "admin",
            "developer",
            "moderator",
            "helper"
    );


    private static final Map<String, String> ROLE_NAMES = Map.of(
            "owner", "Owner",
            "admin", "Admins",
            "developer", "Developers",
            "moderator", "Moderators",
            "helper", "Helpers"
    );


    private static final Map<String, String> ROLE_COLOURS = Map.of(
            "owner", "§c",
            "admin", "§4",
            "developer", "§5",
            "moderator", "§2",
            "helper", "§a"
    );


    @Override
    public String getName() {
        return "staffonline";
    }


    @Override
    public LiteralArgumentBuilder<FabricClientCommandSource> build() {

        return ClientCommandManager.literal("staffonline")

                .executes(ctx -> {
                    run();
                    return 1;
                });

    }


    private void run() {

        EarthMCApi api = EarthlingClient.getInstance()
                .getServiceManager()
                .getEarthMCApi();


        ChatUtil.sendMessage("§7Fetching online staff...");


        api.getStaff()

                .thenCompose(staff -> {


                    List<String> uuids = staff.values()
                            .stream()
                            .flatMap(Collection::stream)
                            .distinct()
                            .collect(Collectors.toList());


                    if (uuids.isEmpty()) {
                        return CompletableFuture.completedFuture(
                                new StaffResult(staff, new ArrayList<>())
                        );
                    }


                    return api.getPlayersBatch(uuids)

                            .thenApply(players ->
                                    new StaffResult(staff, players)
                            );

                })


                .thenAccept(result -> Minecraft.getInstance().execute(() -> {


                    Map<String, List<String>> onlineByRole =
                            new LinkedHashMap<>();


                    for (String role : ROLE_ORDER) {

                        List<String> onlineNames =
                                new ArrayList<>();


                        List<String> roleUUIDs =
                                result.roles.getOrDefault(
                                        role,
                                        Collections.emptyList()
                                );


                        for (Resident player : result.players) {

                            if (!player.isOnline)
                                continue;


                            if (player.uuid == null)
                                continue;


                            if (roleUUIDs.contains(player.uuid)) {

                                onlineNames.add(player.name);

                            }

                        }


                        if (!onlineNames.isEmpty()) {

                            onlineNames.sort(
                                    String.CASE_INSENSITIVE_ORDER
                            );

                            onlineByRole.put(
                                    role,
                                    onlineNames
                            );

                        }

                    }



                    int total = onlineByRole.values()
                            .stream()
                            .mapToInt(List::size)
                            .sum();



                    if (total == 0) {

                        ChatUtil.sendMessage(
                                "§6Online Staff §8(§70§8): §7None"
                        );

                        return;

                    }



                    ChatUtil.sendMessage(
                            "§6Online Staff §8(§f"
                                    + total
                                    + "§8)"
                    );


                    for (String role : ROLE_ORDER) {


                        List<String> players =
                                onlineByRole.get(role);


                        if (players == null)
                            continue;


                        String colour =
                                ROLE_COLOURS.getOrDefault(
                                        role,
                                        "§f"
                                );


                        String name =
                                ROLE_NAMES.getOrDefault(
                                        role,
                                        role
                                );


                        ChatUtil.sendMessage(

                                colour
                                        + name
                                        + " §8("
                                        + players.size()
                                        + "): §f"
                                        + String.join(
                                        "§7, §f",
                                        players
                                )

                        );

                    }


                }))


                .exceptionally(e -> {


                    Minecraft.getInstance().execute(() ->
                            ChatUtil.sendMessage(
                                    "§cFailed fetching staff: "
                                            + e.getMessage()
                            )
                    );


                    return null;

                });


    }



    private static class StaffResult {

        final Map<String, List<String>> roles;

        final List<Resident> players;


        StaffResult(
                Map<String, List<String>> roles,
                List<Resident> players
        ) {

            this.roles = roles;
            this.players = players;

        }

    }

}