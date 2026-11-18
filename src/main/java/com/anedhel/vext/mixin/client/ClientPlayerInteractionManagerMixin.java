/*
 * Copyright (c) 2026.
 * Authors/Developers are listed in the CONTRIBUTING.md
 *
 * The Vanilla Extensions Mod is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * The Vanilla Extensions Mod is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program. If not, see <https://www.gnu.org/licenses/>
 */

package com.anedhel.vext.mixin.client;

import com.anedhel.vext.block.custom.PebblesBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Client-side prediction for breaking a {@link PebblesBlock}.
 * <p>
 * The server only removes one pebble per click (see {@link PebblesBlock#registerBreakHandler()}).
 * Without this mixin, the client would predict a normal break and show air until the server
 * sends the new pebble count, so the block flickers. This mixin makes the client remove one
 * pebble too, so the client already shows what the server will confirm.
 * The break packet is still sent to the server as usual.
 *
 * @author AnoJedt
 * @since after 0.1.0
 */
@Mixin(ClientPlayerInteractionManager.class)
public abstract class ClientPlayerInteractionManagerMixin {

	@Shadow
	@Final
	private MinecraftClient client;

	/**
	 * Runs after vanilla checked that the player may break the block, right before the block is removed.
	 */
	@Inject(method = "breakBlock", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/block/Block;onBreak(Lnet/minecraft/world/World;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/BlockState;Lnet/minecraft/entity/player/PlayerEntity;)Lnet/minecraft/block/BlockState;"),
			cancellable = true)
	private void vext$removeOnePebble(BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
		if (client.world == null || client.player == null) {
			return;
		}
		BlockState state = client.world.getBlockState(pos);
		if (PebblesBlock.tryRemoveOnePebble(client.world, client.player, pos, state)) {
			cir.setReturnValue(true);
		}
	}
}
