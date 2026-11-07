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

import com.mojang.serialization.MapCodec;
import it.unimi.dsi.fastutil.objects.Object2FloatMap;
import it.unimi.dsi.fastutil.objects.Object2FloatOpenHashMap;
import net.minecraft.block.*;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.ai.pathing.NavigationType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.SidedInventory;
import net.minecraft.inventory.SimpleInventory;
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
import net.minecraft.state.property.Properties;
import net.minecraft.state.property.Property;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;
import net.minecraft.world.event.GameEvent;
import org.jspecify.annotations.Nullable;

/**
 * Represents a HayRack block used for turning grass and leaves into hay.
 * <p>
 * This block provides an interaction system with right-clicking.
 *
 * @author Ano_Jedt
 * @since after 0.1.0
 */

public class HayRack extends Block implements InventoryProvider {
    public static final MapCodec<HayRack> CODEC = createCodec(HayRack::new);
    public static final int NUM_LEVELS = 7;
    public static final int MIN_LEVEL = 0;
    public static final int MAX_LEVEL = 6;
    public static final IntProperty LEVEL;
    public static final Object2FloatMap<ItemConvertible> ITEM_TO_LEVEL_INCREASE_CHANCE;

    public MapCodec<HayRack> getCodec() {return CODEC;
    }

    public static void registerDefaultHayRackItems() {
        ITEM_TO_LEVEL_INCREASE_CHANCE.defaultReturnValue(-1.0F);
        float f = 0.5F;
        float g = 1.0F;
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
        ITEM_TO_LEVEL_INCREASE_CHANCE.put(item, levelIncreaseChance);
    }

    public HayRack(Settings settings) {
        super(settings);
    }


    public static void playEffects(World world, BlockPos pos, boolean fill) {
        BlockState blockState = world.getBlockState(pos);
        world.playSoundAtBlockCenterClient(pos, fill ? SoundEvents.BLOCK_COMPOSTER_FILL_SUCCESS : SoundEvents.BLOCK_COMPOSTER_FILL, SoundCategory.BLOCKS, 1.0F, 1.0F, false);
        double d = blockState.getOutlineShape(world, pos).getEndingCoord(Direction.Axis.Y, 0.5, 0.5) + 0.03125;
        double e = 2.0;
        double f = 0.1875;
        double g = 0.625;
        Random random = world.getRandom();

        for(int i = 0; i < 10; ++i) {
            double h = random.nextGaussian() * 0.02;
            double j = random.nextGaussian() * 0.02;
            double k = random.nextGaussian() * 0.02;
            world.addParticleClient(ParticleTypes.COMPOSTER, (double)pos.getX() + 0.1875 + 0.625 * (double)random.nextFloat(), (double)pos.getY() + d + (double)random.nextFloat() * (1.0 - d), (double)pos.getZ() + 0.1875 + 0.625 * (double)random.nextFloat(), h, j, k);
        }

    }

    protected void onBlockAdded(BlockState state, World world, BlockPos pos, BlockState oldState, boolean notify) {
        if ((Integer)state.get(LEVEL) == 6) {
            world.scheduleBlockTick(pos, state.getBlock(), 20);
        }
    }

    protected ActionResult onUseWithItem(ItemStack stack, BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
        int i = (Integer)state.get(LEVEL);
        if (i < 7 && ITEM_TO_LEVEL_INCREASE_CHANCE.containsKey(stack.getItem())) {
            if (i < 6 && !world.isClient()) {
                BlockState blockState = addToHayRack(player, state, world, pos, stack);
                world.syncWorldEvent(1500, pos, state != blockState ? 1 : 0);
                player.incrementStat(Stats.USED.getOrCreateStat(stack.getItem()));
                stack.decrementUnlessCreative(1, player);
            }

            return ActionResult.SUCCESS;
        } else {
            return super.onUseWithItem(stack, state, world, pos, player, hand, hit);
        }
    }

