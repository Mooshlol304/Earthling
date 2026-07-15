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
import xyz.moosh.earthling.client.event.EventBus;
import xyz.moosh.earthling.client.event.impl.RenderNameTagEvent;

import java.util.HashSet;
import java.util.Set;

public class PlayerAffiliations extends Module {

    // Prevents spamming getPlayer() for a name already in flight
    private final Set<String> pendingRequests = new HashSet<>();

    // subscribe() wraps our Consumer in an EventListener and returns it;
    // we must hold onto that wrapper since unsubscribe() expects an EventListener
    private EventBus.EventListener<RenderNameTagEvent> listener;

    public PlayerAffiliations() {
        super("player_affiliations", "Player Affiliations", Category.HUD);
    }

    @Override
    public void onEnable() {
        pendingRequests.clear();
        listener = eventBus.subscribe(RenderNameTagEvent.class, this::renderAffiliation);
    }

    @Override
    public void onDisable() {
        if (listener != null) {
            eventBus.unsubscribe(RenderNameTagEvent.class, listener);
            listener = null;
        }
        pendingRequests.clear();
    }

    private void renderAffiliation(RenderNameTagEvent event) {
        var service = EarthlingClient.getInstance().getServiceManager().getPlayerService();
        String cleanName = event.getPlayerName().replaceAll("§.", "");

        var info = service.getCachedPlayer(cleanName);

        if (info == null) {
            if (!pendingRequests.contains(cleanName)) {
                pendingRequests.add(cleanName);
                service.getPlayer(cleanName).thenAccept(data -> pendingRequests.remove(cleanName));
            }
            return;
        }

        if (info.town != null && !info.town.isEmpty()) {
            String result;
            if (info.nation != null && !info.nation.isEmpty()) {
                result = "§7[§6" + info.nation + "§7|§3" + info.town + "§7]";
            } else {
                result = "§7[§3" + info.town + "§7]";
            }
            event.addAboveName(Component.literal(result));
        }
    }
}