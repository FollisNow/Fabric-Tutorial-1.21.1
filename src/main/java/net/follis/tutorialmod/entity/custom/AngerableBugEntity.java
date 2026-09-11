package net.follis.tutorialmod.entity.custom;

import net.follis.tutorialmod.util.IBugVariants;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.boss.dragon.EnderDragonEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageTypes;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.mob.Angerable;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.util.TimeHelper;
import net.minecraft.util.math.intprovider.UniformIntProvider;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public abstract class AngerableBugEntity extends AnimalEntity implements Angerable, IBugVariants {
    protected static final TrackedData<Integer> ANGER;

    @Nullable
    private UUID angryAt;
    private static final UniformIntProvider ANGER_TIME_RANGE;

    protected AngerableBugEntity(EntityType<? extends AnimalEntity> entityType, World world) {
        super(entityType, world);
    }

    public boolean isInvulnerableTo(DamageSource damageSource) {
        if (damageSource.isOf(DamageTypes.CACTUS) || damageSource.isOf(DamageTypes.SWEET_BERRY_BUSH)
                || damageSource.getAttacker() instanceof EnderDragonEntity || damageSource.isOf(DamageTypes.CRAMMING)) {
            return true;
        } else {
            return super.isInvulnerableTo(damageSource);
        }
    }

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

    // TARGETING
    @Override
    public boolean canTarget(EntityType<?> type) {
        return type != EntityType.CREEPER;
    }

    @Override
    public boolean shouldAngerAt(LivingEntity entity) {
        if (!this.canTarget(entity) || isWearingGoldOrImmune(entity)) {
            return false;
        } else {
            return entity.getType() == EntityType.PLAYER && this.isUniversallyAngry(entity.getWorld()) || entity.getUuid().equals(this.getAngryAt());
        }
    }

    @Override
    protected void initDataTracker(DataTracker.Builder builder) {
        super.initDataTracker(builder);
        builder.add(ANGER, 0);
    }

    static {
        ANGER = DataTracker.registerData(AngerableBugEntity.class, TrackedDataHandlerRegistry.INTEGER);
        ANGER_TIME_RANGE = TimeHelper.betweenSeconds(20, 39);
    }
}
