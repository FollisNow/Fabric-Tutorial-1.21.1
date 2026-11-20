package net.follis.goldenscarabmod.entity.custom;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.BlockStateParticleEffect;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.Box;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.util.UUID;
import java.util.function.Predicate;

/** A sword that bursts up out of the floor, hurts whatever stands above it, then sinks back. */
public class GroundSwordEntity extends Entity {
    private static final TrackedData<Integer> ERUPT_AT = DataTracker.registerData(GroundSwordEntity.class, TrackedDataHandlerRegistry.INTEGER);

    public static final int RISE_TICKS = 3;
    public static final int UP_TICKS = 14;
    public static final int SINK_TICKS = 8;
    private static final double LAUNCH = 0.55;   // upward kick given to whatever is hit
    private static final DustParticleEffect GOLD_DUST = new DustParticleEffect(new Vector3f(1.0F, 0.78F, 0.2F), 1.2F);

    private int ticks;   // ticks lived, counted on both sides

    // server only
    @Nullable private UUID ownerId;
    private Predicate<LivingEntity> targets = e -> true;
    private float damage = 5.0F;
    private int warnAt;

    public GroundSwordEntity(EntityType<? extends GroundSwordEntity> type, World world) {
        super(type, world);
        this.noClip = true;
    }

    @Override
    protected void initDataTracker(DataTracker.Builder builder) {
        builder.add(ERUPT_AT, 20);
    }

    @Override
    protected void readCustomDataFromNbt(NbtCompound nbt) {}

    @Override
    protected void writeCustomDataToNbt(NbtCompound nbt) {}

    /** warnAt: the tick the warning starts. eruptAt: the tick the sword bursts out. Both count from creation. */
    public void configure(LivingEntity owner, Predicate<LivingEntity> targets, float damage, int warnAt, int eruptAt) {
        this.ownerId = owner.getUuid();
        this.targets = targets;
        this.damage = damage;
        this.warnAt = warnAt;
        this.dataTracker.set(ERUPT_AT, eruptAt);
    }

    // ---- for the renderer ----
    public int getTicks() { return ticks; }
    public int getEruptAt() { return dataTracker.get(ERUPT_AT); }

    @Override
    public void tick() {
        super.tick();
        ticks++;
        if (!(getWorld() instanceof ServerWorld world)) return;

        int eruptAt = getEruptAt();
        if (ticks >= warnAt && ticks < eruptAt && ticks % 2 == 0) {
            // the ground glows where a sword is about to come up
            world.spawnParticles(GOLD_DUST, getX(), getY() + 0.1, getZ(), 3, 0.35, 0.02, 0.35, 0.0);
            world.spawnParticles(ParticleTypes.CRIT, getX(), getY() + 0.2, getZ(), 1, 0.3, 0.05, 0.3, 0.02);
        }
        if (ticks == eruptAt) erupt(world);
        if (ticks >= eruptAt + RISE_TICKS + UP_TICKS + SINK_TICKS) discard();
    }

    private void erupt(ServerWorld world) {
        world.spawnParticles(new BlockStateParticleEffect(ParticleTypes.BLOCK, world.getBlockState(getBlockPos().down())),
                getX(), getY() + 0.1, getZ(), 10, 0.3, 0.1, 0.3, 0.15);
        world.spawnParticles(ParticleTypes.END_ROD, getX(), getY() + 0.5, getZ(), 4, 0.15, 0.4, 0.15, 0.05);
        world.playSound(null, getX(), getY(), getZ(), SoundEvents.ITEM_TRIDENT_HIT_GROUND, SoundCategory.HOSTILE, 0.7F, 1.2F);

        Entity owner = ownerId == null ? null : world.getEntity(ownerId);
        DamageSource source = world.getDamageSources().thrown(this, owner);
        Box area = new Box(getX() - 0.75, getY() - 0.3, getZ() - 0.75, getX() + 0.75, getY() + 3.0, getZ() + 0.75);

        for (LivingEntity e : world.getEntitiesByClass(LivingEntity.class, area,
                t -> t != owner && t.isAlive() && !t.isSpectator() && targets.test(t))) {
            e.timeUntilRegen = 0;
            e.damage(source, damage);
            e.setVelocity(e.getVelocity().add(0, LAUNCH, 0));
            e.velocityModified = true;   // tells the clients about the kick
        }
    }

    @Override
    public boolean isAttackable() {
        return false;
    }
}