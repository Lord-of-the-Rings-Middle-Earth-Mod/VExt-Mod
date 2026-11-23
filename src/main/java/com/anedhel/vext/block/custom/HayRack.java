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

import com.anedhel.vext.item.ModItems;
import com.mojang.serialization.MapCodec;
import it.unimi.dsi.fastutil.objects.Object2FloatMap;
import it.unimi.dsi.fastutil.objects.Object2FloatOpenHashMap;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.InventoryProvider;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.ai.pathing.NavigationType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.SidedInventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemConvertible;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.stat.Stats;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.IntProperty;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;
import net.minecraft.world.WorldView;
import net.minecraft.world.event.GameEvent;
import net.minecraft.world.tick.ScheduledTickView;
import org.jspecify.annotations.Nullable;

/**
 * Represents a HayRack block used for turning grass and leaves into hay.
 * <p>
 * It works like a composter: grass, leaves and wheat are put in by right-click or by a hopper from above.
 * Every item has a chance to raise the {@link #LEVEL} by one (see {@link #registerDefaultHayRackItems()}).
 * When the rack is full ({@link #MAX_FILL_LEVEL}), the hay dries for 20 ± 2 seconds and then the rack is
 * {@link #READY_LEVEL ready}. Right-clicking a ready rack (or a hopper below it) takes out the {@link #OUTPUT}.
 * <p>
 * The rack needs a full block below it and gives a comparator signal equal to its level.
 *
 * @author Ano_Jedt
 * @since after 0.1.0
 */
public class HayRack extends Block implements InventoryProvider {
	public static final MapCodec<HayRack> CODEC = createCodec(HayRack::new);

	/** Level of an empty rack. */
	public static final int MIN_LEVEL = 0;
	/** Highest level reached by putting items in, uses model {@code hay_rack_fill8}. */
	public static final int MAX_FILL_LEVEL = 8;
	/** Level after drying, uses model {@code hay_rack_fill_hay}. */
	public static final int READY_LEVEL = 9;
	public static final IntProperty LEVEL = IntProperty.of("level", MIN_LEVEL, READY_LEVEL);

	/** Drying time from full to ready: 20 seconds ± 2 seconds (in ticks). */
	private static final int DRYING_TICKS = 20 * 20;
	private static final int DRYING_TICKS_VARIATION = 2 * 20;

	/**
	 * The item taken out of a ready hay rack.
	 * TODO: hay is a placeholder for now, decide later what the output should be.
	 */
	public static final Item OUTPUT = ModItems.HAY;

	public static final Object2FloatMap<ItemConvertible> ITEM_TO_LEVEL_INCREASE_CHANCE = new Object2FloatOpenHashMap<>();

	public HayRack(Settings settings) {
		super(settings);
		setDefaultState(getStateManager().getDefaultState().with(LEVEL, MIN_LEVEL));
	}

	@Override
	public MapCodec<HayRack> getCodec() {
		return CODEC;
	}

	/**
	 * Registers the items that can be put into the hay rack and their chance to raise the level.
	 * This method should be called once during mod initialization.
	 */
	public static void registerDefaultHayRackItems() {
		ITEM_TO_LEVEL_INCREASE_CHANCE.defaultReturnValue(-1.0F);
		registerHayItems(0.5F, Items.JUNGLE_LEAVES);
		registerHayItems(0.5F, Items.OAK_LEAVES);
		registerHayItems(0.5F, Items.SPRUCE_LEAVES);
		registerHayItems(0.5F, Items.DARK_OAK_LEAVES);
		registerHayItems(0.5F, Items.PALE_OAK_LEAVES);
		registerHayItems(0.5F, Items.ACACIA_LEAVES);
		registerHayItems(0.5F, Items.CHERRY_LEAVES);
		registerHayItems(0.5F, Items.BIRCH_LEAVES);
		registerHayItems(0.5F, Items.AZALEA_LEAVES);
		registerHayItems(0.5F, Items.MANGROVE_LEAVES);
		registerHayItems(0.5F, Items.SHORT_GRASS);
		registerHayItems(0.5F, Items.WILDFLOWERS);
		registerHayItems(0.5F, Items.SHORT_DRY_GRASS);
		registerHayItems(0.5F, Items.TALL_DRY_GRASS);
		registerHayItems(1.0F, Items.TALL_GRASS);
		registerHayItems(1.0F, Items.WHEAT);
	}

	private static void registerHayItems(float levelIncreaseChance, ItemConvertible item) {
		ITEM_TO_LEVEL_INCREASE_CHANCE.put(item.asItem(), levelIncreaseChance);
	}

