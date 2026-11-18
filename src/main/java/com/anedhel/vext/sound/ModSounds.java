/*
 * Copyright (c) 2026.
 * Authors/Developers are listed in the CONTRIBUTING.md
 *
 * The Vanilla Extensions Mod is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * The Vanilla Extensions Mod is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program. If not, see <https://www.gnu.org/licenses/>
 */

package com.anedhel.vext.sound;

import com.anedhel.vext.VExtMod;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;

/**
 * This class is responsible for registering all custom sound events and block sound groups for the mod.
 * <p>
 * Every sound event registered here needs a matching entry in {@code assets/vext/sounds.ogg}.
 *
 * @author AnoJedt
 * @since after 0.1.0
 */
public class ModSounds {

    /**
     * Sound played when a {@link com.anedhel.vext.block.custom.SpiderWebBlock} is broken.
     */
    public static final SoundEvent SPIDER_WEB_BREAK = registerSoundEvent("spider_web_break");

    /**
     * Block sound group for the spider web. Uses {@link #SPIDER_WEB_BREAK}
     * for break, step, place, hit and fall sounds.
     */
    public static final BlockSoundGroup SPIDER_WEB_SOUNDS = new BlockSoundGroup(1f, 1f,
            SPIDER_WEB_BREAK, SPIDER_WEB_BREAK, SPIDER_WEB_BREAK, SPIDER_WEB_BREAK, SPIDER_WEB_BREAK);

    /**
     * Registers a sound event under the mod namespace.
     *
     * @param name the path of the sound event, as used in {@code sounds.json}
     * @return the registered sound event
     */
    private static SoundEvent registerSoundEvent(String name) {
        Identifier id = Identifier.of(VExtMod.MOD_ID, name);
        return Registry.register(Registries.SOUND_EVENT, id, SoundEvent.of(id));
    }

    /**
     * Registers all mod sounds. This method should be called during the mod initialization phase.
     */
    public static void registerSounds() {
        VExtMod.LOGGER.info("Registering Mod Sounds for " + VExtMod.MOD_ID);
    }
}
