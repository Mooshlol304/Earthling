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
import net.minecraft.client.gui.contextualbar.ContextualBarRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import xyz.moosh.earthling.client.EarthlingClient;
import xyz.moosh.earthling.client.module.ExpOverlay;
import xyz.moosh.earthling.client.util.ExperienceUtils;

@Mixin(ContextualBarRenderer.class)
public interface ExperienceTextMixin {

    @Redirect(
            method = "renderExperienceLevel",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/network/chat/Component;translatable(Ljava/lang/String;[Ljava/lang/Object;)Lnet/minecraft/network/chat/MutableComponent;"
            )
    )
    private static MutableComponent redirectLevelText(String key, Object[] args) {
        int level = (int) args[0];
        ExpOverlay mod = EarthlingClient.getInstance().getModuleManager().get(ExpOverlay.class);

        if (mod != null && mod.isEnabled() && Minecraft.getInstance().player != null) {
            // Show XP points earned toward the current level, e.g. "20 (+200)"
            int progressXp = ExperienceUtils.getXpProgressPoints(Minecraft.getInstance().player);
            return Component.literal(level + " (+" + progressXp + ")");
        }

        return Component.translatable(key, args);
    }
}