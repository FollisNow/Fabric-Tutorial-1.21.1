package net.follis.tutorialmod.effect;

import net.follis.tutorialmod.TutorialMod;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectCategory;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.Identifier;

public class ModEffects {
    public static final RegistryEntry<StatusEffect> SLIMEY = registerStatusEffect("slimey",
            new SlimeyEffect(StatusEffectCategory.NEUTRAL, 0x36ebab)
                    .addAttributeModifier(EntityAttributes.GENERIC_MOVEMENT_SPEED,
                            Identifier.of(TutorialMod.MOD_ID, "slimey"), -0.25f,
                            EntityAttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));

    public static final RegistryEntry<StatusEffect> STRANGE_ATTRACTOR = registerStatusEffect("strange_attractor",
            new StrangeAttractorEffect(StatusEffectCategory.NEUTRAL, 0x06baf9)
                    .addAttributeModifier(EntityAttributes.GENERIC_MOVEMENT_SPEED,
                            Identifier.of(TutorialMod.MOD_ID, "strange_attractor"), 0.05f,
                            EntityAttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));


    public static final RegistryEntry<StatusEffect> REJUNEVATION = registerStatusEffect("rejuvenation",
            new RejuvenationEffect(StatusEffectCategory.NEUTRAL, 0xd2d03c));

    public static final RegistryEntry<StatusEffect> HATRED = registerStatusEffect("hatred",
            new HatredEffect(StatusEffectCategory.HARMFUL, 0xbf2c2c));

    public static final RegistryEntry<StatusEffect> LOVE = registerStatusEffect("love",
            new LoveEffect(StatusEffectCategory.BENEFICIAL, 0xff94cc));

    public static final RegistryEntry<StatusEffect> SPORULATION = registerStatusEffect("sporulation",
            new SporulationEffect(StatusEffectCategory.NEUTRAL, 0xb36d0b));


    private static RegistryEntry<StatusEffect> registerStatusEffect(String name, StatusEffect statusEffect) {
        return Registry.registerReference(Registries.STATUS_EFFECT, Identifier.of(TutorialMod.MOD_ID, name), statusEffect);
    }

    public static void registerEffects() {
        TutorialMod.LOGGER.info("Registering Mod Effects for " + TutorialMod.MOD_ID);
    }
}
