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

package xyz.moosh.earthling.client.module;

import net.minecraft.network.chat.Component;
import xyz.moosh.earthling.client.EarthlingClient;
import xyz.moosh.earthling.client.event.impl.RenderNameTagEvent;
import xyz.moosh.earthling.client.service.PlayerService;

import java.util.HashSet;
import java.util.Set;

public class PlayerAffiliations extends Module {

    // Keep track of who we are currently fetching to prevent API spam
    private final Set<String> pendingRequests = new HashSet<>();

    public PlayerAffiliations() {
        super("player_affiliations", "Player Affiliations", Category.HUD);
    }

    @Override
    public void onEnable() {
        // Correctly subscribing to the event
        eventBus.subscribe(RenderNameTagEvent.class, this::renderAffiliation);
    }

    @Override
    public void onDisable() {
        // Cleanup if necessary
        pendingRequests.clear();
    }

    private void renderAffiliation(RenderNameTagEvent event) {
        var service = EarthlingClient.getInstance().getServiceManager().getPlayerService();
        String cleanName = event.getPlayerName().replaceAll("§.", "");

        // 1. Check the cache
        var info = service.getCachedPlayer(cleanName);

        // 2. Logic: If null, fetch it once and then stop
        if (info == null) {
            if (!pendingRequests.contains(cleanName)) {
                pendingRequests.add(cleanName);

                // Trigger async fetch
                service.getPlayer(cleanName).thenAccept(data -> {
                    pendingRequests.remove(cleanName);
                });
            }
            // Return early: we have nothing to render yet
            return;
        }

// 3. Render logic (updated with §3 for the darker town color)
        if (info.town != null && !info.town.isEmpty()) {
            String result;
            if (info.nation != null && !info.nation.isEmpty()) {
                // §6 = Gold, §7 = Gray, §3 = Dark Aqua
                result = "§7[§6" + info.nation + "§7|§3" + info.town + "§7]";
            } else {
                // §3 = Dark Aqua
                result = "§7[§3" + info.town + "§7]";
            }
            event.addAboveName(Component.literal(result));
        }
    }
}