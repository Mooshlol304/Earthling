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
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xyz.moosh.earthling.client.EarthlingClient;
import xyz.moosh.earthling.client.event.impl.BlockBreakEvent;

/**
 * Intercepts {@link MultiPlayerGameMode#destroyBlock} to fire {@link BlockBreakEvent}.
 *
 * Two injections are used:
 *  - HEAD: capture the block state before it becomes air
 *  - RETURN: only post the event if the server confirmed the break (return == true)
 *
 * This prevents counting blocks the player had no permission to break (Towny protection,
 * locked chests, etc.) and blocks that snap back on creative-like interactions.
 */
@Mixin(MultiPlayerGameMode.class)
public class MixinMultiPlayerGameMode {

    /** Stash the state captured at HEAD so RETURN can use it. */
    @Unique
    private BlockState earthling$pendingState = null;

    /** Capture block state before the break attempt. */
    @Inject(method = "destroyBlock", at = @At("HEAD"))
    private void earthling$captureState(BlockPos pos,
                                        CallbackInfoReturnable<Boolean> cir) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            earthling$pendingState = null;
            return;
        }
        earthling$pendingState = mc.level.getBlockState(pos);
    }

    /**
     * Post the event only if the break actually succeeded.
     * {@code cir.getReturnValue() == false} means the server rejected the break
     * (Towny protection, no permission, etc.) — we skip it entirely.
     */
    @Inject(method = "destroyBlock", at = @At("RETURN"))
    private void earthling$onBreakResult(BlockPos pos,
                                         CallbackInfoReturnable<Boolean> cir) {
        BlockState state = earthling$pendingState;
        earthling$pendingState = null; // always clear

        if (!cir.getReturnValue() || state == null) return; // break failed or cancelled
        if (state.isAir()) return;                           // already air, nothing to track
        if (EarthlingClient.getInstance() == null) return;

        EarthlingClient.getInstance().getEventBus().post(new BlockBreakEvent(pos, state));
    }
}