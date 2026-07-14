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

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xyz.moosh.earthling.client.EarthlingClient;
import xyz.moosh.earthling.client.event.impl.RenderNameTagEvent;

import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;

@Mixin(AvatarRenderer.class)
public abstract class PlayerInfoMixin {

    /**
     * Guards against duplicate extra-line injection when the rendering pipeline
     * calls submitNameTag more than once per entity per frame (e.g. shadow pass,
     * outline pass, main pass). Keys are held weakly so entries are automatically
     * evicted once the AvatarRenderState object is no longer referenced after the
     * frame, meaning the next frame always gets a fresh render.
     */
    private static final Set<AvatarRenderState> ert$renderedStates =
            Collections.newSetFromMap(new WeakHashMap<>());

    @Inject(method = "submitNameTag", at = @At("TAIL"))
    private void ert$injectNameDisplay(
            AvatarRenderState state,
            PoseStack poseStack,
            SubmitNodeCollector collector,
            CameraRenderState camera,
            CallbackInfo ci) {

        if (state.nameTag == null) return;

        // If we've already injected extra lines for this state object this frame,
        // bail out — we're in a secondary render pass (shadow, outline, etc.)
        if (!ert$renderedStates.add(state)) return;

        RenderNameTagEvent event = new RenderNameTagEvent(state.nameTag.getString());
        EarthlingClient.getInstance().getEventBus().post(event);

        if (event.getExtraLines().isEmpty()) return;

        // Draw extra lines above the vanilla name, stacking 0.25 units per line.
        poseStack.pushPose();
        poseStack.translate(0.0D, 0.25D, 0.0D);

        for (Component line : event.getExtraLines()) {
            collector.submitNameTag(
                    poseStack,
                    state.nameTagAttachment,
                    0,
                    line,
                    !state.isDiscrete,
                    state.lightCoords,
                    state.distanceToCameraSq,
                    camera
            );
            poseStack.translate(0.0D, 0.25D, 0.0D);
        }

        poseStack.popPose();
    }
}