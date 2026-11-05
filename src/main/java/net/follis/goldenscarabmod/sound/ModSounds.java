package net.follis.goldenscarabmod.sound;

import net.follis.goldenscarabmod.GoldenScarabMod;
import net.minecraft.block.jukebox.JukeboxSong;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;

public class ModSounds {
    public static final SoundEvent CHISEL_USE = registerSoundEvent("chisel_use");

    public static final SoundEvent MAGIC_BLOCK_BREAK = registerSoundEvent("magic_block_break");
    public static final SoundEvent MAGIC_BLOCK_STEP = registerSoundEvent("magic_block_step");
    public static final SoundEvent MAGIC_BLOCK_PLACE = registerSoundEvent("magic_block_place");
    public static final SoundEvent MAGIC_BLOCK_HIT = registerSoundEvent("magic_block_hit");
    public static final SoundEvent MAGIC_BLOCK_FALL = registerSoundEvent("magic_block_fall");

    public static final SoundEvent MINI_SUN_SPAWN = registerSoundEvent("mini_sun_spawn");

    public static final BlockSoundGroup MAGIC_BLOCK_SOUNDS = new BlockSoundGroup(1f, 1f,
            MAGIC_BLOCK_BREAK, MAGIC_BLOCK_STEP, MAGIC_BLOCK_PLACE, MAGIC_BLOCK_HIT, MAGIC_BLOCK_FALL);

    public static final SoundEvent GOLD_PILE_BREAK = registerSoundEvent("gold_pile_break");
    public static final SoundEvent GOLD_PILE_STEP = registerSoundEvent("gold_pile_step");
    public static final SoundEvent GOLD_PILE_PLACE = registerSoundEvent("gold_pile_place");
    public static final SoundEvent GOLD_PILE_HIT = registerSoundEvent("gold_pile_hit");
    public static final SoundEvent GOLD_PILE_FALL = registerSoundEvent("gold_pile_fall");

    public static final BlockSoundGroup GOLD_PILE_SOUNDS = new BlockSoundGroup(1f, 1f,
            GOLD_PILE_BREAK, GOLD_PILE_STEP, GOLD_PILE_PLACE, GOLD_PILE_HIT, GOLD_PILE_FALL);

    //Boss
    public static final SoundEvent KING_IN_YELLOW_QUESTION = registerSoundEvent("king_in_yellow_question");
    public static final SoundEvent KING_IN_YELLOW_SUMMON = registerSoundEvent("king_in_yellow_summon");
    public static final SoundEvent KING_IN_YELLOW_CAST = registerSoundEvent("king_in_yellow_cast");
    public static final SoundEvent KING_IN_YELLOW_BELL = registerSoundEvent("king_in_yellow_bell");
    public static final SoundEvent KING_IN_YELLOW_DEATH = registerSoundEvent("king_in_yellow_death");
    public static final SoundEvent KING_IN_YELLOW_STEP = registerSoundEvent("king_in_yellow_step");

    public static final SoundEvent GOLDEN_SWARM_CLONE = registerSoundEvent("golden_swarm_clone");
    public static final SoundEvent GOLDEN_SWARM_RECOMBINE = registerSoundEvent("golden_swarm_recombine");
    public static final SoundEvent GOLDEN_SWARM_STEP = registerSoundEvent("golden_swarm_step");
    public static final SoundEvent GOLDEN_SWARM_WINDUP_BURST = registerSoundEvent("golden_swarm_windup_burst");
    public static final SoundEvent GOLDEN_SWARM_SHAPE = registerSoundEvent("golden_swarm_shape");

    public static final SoundEvent BAR_BRAWL = registerSoundEvent("bar_brawl");
    public static final RegistryKey<JukeboxSong> BAR_BRAWL_KEY =
            RegistryKey.of(RegistryKeys.JUKEBOX_SONG, Identifier.of(GoldenScarabMod.MOD_ID, "bar_brawl"));


    private static SoundEvent registerSoundEvent(String name) {
        Identifier id = Identifier.of(GoldenScarabMod.MOD_ID, name);
        return Registry.register(Registries.SOUND_EVENT, id, SoundEvent.of(id));
    }

    public static void registerSounds() {
        GoldenScarabMod.LOGGER.info("Registering Mod Sounds for " + GoldenScarabMod.MOD_ID);
    }
}