	@Override
	protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
		builder.add(LEVEL);
	}

	@Override
	protected void onBlockAdded(BlockState state, World world, BlockPos pos, BlockState oldState, boolean notify) {
		if (state.get(LEVEL) == MAX_FILL_LEVEL) {
			scheduleDrying(world, pos, state.getBlock());
		}
	}

	@Override
	protected ActionResult onUseWithItem(ItemStack stack, BlockState state, World world, BlockPos pos,
			PlayerEntity player, Hand hand, BlockHitResult hit) {
		int level = state.get(LEVEL);
		if (level < READY_LEVEL && ITEM_TO_LEVEL_INCREASE_CHANCE.containsKey(stack.getItem())) {
			if (level < MAX_FILL_LEVEL && !world.isClient()) {
				addToHayRack(player, state, world, pos, stack);
				playFillEffects(world, pos);
				player.incrementStat(Stats.USED.getOrCreateStat(stack.getItem()));
				stack.decrementUnlessCreative(1, player);
			}
			return ActionResult.SUCCESS;
		}
		return super.onUseWithItem(stack, state, world, pos, player, hand, hit);
	}

	@Override
	protected ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
		if (state.get(LEVEL) == READY_LEVEL) {
			emptyFullHayRack(player, state, world, pos);
			return ActionResult.SUCCESS;
		}
		return ActionResult.PASS;
	}

	/**
	 * Drops the {@link #OUTPUT} above the rack and empties it.
	 */
	public static BlockState emptyFullHayRack(Entity user, BlockState state, World world, BlockPos pos) {
		if (!world.isClient()) {
			Vec3d vec3d = Vec3d.add(pos, 0.5, 1.01, 0.5).addHorizontalRandom(world.random, 0.7F);
			ItemEntity itemEntity = new ItemEntity(world, vec3d.getX(), vec3d.getY(), vec3d.getZ(), new ItemStack(OUTPUT));
			itemEntity.setToDefaultPickupDelay();
			world.spawnEntity(itemEntity);
		}

		BlockState blockState = emptyHayRack(user, state, world, pos);
		world.playSound(null, pos, SoundEvents.BLOCK_COMPOSTER_EMPTY, SoundCategory.BLOCKS, 1.0F, 1.0F);
		return blockState;
	}

	static BlockState emptyHayRack(@Nullable Entity user, BlockState state, WorldAccess world, BlockPos pos) {
		BlockState blockState = state.with(LEVEL, MIN_LEVEL);
		world.setBlockState(pos, blockState, Block.NOTIFY_ALL);
		world.emitGameEvent(GameEvent.BLOCK_CHANGE, pos, GameEvent.Emitter.of(user, blockState));
		return blockState;
	}

	/**
	 * Tries to raise the level by one, using the chance of the item. The first item always raises the level.
	 */
	static BlockState addToHayRack(@Nullable Entity user, BlockState state, WorldAccess world, BlockPos pos, ItemStack stack) {
		int level = state.get(LEVEL);
		float chance = ITEM_TO_LEVEL_INCREASE_CHANCE.getFloat(stack.getItem());
		if ((level != MIN_LEVEL || !(chance > 0.0F)) && !(world.getRandom().nextDouble() < chance)) {
			return state;
		}

		int newLevel = level + 1;
		BlockState blockState = state.with(LEVEL, newLevel);
		world.setBlockState(pos, blockState, Block.NOTIFY_ALL);
		world.emitGameEvent(GameEvent.BLOCK_CHANGE, pos, GameEvent.Emitter.of(user, blockState));
		if (newLevel == MAX_FILL_LEVEL) {
			scheduleDrying(world, pos, state.getBlock());
		}
		return blockState;
	}

	private static void scheduleDrying(WorldAccess world, BlockPos pos, Block block) {
		int variation = world.getRandom().nextBetween(-DRYING_TICKS_VARIATION, DRYING_TICKS_VARIATION);
		world.scheduleBlockTick(pos, block, DRYING_TICKS + variation);
	}

	/**
	 * Plays the grass break sound and composter particles when an item is put in (server side).
	 */
	private static void playFillEffects(WorldAccess world, BlockPos pos) {
		world.playSound(null, pos, SoundEvents.BLOCK_GRASS_BREAK, SoundCategory.BLOCKS, 1.0F, 1.0F);
		if (world instanceof ServerWorld serverWorld) {
			serverWorld.spawnParticles(ParticleTypes.COMPOSTER, pos.getX() + 0.5, pos.getY() + 0.8, pos.getZ() + 0.5,
					10, 0.3, 0.15, 0.3, 0.02);
		}
	}

	/**
	 * Turns a full rack into a ready rack after the drying time.
	 */
	@Override
	protected void scheduledTick(BlockState state, ServerWorld world, BlockPos pos, Random random) {
		if (state.get(LEVEL) == MAX_FILL_LEVEL) {
			world.setBlockState(pos, state.with(LEVEL, READY_LEVEL), Block.NOTIFY_ALL);
		}
	}

	/**
	 * The hay rack needs a full solid top face below it.
	 */
	@Override
	protected boolean canPlaceAt(BlockState state, WorldView world, BlockPos pos) {
		BlockPos below = pos.down();
		return world.getBlockState(below).isSideSolidFullSquare(world, below, Direction.UP);
	}

	/**
	 * Breaks the rack (with its loot) when the block below is removed.
	 */
	@Override
	protected BlockState getStateForNeighborUpdate(BlockState state, WorldView world, ScheduledTickView tickView,
			BlockPos pos, Direction direction, BlockPos neighborPos, BlockState neighborState, Random random) {
		if (direction == Direction.DOWN && !state.canPlaceAt(world, pos)) {
			return Blocks.AIR.getDefaultState();
		}
		return state;
	}

	@Override
	protected boolean hasComparatorOutput(BlockState state) {
		return true;
	}

	/**
	 * Comparator signal strength: the current level (0 empty, 8 full, 9 ready).
	 */
	@Override
	protected int getComparatorOutput(BlockState state, World world, BlockPos pos, Direction direction) {
		return state.get(LEVEL);
	}

	@Override
	protected boolean canPathfindThrough(BlockState state, NavigationType type) {
		return false;
	}

	/**
	 * Hoppers can put items in from above while the rack is filling,
	 * and take the {@link #OUTPUT} out from below when it is ready.
	 */
	@Override
	public SidedInventory getInventory(BlockState state, WorldAccess world, BlockPos pos) {
		int level = state.get(LEVEL);
		if (level == READY_LEVEL) {
			return new FullHayRackInventory(state, world, pos, new ItemStack(OUTPUT));
		}
		return level < MAX_FILL_LEVEL ? new HayRackInventory(state, world, pos) : new DummyInventory();
	}

	static class FullHayRackInventory extends SimpleInventory implements SidedInventory {
		private final BlockState state;
		private final WorldAccess world;
		private final BlockPos pos;
		private boolean dirty;

		public FullHayRackInventory(BlockState state, WorldAccess world, BlockPos pos, ItemStack outputItem) {
			super(outputItem);
			this.state = state;
			this.world = world;
			this.pos = pos;
		}

		@Override
		public int getMaxCountPerStack() {
			return 1;
		}

		@Override
		public int[] getAvailableSlots(Direction side) {
			return side == Direction.DOWN ? new int[]{0} : new int[0];
		}

		@Override
		public boolean canInsert(int slot, ItemStack stack, @Nullable Direction dir) {
			return false;
		}

		@Override
		public boolean canExtract(int slot, ItemStack stack, Direction dir) {
			return !this.dirty && dir == Direction.DOWN && stack.isOf(OUTPUT);
		}

		@Override
		public void markDirty() {
			HayRack.emptyHayRack(null, this.state, this.world, this.pos);
			this.dirty = true;
		}
	}

	static class HayRackInventory extends SimpleInventory implements SidedInventory {
		private final BlockState state;
		private final WorldAccess world;
		private final BlockPos pos;
		private boolean dirty;

		public HayRackInventory(BlockState state, WorldAccess world, BlockPos pos) {
			super(1);
			this.state = state;
			this.world = world;
			this.pos = pos;
		}

		@Override
		public int getMaxCountPerStack() {
			return 1;
		}

		@Override
		public int[] getAvailableSlots(Direction side) {
			return side == Direction.UP ? new int[]{0} : new int[0];
		}

		@Override
		public boolean canInsert(int slot, ItemStack stack, @Nullable Direction dir) {
			return !this.dirty && dir == Direction.UP && HayRack.ITEM_TO_LEVEL_INCREASE_CHANCE.containsKey(stack.getItem());
		}

		@Override
		public boolean canExtract(int slot, ItemStack stack, Direction dir) {
			return false;
		}

		@Override
		public void markDirty() {
			ItemStack itemStack = this.getStack(0);
			if (!itemStack.isEmpty()) {
				this.dirty = true;
				HayRack.addToHayRack(null, this.state, this.world, this.pos, itemStack);
				HayRack.playFillEffects(this.world, this.pos);
				this.removeStack(0);
			}
		}
	}

	static class DummyInventory extends SimpleInventory implements SidedInventory {
		public DummyInventory() {
			super(0);
		}

		@Override
		public int[] getAvailableSlots(Direction side) {
			return new int[0];
		}

		@Override
		public boolean canInsert(int slot, ItemStack stack, @Nullable Direction dir) {
			return false;
		}

		@Override
		public boolean canExtract(int slot, ItemStack stack, Direction dir) {
			return false;
		}
	}
}
