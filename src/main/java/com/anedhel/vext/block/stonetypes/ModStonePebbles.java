/*
 * Copyright (c) 2026.
 * Authors/Developers are listed in the CONTRIBUTING.md
 *
 * The Vanilla Extensions Mod is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * The Vanilla Extensions Mod is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program. If not, see <https://www.gnu.org/licenses/>
 */

package com.anedhel.vext.block.stonetypes;

import com.anedhel.vext.VExtMod;
import com.anedhel.vext.block.custom.PebblesBlock;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.piston.PistonBehavior;
import net.minecraft.sound.BlockSoundGroup;

import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

import static com.anedhel.vext.block.ModBlocks.registerBlock;

/**
 * Registers one {@link PebblesBlock} for every {@link ModStoneTypes stone type}.
 * <p>
 * The blocks are named {@code <stone type>_pebbles} (e.g. {@code blueslate_pebbles}).
 * New stone types added to {@link ModStoneTypes} get their pebbles automatically,
 * including models, loot table, recipes and translation from datagen.
 *
 * @author AnoJedt
 * @since after 0.1.0
 */
public class ModStonePebbles {

	private static final Map<ModStoneTypes, Block> PEBBLES = createPebbles();

	private static Map<ModStoneTypes, Block> createPebbles() {
		EnumMap<ModStoneTypes, Block> pebbles = new EnumMap<>(ModStoneTypes.class);
		for (ModStoneTypes stoneType : ModStoneTypes.values()) {
			pebbles.put(stoneType, registerBlock(stoneType.getFileConformName() + "_pebbles",
					settings -> new PebblesBlock(settings
							.breakInstantly()
							.noCollision()
							.offset(AbstractBlock.OffsetType.XZ)
							.sounds(BlockSoundGroup.STONE)
							.pistonBehavior(PistonBehavior.DESTROY))));
		}
		return Collections.unmodifiableMap(pebbles);
	}

	/**
	 * Returns the pebbles block of the given stone type.
	 *
	 * @param stoneType the stone type
	 * @return the pebbles block of that stone type
	 */
	public static Block getPebbles(ModStoneTypes stoneType) {
		return PEBBLES.get(stoneType);
	}

	/**
	 * Returns all pebbles blocks, in the order of {@link ModStoneTypes}.
	 *
	 * @return all pebbles blocks
	 */
	public static Collection<Block> getAllPebbles() {
		return PEBBLES.values();
	}

	/**
	 * Registers all pebbles blocks and their break handler.
	 * This method should be called during the mod initialization phase, after the stone types are registered.
	 */
	public static void registerPebbles() {
		VExtMod.LOGGER.info("Registering Mod Stone Pebbles for " + VExtMod.MOD_ID);
		PebblesBlock.registerBreakHandler();
	}
}
