/*
 * Copyright (c) 2026.
 * Authors/Developers are listed in the CONTRIBUTING.md
 *
 * The Vanilla Extensions Mod is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * The Vanilla Extensions Mod is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program. If not, see <https://www.gnu.org/licenses/>
 */

/*
 * TODO: Vanilla stone pebbles have no crafting recipes yet.
 *  The cobbled variants of the vanilla stones (cobbled granite, cobbled diorite, ...) will be added later.
 *  When they exist, pass the cobbled block as third argument of vanilla(...) in createPebbles(),
 *  then datagen creates the recipes (2 cobbled diagonal -> 18 pebbles, 3x3 pebbles -> 1 cobbled) automatically.
 */

package com.anedhel.vext.block.stonetypes;

import com.anedhel.vext.VExtMod;
import com.anedhel.vext.block.custom.PebblesBlock;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.block.piston.PistonBehavior;
import net.minecraft.sound.BlockSoundGroup;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.anedhel.vext.block.ModBlocks.registerBlock;

/**
 * Registers one {@link PebblesBlock} for every {@link ModStoneTypes mod stone type} and for the vanilla stones.
 * <p>
 * The blocks are named {@code <stone>_pebbles} (e.g. {@code blueslate_pebbles}, {@code granite_pebbles}).
 * New stone types added to {@link ModStoneTypes} get their pebbles automatically,
 * including models, loot table, recipes and translation from datagen.
 * The pebbles use the texture of their base stone block.
 *
 * @author AnoJedt
 * @since after 0.1.0
 */
public class ModStonePebbles {

	/**
	 * One pebbles block with the data datagen needs.
	 *
	 * @param pebbles     the registered pebbles block
	 * @param baseStone   the stone block whose texture the pebbles use
	 * @param cobbled     the cobbled block used in the recipes, {@code null} if there are no recipes yet
	 * @param displayName the English name of the pebbles block
	 */
	public record StonePebbles(Block pebbles, Block baseStone, @Nullable Block cobbled, String displayName) {
	}

	private static final List<StonePebbles> PEBBLES = createPebbles();

	private static List<StonePebbles> createPebbles() {
		List<StonePebbles> pebbles = new ArrayList<>();

		for (ModStoneTypes stoneType : ModStoneTypes.values()) {
			ModStoneSet stoneSet = stoneType.getModStoneSet();
			pebbles.add(create(stoneType.getFileConformName(), stoneType.getName(),
					stoneSet.getStoneVariant("base"), stoneSet.getCobbledVariant("base")));
		}

		pebbles.add(vanilla("stone", "Stone", Blocks.STONE));
		pebbles.add(vanilla("granite", "Granite", Blocks.GRANITE));
		pebbles.add(vanilla("diorite", "Diorite", Blocks.DIORITE));
		pebbles.add(vanilla("andesite", "Andesite", Blocks.ANDESITE));
		pebbles.add(vanilla("deepslate", "Deepslate", Blocks.DEEPSLATE));
		pebbles.add(vanilla("tuff", "Tuff", Blocks.TUFF));
		pebbles.add(vanilla("calcite", "Calcite", Blocks.CALCITE));
		pebbles.add(vanilla("blackstone", "Blackstone", Blocks.BLACKSTONE));

		return Collections.unmodifiableList(pebbles);
	}

	/**
	 * Creates pebbles for a vanilla stone, without recipes (see the TODO at the top of this file).
	 */
	private static StonePebbles vanilla(String name, String displayName, Block baseStone) {
		return create(name, displayName, baseStone, null);
	}

	private static StonePebbles create(String name, String displayName, Block baseStone, @Nullable Block cobbled) {
		Block block = registerBlock(name + "_pebbles",
				settings -> new PebblesBlock(settings
						.breakInstantly()
						.noCollision()
						.offset(AbstractBlock.OffsetType.XZ)
						.sounds(BlockSoundGroup.STONE)
						.pistonBehavior(PistonBehavior.DESTROY)));
		return new StonePebbles(block, baseStone, cobbled, displayName + " Pebbles");
	}

	/**
	 * Returns all pebbles: first the mod stone types (in the order of {@link ModStoneTypes}), then the vanilla stones.
	 *
	 * @return all pebbles with their datagen data
	 */
	public static List<StonePebbles> getAll() {
		return PEBBLES;
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
