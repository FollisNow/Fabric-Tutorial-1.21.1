package net.follis.tutorialmod.entity.custom;

import net.follis.tutorialmod.particle.ModParticles;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public class LockingEntity extends Entity {
    public LockingEntity(EntityType<?> type, World world) {
        super(type, world);
    }

    private static final TrackedData<Integer> DATA_ID_TYPE_TARGETID =
            DataTracker.registerData(LockingEntity.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Integer> DATA_ID_TYPE_DURATION =
            DataTracker.registerData(LockingEntity.class, TrackedDataHandlerRegistry.INTEGER);

    @Override
    protected void initDataTracker(DataTracker.Builder builder) {
        builder.add(DATA_ID_TYPE_TARGETID, 0);
        builder.add(DATA_ID_TYPE_DURATION, 0);
    }

    private int getTargetId() {
        return this.dataTracker.get(DATA_ID_TYPE_TARGETID);
    }
    private int getDuration() {
        return this.dataTracker.get(DATA_ID_TYPE_DURATION);
    }

    public void setTarget(int targetId) {
        this.dataTracker.set(DATA_ID_TYPE_TARGETID, targetId);
    }
    public void setDuration(int duration) {
        this.dataTracker.set(DATA_ID_TYPE_DURATION, duration);
    }

    @Override
    public void tick() {
        super.tick();
        if (this.getWorld() instanceof ServerWorld serverWorld){
            Entity target = serverWorld.getEntityById(getTargetId());
            if (target != null && target.isAlive() && getDuration() > 0) {
                Vec3d diff = this.getBlockPos().toCenterPos().subtract(target.getPos());
                target.setVelocity(diff.multiply(0.1));
                addParticles(this, target, getDuration(), serverWorld);
                addParticles(this, target, getDuration() + random.nextInt(4), serverWorld);
                addParticles(this, target, getDuration() + random.nextInt(8), serverWorld);
                addParticles(this, target, getDuration() + random.nextInt(12), serverWorld);



                setDuration(getDuration() - 1);
            } else {
                this.kill();
            }
        }
    }


    private void addParticles(LockingEntity lockingEntity, Entity target, int duration, ServerWorld serverWorld) {
        Vec3d vec3d = Vec3Lerp(MathHelper.sin((float) duration / 4), target, lockingEntity.getBlockPos());
        serverWorld.spawnParticles(ModParticles.GOLDEN_CHAIN_PARTICLE,
                vec3d.x, vec3d.y,
                vec3d.z, 1, 0, 0, 0, 0);
    }

    public final Vec3d Vec3Lerp(float delta, Entity target, BlockPos selfPos) {
        double d = MathHelper.lerp(delta, selfPos.getX() + 0.5, target.getBlockPos().getX() + 0.5);
        double e = MathHelper.lerp((double)delta, selfPos.getY(), target.getBlockPos().getY() + target.getHeight() / 2);
        double f = MathHelper.lerp(delta, selfPos.getZ() + 0.5, target.getBlockPos().getZ() + 0.5);
        return new Vec3d(d, e, f);
    }

    @Override
    public void writeCustomDataToNbt(NbtCompound nbt) {
        nbt.putInt("Target", this.getTargetId());
        nbt.putInt("Duration", this.getDuration());
    }

    @Override
    public void readCustomDataFromNbt(NbtCompound nbt) {
        this.dataTracker.set(DATA_ID_TYPE_TARGETID, nbt.getInt("Target"));
        this.dataTracker.set(DATA_ID_TYPE_DURATION, nbt.getInt("Duration"));
    }
}
