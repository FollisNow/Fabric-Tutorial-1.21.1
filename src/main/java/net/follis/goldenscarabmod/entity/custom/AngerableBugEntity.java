package net.follis.goldenscarabmod.entity.custom;

import net.follis.goldenscarabmod.util.IBugVariants;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.ActiveTargetGoal;
import net.minecraft.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.entity.ai.goal.RevengeGoal;
import net.minecraft.entity.boss.dragon.EnderDragonEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageTypes;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.mob.Angerable;
import net.minecraft.entity.mob.CreeperEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.Monster;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.TimeHelper;
import net.minecraft.util.math.intprovider.UniformIntProvider;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * Shared base for the hostile-when-angry bugs (Beetle, Moth, ...).
 *
 * <p>Handles: anger storage + ticking + NBT, gold/immunity checks, the common combat goals and the
 * breeding cooldown. Subclasses can restrict who is allowed to fight by overriding {@link #canFight()}.
 */
public abstract class AngerableBugEntity extends AnimalEntity implements Angerable, IBugVariants {
    /** Ticks an adult has to wait after breeding. */
    public static final int BREEDING_COOLDOWN_TICKS = 1200;

    protected static final TrackedData<Integer> ANGER =
            DataTracker.registerData(AngerableBugEntity.class, TrackedDataHandlerRegistry.INTEGER);
    private static final UniformIntProvider ANGER_TIME_RANGE = TimeHelper.betweenSeconds(20, 39);

    @Nullable
    private UUID angryAt;

    protected AngerableBugEntity(EntityType<? extends AnimalEntity> entityType, World world) {
        super(entityType, world);
    }

    /**
     * Whether this bug is currently allowed to fight at all (e.g. only OMEN beetles).
     * When false it never gets angry, never targets anything and never attacks.
     */
    public boolean canFight() {
        return true;
    }

    @Override
    public boolean isInvulnerableTo(DamageSource damageSource) {
        if (damageSource.isOf(DamageTypes.CACTUS) || damageSource.isOf(DamageTypes.SWEET_BERRY_BUSH)
                || damageSource.getAttacker() instanceof EnderDragonEntity || damageSource.isOf(DamageTypes.CRAMMING)) {
            return true;
        }
        return super.isInvulnerableTo(damageSource);
    }

    // =====================================================================
    // Anger
    // =====================================================================

    @Override
    public int getAngerTime() {
        return this.dataTracker.get(ANGER);
    }

    @Override
    public void setAngerTime(int angerTime) {
        this.dataTracker.set(ANGER, angerTime);
    }

    @Override
    public @Nullable UUID getAngryAt() {
        return this.angryAt;
    }

    @Override
    public void setAngryAt(@Nullable UUID angryAt) {
        this.angryAt = angryAt;
    }

    @Override
    public void chooseRandomAngerTime() {
        this.setAngerTime(ANGER_TIME_RANGE.get(this.random));
    }

    /** Without this, targets never turn into anger time and every "hasAngerTime()" goal stays dead. */
    @Override
    protected void mobTick() {
        super.mobTick();
        if (this.getWorld() instanceof ServerWorld serverWorld) {
            this.tickAngerLogic(serverWorld, false);
        }
    }

    // =====================================================================
    // Targeting
    // =====================================================================

    @Override
    public boolean canTarget(EntityType<?> type) {
        return type != EntityType.CREEPER;
    }

    @Override
    public boolean shouldAngerAt(LivingEntity entity) {
        if (!this.canFight() || !this.canTarget(entity) || this.isWearingGoldOrImmune(entity)) {
            return false;
        }
        return entity.getType() == EntityType.PLAYER && this.isUniversallyAngry(entity.getWorld())
                || entity.getUuid().equals(this.getAngryAt());
    }

    /** Registers revenge / player / monster targeting. Call at the end of initGoals(). */
    protected void addCombatTargetGoals() {
        this.targetSelector.add(1, new BugRevengeGoal(this).setGroupRevenge());
        this.targetSelector.add(2, new BugBiteTargetGoal(this));
        this.targetSelector.add(3, new ActiveTargetGoal<>(this, MobEntity.class, 10, false, false,
                entity -> this.canFight()
                        && entity instanceof Monster
                        && !(entity instanceof CreeperEntity)
                        && !this.isWearingGoldOrImmune(entity)));
        this.targetSelector.add(4, new ActiveTargetGoal<>(this, PlayerEntity.class, 5, false, false, this::shouldAngerAt));
    }

    // =====================================================================
    // Breeding
    // =====================================================================

    @Override
    public void breed(ServerWorld world, AnimalEntity other) {
        super.breed(world, other);
        this.setBreedingAge(BREEDING_COOLDOWN_TICKS);
        other.setBreedingAge(BREEDING_COOLDOWN_TICKS);
    }

    // =====================================================================
    // Data / NBT
    // =====================================================================

    @Override
    protected void initDataTracker(DataTracker.Builder builder) {
        super.initDataTracker(builder);
        builder.add(ANGER, 0);
    }

    @Override
    public void writeCustomDataToNbt(NbtCompound nbt) {
        super.writeCustomDataToNbt(nbt);
        this.writeAngerToNbt(nbt);
    }

    @Override
    public void readCustomDataFromNbt(NbtCompound nbt) {
        super.readCustomDataFromNbt(nbt);
        this.readAngerFromNbt(this.getWorld(), nbt);
    }

    // =====================================================================
    // Shared goals
    // =====================================================================

    /** Melee attack that only runs while the bug is angry and allowed to fight. */
    public static class BugMeleeGoal extends MeleeAttackGoal {
        protected final AngerableBugEntity bug;

        public BugMeleeGoal(AngerableBugEntity bug, double speed, boolean pauseWhenMobIdle) {
            super(bug, speed, pauseWhenMobIdle);
            this.bug = bug;
        }

        @Override
        public boolean canStart() {
            return this.bug.canFight() && this.bug.hasAngerTime() && super.canStart();
        }

        @Override
        public boolean shouldContinue() {
            return this.bug.canFight() && this.bug.hasAngerTime() && super.shouldContinue();
        }
    }

    /** Revenge on whoever hurt us (and nearby bugs of the same class), unless they are protected by gold. */
    public static class BugRevengeGoal extends RevengeGoal {
        private final AngerableBugEntity bug;

        public BugRevengeGoal(AngerableBugEntity bug) {
            super(bug);
            this.bug = bug;
        }

        @Override
        public boolean canStart() {
            LivingEntity attacker = this.bug.getAttacker();
            return this.bug.canFight()
                    && attacker != null
                    && !this.bug.isWearingGoldOrImmune(attacker)
                    && super.canStart();
        }

        @Override
        public boolean shouldContinue() {
            LivingEntity target = this.bug.getTarget();
            return this.bug.canFight()
                    && this.bug.hasAngerTime()
                    && target != null
                    && !this.bug.isWearingGoldOrImmune(target)
                    && super.shouldContinue();
        }

        @Override
        protected void setMobEntityTarget(MobEntity mob, LivingEntity target) {
            if (mob instanceof AngerableBugEntity other
                    && other.canFight()
                    && this.mob.canSee(target)
                    && !other.isWearingGoldOrImmune(target)) {
                mob.setTarget(target);
            }
        }
    }

    /** Keeps chasing a player while angry; drops them the moment they put gold on. */
    public static class BugBiteTargetGoal extends ActiveTargetGoal<PlayerEntity> {
        private final AngerableBugEntity bug;

        public BugBiteTargetGoal(AngerableBugEntity bug) {
            super(bug, PlayerEntity.class, 10, true, false, bug::shouldAngerAt);
            this.bug = bug;
        }

        @Override
        public boolean canStart() {
            return this.bug.canFight() && this.bug.hasAngerTime() && super.canStart();
        }

        @Override
        public boolean shouldContinue() {
            LivingEntity target = this.bug.getTarget();
            if (!this.bug.canFight() || !this.bug.hasAngerTime()
                    || target == null || this.bug.isWearingGoldOrImmune(target)) {
                return false; // the goal selector calls stop(), which clears the target
            }
            return super.shouldContinue();
        }
    }
}