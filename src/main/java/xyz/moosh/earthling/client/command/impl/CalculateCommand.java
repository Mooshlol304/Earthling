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

import com.google.gson.JsonObject;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.Minecraft;
import xyz.moosh.earthling.client.EarthlingClient;
import xyz.moosh.earthling.client.api.ICommand;
import xyz.moosh.earthling.client.util.ChatUtil;

/** /er calculate <x> <z> — distance, direction, and Towny location info */
public class CalculateCommand implements ICommand {

    @Override public String getName() { return "calculate"; }

    @Override
    public LiteralArgumentBuilder<FabricClientCommandSource> build() {
        return ClientCommandManager.literal("calculate")
                .then(ClientCommandManager.argument("x", IntegerArgumentType.integer())
                        .then(ClientCommandManager.argument("z", IntegerArgumentType.integer())
                                .executes(ctx -> {
                                    run(IntegerArgumentType.getInteger(ctx, "x"),
                                            IntegerArgumentType.getInteger(ctx, "z"));
                                    return 1;
                                })));
    }

    private void run(int tx, int tz) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        int    px   = (int) mc.player.getX();
        int    pz   = (int) mc.player.getZ();
        double dist = Math.sqrt((double)(tx - px) * (tx - px) + (double)(tz - pz) * (tz - pz));
        String dir  = bearing(px, pz, tx, tz);

        ChatUtil.sendMessage("§b(" + tx + ", " + tz + ") §8• §e" + (int) dist + "m §7(" + dir + ")"
                + " §8• §7from §f(" + px + ", " + pz + ")");

        // Async: query Towny location
        EarthlingClient.getInstance().getServiceManager().getEarthMCApi()
                .getLocationInfo(tx, tz)
                .thenAccept(loc -> Minecraft.getInstance().execute(() -> {
                    if (loc == null) return;
                    boolean wild = loc.has("isWilderness")
                            && !loc.get("isWilderness").isJsonNull()
                            && loc.get("isWilderness").getAsBoolean();
                    if (wild) {
                        ChatUtil.sendMessage("§7Location: §8Wilderness");
                        return;
                    }
                    String town   = getNestedName(loc, "town");
                    String nation = getNestedName(loc, "nation");
                    String info   = "§7Town: §b" + (town != null ? town : "Unknown");
                    if (nation != null) info += " §8(§f" + nation + "§8)";
                    ChatUtil.sendMessage(info);
                }));
    }

    /** Safely get the "name" field from a nested object in a JsonObject. */
    private String getNestedName(JsonObject parent, String key) {
        if (!parent.has(key) || !parent.get(key).isJsonObject()) return null;
        JsonObject child = parent.getAsJsonObject(key);
        if (!child.has("name") || child.get("name").isJsonNull()) return null;
        return child.get("name").getAsString();
    }

    private String bearing(double fx, double fz, double tx, double tz) {
        double angle = Math.toDegrees(Math.atan2(tx - fx, -(tz - fz)));
        if (angle < 0) angle += 360;
        String[] dirs = {"N","NE","E","SE","S","SW","W","NW","N"};
        return dirs[(int) Math.round(angle / 45) % 8];
    }
}