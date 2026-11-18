/*
 * Copyright (c) 2026.
 * Authors/Developers are listed in the CONTRIBUTING.md
 *
 * The Vanilla Extensions Mod is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * The Vanilla Extensions Mod is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program. If not, see <https://www.gnu.org/licenses/>
 */

package com.anedhel.vext.block.custom;

import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.ShapeContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.sound.SoundCategory;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.IntProperty;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.WorldView;
import net.minecraft.world.tick.ScheduledTickView;
import org.jspecify.annotations.Nullable;

/**
 * Represents small loose stones lying on the ground, one block per stone type.
 * <p>
 * A block holds 1 to {@link #MAX_PEBBLES} pebbles, stored in the {@link #PEBBLES} property.
 * Using a pebble item on an existing pebbles block adds one more pebble (like sea pickles).
 * Each pebble adds one more stone to the model, the stones already lying there stay in place.
 * <p>
 * The block breaks instantly with any tool or by hand, but every click only removes one pebble,
 * see {@link #registerBreakHandler()}. It has no collision and gets a small random offset like flowers.
 *
 * @author AnoJedt
 * @since after 0.1.0
 */
public class PebblesBlock extends Block {
	public static final int MAX_PEBBLES = 6;
	public static final IntProperty PEBBLES = IntProperty.of("pebbles", 1, MAX_PEBBLES);

	private static final VoxelShape SHAPE = Block.createCuboidShape(1.0, 0.0, 1.0, 15.0, 3.0, 15.0);

	public PebblesBlock(Settings settings) {
		super(settings);
		setDefaultState(getStateManager().getDefaultState().with(PEBBLES, 1));
	}

	/**
	 * Adds the {@link #PEBBLES} property to the block's state manager.
	 *
	 * @param builder the state manager builder used to define the block's state properties
	 */
	@Override
	protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
		builder.add(PEBBLES);
	}

	/**
	 * Adds one pebble when placed into an existing pebbles block, otherwise places a single pebble.
	 *
	 * @param ctx the placement context
	 * @return the block state to place
	 */
	@Override
	public @Nullable BlockState getPlacementState(ItemPlacementContext ctx) {
		BlockState existing = ctx.getWorld().getBlockState(ctx.getBlockPos());
		if (existing.isOf(this)) {
			return existing.with(PEBBLES, Math.min(MAX_PEBBLES, existing.get(PEBBLES) + 1));
		}
		return getDefaultState();
	}

	/**
	 * Allows placing another pebble into this block while holding the same pebbles item
	 * and the block is not full yet. Sneaking places the pebble next to it instead.
	 */
	@Override
	protected boolean canReplace(BlockState state, ItemPlacementContext context) {
		if (!context.shouldCancelInteraction() && context.getStack().isOf(this.asItem())
				&& state.get(PEBBLES) < MAX_PEBBLES) {
			return true;
		}
		return super.canReplace(state, context);
	}

	/**
	 * Pebbles need a full solid top face below them.
	 */
	@Override
	protected boolean canPlaceAt(BlockState state, WorldView world, BlockPos pos) {
		BlockPos below = pos.down();
		return world.getBlockState(below).isSideSolidFullSquare(world, below, Direction.UP);
	}

	/**
	 * Breaks the block (with its loot) when the block below is removed.
	 */
	@Override
	protected BlockState getStateForNeighborUpdate(BlockState state, WorldView world, ScheduledTickView tickView,
			BlockPos pos, Direction direction, BlockPos neighborPos, BlockState neighborState, Random random) {
		if (direction == Direction.DOWN && !state.canPlaceAt(world, pos)) {
			return Blocks.AIR.getDefaultState();
		}
		return state;
	}

	/**
	 * Returns a low outline shape, moved by the same random offset as the model.
	 */
	@Override
	protected VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
		return SHAPE.offset(state.getModelOffset(pos));
	}

	/**
	 * Limits the random offset to 2 pixels, so the stones do not stick out of the block too much.
	 * Flowers use 0.25 (4 pixels).
	 */
	@Override
	protected float getMaxHorizontalModelOffset() {
		return 0.125f;
	}

	/**
	 * Registers the handler that removes only one pebble per click.
	 * <p>
	 * When a player breaks a pebbles block with more than one pebble, the break is cancelled and the
	 * pebble count is reduced by one instead. One pebble is dropped (not in creative mode).
	 * The last pebble breaks the block normally and uses the loot table.
	 * This method should be called once during mod initialization.
	 */
	public static void registerBreakHandler() {
		PlayerBlockBreakEvents.BEFORE.register((world, player, pos, state, blockEntity) -> {
			if (!(state.getBlock() instanceof PebblesBlock) || state.get(PEBBLES) <= 1) {
				return true;
			}

			world.setBlockState(pos, state.with(PEBBLES, state.get(PEBBLES) - 1), Block.NOTIFY_ALL);
			if (!player.isCreative()) {
				Block.dropStack(world, pos, new ItemStack(state.getBlock()));
			}
			// the cancelled break skips spawnBreakParticles, so play the break sound for the other players here
			playBreakSound(world, player, pos, state);
			return false;
		});
	}

	/**
	 * Replaces the vanilla break particles with only the break sound.
	 * <p>
	 * Called on the client (for the breaking player) and on the server (for all other players)
	 * when a player breaks the block.
	 */
	@Override
	protected void spawnBreakParticles(World world, PlayerEntity player, BlockPos pos, BlockState state) {
		playBreakSound(world, player, pos, state);
	}

	/**
	 * Plays the break sound of the block with the vanilla volume and pitch.
	 * On the client it is played for the given player, on the server for everyone except that player,
	 * so every player hears it once.
	 */
	private static void playBreakSound(World world, PlayerEntity player, BlockPos pos, BlockState state) {
		BlockSoundGroup sounds = state.getSoundGroup();
		world.playSound(player, pos, sounds.getBreakSound(), SoundCategory.BLOCKS,
				(sounds.getVolume() + 1.0f) / 2.0f, sounds.getPitch() * 0.8f);
	}
}
