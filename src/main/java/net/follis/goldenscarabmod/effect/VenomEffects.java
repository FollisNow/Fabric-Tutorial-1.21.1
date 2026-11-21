package net.follis.goldenscarabmod.effect;

import net.follis.goldenscarabmod.GoldenScarabMod;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.NoPenaltyTargeting;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectCategory;
import net.minecraft.entity.mob.Angerable;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.entity.passive.WolfEntity;
import net.minecraft.predicate.entity.EntityPredicates;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;

import java.util.List;

/** Venom effects given by scorpion stings. Behaviour lives in VenomEvents and the *VenomMixin classes. */
public final class VenomEffects {
    // --- marker effects: only a flag, checked by events/mixins ---
    public static final RegistryEntry<StatusEffect> ARCHER   = reg("archer",   new MarkerEffect(0x7A5C3A));
    public static final RegistryEntry<StatusEffect> ARMS_UP  = reg("arms_up",  new MarkerEffect(0xB0B0B0));
    public static final RegistryEntry<StatusEffect> BLADE    = reg("blade",    new MarkerEffect(0x8FA3AD));
    public static final RegistryEntry<StatusEffect> DANGER   = reg("danger",   new MarkerEffect(0x4CAF50));
    public static final RegistryEntry<StatusEffect> EXPLORER = reg("explorer", new MarkerEffect(0xC2A878));
    public static final RegistryEntry<StatusEffect> FLOW     = reg("flow",     new MarkerEffect(0x3F76E4));
    public static final RegistryEntry<StatusEffect> HEART    = reg("heart",    new MarkerEffect(0xC43C3C));
    public static final RegistryEntry<StatusEffect> MINER    = reg("miner",    new MarkerEffect(0x6B6B6B));
    public static final RegistryEntry<StatusEffect> MOURNER  = reg("mourner",  new MarkerEffect(0x4A3B5C));
    public static final RegistryEntry<StatusEffect> PLENTY   = reg("plenty",   new MarkerEffect(0x9C6B30));
    public static final RegistryEntry<StatusEffect> PRIZE    = reg("prize",    new MarkerEffect(0x3DDC84));
    public static final RegistryEntry<StatusEffect> SCRAPE   = reg("scrape",   new MarkerEffect(0xB87333));
    public static final RegistryEntry<StatusEffect> SHEAF    = reg("sheaf",    new MarkerEffect(0xD8C36A));
    public static final RegistryEntry<StatusEffect> SHELTER  = reg("shelter",  new MarkerEffect(0x8B5A2B));
    public static final RegistryEntry<StatusEffect> SNORT    = reg("snort",    new MarkerEffect(0xC9A0DC));

    // --- attribute effects ---
    public static final RegistryEntry<StatusEffect> GUSTER = reg("guster", new MarkerEffect(0xBFE9F5)
            .addAttributeModifier(EntityAttributes.GENERIC_ATTACK_KNOCKBACK,
                    Identifier.of(GoldenScarabMod.MOD_ID, "effect.guster"), 1.5,
                    EntityAttributeModifier.Operation.ADD_VALUE));
    public static final RegistryEntry<StatusEffect> SKULL = reg("skull", new MarkerEffect(0x2B2B2B)
            .addAttributeModifier(EntityAttributes.GENERIC_MAX_HEALTH,
                    Identifier.of(GoldenScarabMod.MOD_ID, "effect.skull"), -6.0,
                    EntityAttributeModifier.Operation.ADD_VALUE));

    // --- active effects ---
    public static final RegistryEntry<StatusEffect> HEARTBREAK = reg("heartbreak", new HeartbreakEffect());
    public static final RegistryEntry<StatusEffect> HOWL = reg("howl", new HowlEffect());

    private static RegistryEntry<StatusEffect> reg(String name, StatusEffect effect) {
        return Registry.registerReference(Registries.STATUS_EFFECT, Identifier.of(GoldenScarabMod.MOD_ID, name), effect);
    }

    public static void register() {
        GoldenScarabMod.LOGGER.info("Registering venom effects for " + GoldenScarabMod.MOD_ID);
    }

    public static class MarkerEffect extends StatusEffect {
        public MarkerEffect(int color) {
            super(StatusEffectCategory.HARMFUL, color);
        }
    }

    /** Nearby mobs lose the target and run away from it. */
    static class HeartbreakEffect extends StatusEffect {
        HeartbreakEffect() { super(StatusEffectCategory.HARMFUL, 0xE0508C); }

        @Override
        public boolean canApplyUpdateEffect(int duration, int amplifier) {
            return duration % 5 == 0;
        }

        @Override
        public boolean applyUpdateEffect(LivingEntity entity, int amplifier) {
            if (!(entity.getWorld() instanceof ServerWorld world)) return true;
            List<PathAwareEntity> mobs = world.getEntitiesByClass(PathAwareEntity.class,
                    entity.getBoundingBox().expand(10), mob -> mob != entity && mob.isAlive());
            for (PathAwareEntity mob : mobs) {
                if (mob.getTarget() == entity) mob.setTarget(null);
                if (mob instanceof Angerable angerable && entity.getUuid().equals(angerable.getAngryAt())) {
                    angerable.stopAnger();
                }
                Vec3d away = NoPenaltyTargeting.findFrom(mob, 12, 6, entity.getPos());
                if (away != null) mob.getNavigation().startMovingTo(away.x, away.y, away.z, 1.3);
            }
            return true;
        }
    }

    /** Untamed wolves nearby are angered at the target. */
    static class HowlEffect extends StatusEffect {
        HowlEffect() { super(StatusEffectCategory.HARMFUL, 0x9AA5B1); }

        @Override
        public boolean canApplyUpdateEffect(int duration, int amplifier) {
            return duration % 20 == 0;
        }

        @Override
        public boolean applyUpdateEffect(LivingEntity entity, int amplifier) {
            if (!(entity.getWorld() instanceof ServerWorld world)
                    || !EntityPredicates.EXCEPT_CREATIVE_OR_SPECTATOR.test(entity)) return true;
            for (WolfEntity wolf : world.getEntitiesByClass(WolfEntity.class,
                    entity.getBoundingBox().expand(20), wolf -> !wolf.isTamed() && wolf.isAlive())) {
                wolf.setAngryAt(entity.getUuid());
                wolf.setAngerTime(600);
                wolf.setTarget(entity);
            }
            return true;
        }
    }
}