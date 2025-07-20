package net.follis.tutorialmod.potion;

import net.follis.tutorialmod.TutorialMod;
import net.follis.tutorialmod.effect.ModEffects;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.potion.Potion;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.Identifier;

public class ModPotions {
    public static final RegistryEntry<Potion> SLIMEY_POTION = registerPotion("slimey_potion",
            new Potion(new StatusEffectInstance(ModEffects.SLIMEY, 1200, 0)));

    public static final RegistryEntry<Potion> ATTRACTION_POTION = registerPotion("attraction_potion",
            new Potion(new StatusEffectInstance(ModEffects.STRANGE_ATTRACTOR, 18000, 0)));

    public static final RegistryEntry<Potion> REJUVENATION_POTION = registerPotion("rejuvenation_potion",
            new Potion(new StatusEffectInstance(ModEffects.REJUNEVATION, 100, 0)));

    public static final RegistryEntry<Potion> HATRED_POTION = registerPotion("hatred_potion",
            new Potion(new StatusEffectInstance(ModEffects.HATRED, 4800, 0)));

    public static final RegistryEntry<Potion> LOVE_POTION = registerPotion("love_potion",
            new Potion(new StatusEffectInstance(ModEffects.LOVE, 1200, 0)));


    private static RegistryEntry<Potion> registerPotion(String name, Potion potion) {
        return Registry.registerReference(Registries.POTION, Identifier.of(TutorialMod.MOD_ID, name), potion);
    }

    public static void registerPotions() {
        TutorialMod.LOGGER.info("Registering Mod Potions for " + TutorialMod.MOD_ID);
    }
}