    protected ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
        int i = (Integer)state.get(LEVEL);
        if (i == 7) {
            emptyFullHayRack(player, state, world, pos);
            return ActionResult.SUCCESS;
        } else {
            return ActionResult.PASS;
        }
    }

    public static BlockState hayrack(Entity user, BlockState state, ServerWorld world, ItemStack stack, BlockPos pos) {
        int i = (Integer)state.get(LEVEL);
        if (i < 6 && ITEM_TO_LEVEL_INCREASE_CHANCE.containsKey(stack.getItem())) {
            BlockState blockState = addToHayRack(user, state, world, pos, stack);
            stack.decrement(1);
            return blockState;
        } else {
            return state;
        }
    }

    public static BlockState emptyFullHayRack(Entity user, BlockState state, World world, BlockPos pos) {
        if (!world.isClient()) {
            Vec3d vec3d = Vec3d.add(pos, 0.5, 1.01, 0.5).addHorizontalRandom(world.random, 0.7F);
            ItemEntity itemEntity = new ItemEntity(world, vec3d.getX(), vec3d.getY(), vec3d.getZ(), new ItemStack(Items.DIAMOND));
            itemEntity.setToDefaultPickupDelay();
            world.spawnEntity(itemEntity);
        }

        BlockState blockState = emptyHayRack(user, state, world, pos);
        world.playSound((Entity)null, pos, SoundEvents.BLOCK_COMPOSTER_EMPTY, SoundCategory.BLOCKS, 1.0F, 1.0F);
        return blockState;
    }

    static BlockState emptyHayRack(@Nullable Entity user, BlockState state, WorldAccess world, BlockPos pos) {
        BlockState blockState = (BlockState)state.with(LEVEL, 0);
        world.setBlockState(pos, blockState, 3);
        world.emitGameEvent(GameEvent.BLOCK_CHANGE, pos, GameEvent.Emitter.of(user, blockState));
        return blockState;
    }

    static BlockState addToHayRack(@Nullable Entity user, BlockState state, WorldAccess world, BlockPos pos, ItemStack stack) {
        int i = (Integer)state.get(LEVEL);
        float f = ITEM_TO_LEVEL_INCREASE_CHANCE.getFloat(stack.getItem());
        if ((i != 0 || !(f > 0.0F)) && !(world.getRandom().nextDouble() < (double)f)) {
            return state;
        } else {
            int j = i + 1;
            BlockState blockState = (BlockState)state.with(LEVEL, j);
            world.setBlockState(pos, blockState, 3);
            world.emitGameEvent(GameEvent.BLOCK_CHANGE, pos, GameEvent.Emitter.of(user, blockState));
            if (j == 6) {
                world.scheduleBlockTick(pos, state.getBlock(), 20);
            }

            return blockState;
        }
    }

    protected void scheduledTick(BlockState state, ServerWorld world, BlockPos pos, Random random) {
        if ((Integer)state.get(LEVEL) == 6) {
            world.setBlockState(pos, (BlockState)state.cycle(LEVEL), 3);
            /*world.playSound((Entity)null, pos, SoundEvents.BLOCK_HAYRACK_READY, SoundCategory.BLOCKS, 1.0F, 1.0F);*/
        }

    }

    protected boolean hasHayRackOutput(BlockState state) {
        return true;
    }

    protected int getHayRackOutput(BlockState state, World world, BlockPos pos, Direction direction) {
        return (Integer)state.get(LEVEL);
    }

    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(new Property[]{LEVEL});
    }

    protected boolean canPathfindThrough(BlockState state, NavigationType type) {
        return false;
    }

    public SidedInventory getInventory(BlockState state, WorldAccess world, BlockPos pos) {
        int i = (Integer)state.get(LEVEL);
        if (i == 7) {
            return new HayRack.FullHayRackInventory(state, world, pos, new ItemStack(Items.DIAMOND));
        } else {
            return (SidedInventory)(i < 6 ? new HayRack.HayRackInventory(state, world, pos) : new HayRack.DummyInventory());
        }
    }

    static {
        LEVEL = Properties.LEVEL_8;
        ITEM_TO_LEVEL_INCREASE_CHANCE = new Object2FloatOpenHashMap();
    }

    static class FullHayRackInventory extends SimpleInventory implements SidedInventory {
        private final BlockState state;
        private final WorldAccess world;
        private final BlockPos pos;
        private boolean dirty;

        public FullHayRackInventory(BlockState state, WorldAccess world, BlockPos pos, ItemStack outputItem) {
            super(new ItemStack[]{outputItem});
            this.state = state;
            this.world = world;
            this.pos = pos;
        }

        public int getMaxCountPerStack() {
            return 1;
        }

        public int[] getAvailableSlots(Direction side) {
            return side == Direction.DOWN ? new int[]{0} : new int[0];
        }

        public boolean canInsert(int slot, ItemStack stack, @Nullable Direction dir) {
            return false;
        }

        public boolean canExtract(int slot, ItemStack stack, Direction dir) {
            return !this.dirty && dir == Direction.DOWN && stack.isOf(Items.DIAMOND);
        }

        public void markDirty() {
            HayRack.emptyHayRack((Entity)null, this.state, this.world, this.pos);
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

        public int getMaxCountPerStack() {
            return 1;
        }

        public int[] getAvailableSlots(Direction side) {
            return side == Direction.UP ? new int[]{0} : new int[0];
        }

        public boolean canInsert(int slot, ItemStack stack, @Nullable Direction dir) {
            return !this.dirty && dir == Direction.UP && HayRack.ITEM_TO_LEVEL_INCREASE_CHANCE.containsKey(stack.getItem());
        }

        public boolean canExtract(int slot, ItemStack stack, Direction dir) {
            return false;
        }

        public void markDirty() {
            ItemStack itemStack = this.getStack(0);
            if (!itemStack.isEmpty()) {
                this.dirty = true;
                BlockState blockState = HayRack.addToHayRack((Entity)null, this.state, this.world, this.pos, itemStack);
                this.world.syncWorldEvent(1500, this.pos, blockState != this.state ? 1 : 0);
                this.removeStack(0);
            }
        }
    }

    static class DummyInventory extends SimpleInventory implements SidedInventory {
        public DummyInventory() {
            super(0);
        }

        public int[] getAvailableSlots(Direction side) {
            return new int[0];
        }

        public boolean canInsert(int slot, ItemStack stack, @Nullable Direction dir) {
            return false;
        }

        public boolean canExtract(int slot, ItemStack stack, Direction dir) {
            return false;
        }
    }
}
