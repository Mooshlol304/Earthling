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

package xyz.moosh.earthling.client.event.impl;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import xyz.moosh.earthling.client.event.Event;

/**
 * Fired (via mixin) just before the local player breaks a block.
 * Block state is captured before the break so the ore type is still readable.
 */
public class BlockBreakEvent extends Event {

    private final BlockPos   pos;
    private final BlockState state;

    public BlockBreakEvent(BlockPos pos, BlockState state) {
        this.pos   = pos;
        this.state = state;
    }

    public BlockPos   getPos()   { return pos; }
    public BlockState getState() { return state; }
}
