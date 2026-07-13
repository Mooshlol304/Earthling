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

package xyz.moosh.earthling.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.ChatScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xyz.moosh.earthling.client.EarthlingClient;
import xyz.moosh.earthling.client.module.ChatPreview;
import xyz.moosh.earthling.client.model.ChatChannel;
import xyz.moosh.earthling.client.service.ChatTrackerService;

@Mixin(EditBox.class)
public abstract class EditBoxMixin {

    @Unique
    private static ChatChannel earthling$lastChannel = ChatChannel.GLOBAL;

    @Inject(method = "renderWidget", at = @At("TAIL"))
    private void earthling$chatPreview(
            GuiGraphics graphics,
            int mouseX,
            int mouseY,
            float delta,
            CallbackInfo ci
    ) {
        Minecraft mc = Minecraft.getInstance();

        // Only render inside ChatScreen — we don't want this bleeding into config screens.
        if (!(mc.screen instanceof ChatScreen)) return;

        ChatPreview module = EarthlingClient.getInstance().getModuleManager().get(ChatPreview.class);
        if (module == null || !module.isEnabled()) return;

        EditBox box = (EditBox) (Object) this;

        ChatTrackerService tracker = EarthlingClient.getInstance()
                .getServiceManager()
                .getChatTracker();

        if (tracker != null) {
            ChatChannel current = tracker.getChannel();
            if (current != null) earthling$lastChannel = current;
        }

        ChatChannel channel = earthling$lastChannel != null ? earthling$lastChannel : ChatChannel.GLOBAL;

        String name = channel.getName();
        if (name == null || name.isEmpty()) name = "global";
        name = name.substring(0, 1).toUpperCase() + name.substring(1).toLowerCase();

        if (tracker != null && tracker.isParty()) name += " (Party)";

        int colour = channel.getColour();
        if (colour == 0) colour = 0xFFFFFF;

        graphics.drawString(mc.font, "Sending to: [" + name + "]", box.getX(), box.getY() - 12, colour, true);
    }
}