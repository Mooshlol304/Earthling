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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Mixin(AvatarRenderer.class)
public abstract class PlayerInfoMixin {

    /**
     * Fix 1 — per-frame render-pass dedup.
     *
     * AvatarRenderState is allocated fresh per submitNameTag call, so identity
     * keying (WeakHashMap<AvatarRenderState>) is useless — every call looks new.
     * Instead we key on player name and gate with a nanoTime-based frame boundary.
     *
     * 2 ms threshold: multiple render passes for the same entity within a frame
     * are sequential and complete in microseconds. 2 ms safely separates them
     * from actual new frames even at 500fps, which no MC instance will reach.
     */
    private static final Set<String> ert$renderedThisFrame = new HashSet<>();
    private static long ert$lastFrameNanos = 0L;
    private static final long FRAME_BOUNDARY_NS = 2_000_000L; // 2 ms

    @Inject(method = "submitNameTag", at = @At("TAIL"))
    private void ert$injectNameDisplay(
            AvatarRenderState state,
            PoseStack poseStack,
            SubmitNodeCollector collector,
            CameraRenderState camera,
            CallbackInfo ci) {

        if (state.nameTag == null) return;

        // Roll the per-frame set over when a new frame starts.
        long now = System.nanoTime();
        if (now - ert$lastFrameNanos > FRAME_BOUNDARY_NS) {
            ert$renderedThisFrame.clear();
            ert$lastFrameNanos = now;
        }

        String playerName = state.nameTag.getString();

        // First render pass for this player this frame: proceed and claim the slot.
        // Any secondary pass (shadows, outlines, etc.) hits the guard and exits.
        if (!ert$renderedThisFrame.add(playerName)) return;

        RenderNameTagEvent event = new RenderNameTagEvent(playerName);
        EarthlingClient.getInstance().getEventBus().post(event);

        if (event.getExtraLines().isEmpty()) return;

        /**
         * Fix 2 — content-level line dedup.
         *
         * If the EventBus listener gets registered more than once (e.g. on server
         * switch or reinit without a prior unsubscribe), getExtraLines() will
         * contain duplicate Component instances. Deduplicate by string content
         * before drawing so a double-registration never causes double lines,
         * regardless of what happens upstream.
         */
        List<Component> lines = new ArrayList<>();
        Set<String> seenLines = new HashSet<>();
        for (Component line : event.getExtraLines()) {
            if (seenLines.add(line.getString())) {
                lines.add(line);
            }
        }

        // Draw extra lines above the vanilla name, stacking 0.25 units per line.
        poseStack.pushPose();
        poseStack.translate(0.0D, 0.25D, 0.0D);

        for (Component line : lines) {
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