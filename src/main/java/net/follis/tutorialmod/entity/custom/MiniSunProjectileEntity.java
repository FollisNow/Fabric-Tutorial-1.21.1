package net.follis.tutorialmod.entity.custom;

import net.follis.tutorialmod.entity.ModEntities;
import net.follis.tutorialmod.item.ModItems;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.entity.projectile.SmallFireballEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.function.Predicate;

public class MiniSunProjectileEntity extends PersistentProjectileEntity {
    private float rotation;
    private int fireballCount = 0; // Track the number of fireballs shot in the current burst
    private final int maxFireballCount = 3;
    private final int burstCooldown = 5; // Cooldown between fireball shot in the same burst
    private final int resetBurstCooldown = 20; // Cooldown after three fireballs

    private static final TrackedData<Integer> DATA_ID_TYPE_DURATION =
            DataTracker.registerData(MiniSunProjectileEntity.class, TrackedDataHandlerRegistry.INTEGER);

    private static final TrackedData<Integer> DATA_ID_TYPE_COOLDOWN =
            DataTracker.registerData(MiniSunProjectileEntity.class, TrackedDataHandlerRegistry.INTEGER);

    public MiniSunProjectileEntity(EntityType<? extends PersistentProjectileEntity> entityType, World world) {
        super(entityType, world);

    }
    public MiniSunProjectileEntity(World world, PlayerEntity player) {
        super(ModEntities.MINI_SUN, player, world, new ItemStack(ModItems.MINI_SUN), null);
    }

    @Override
    protected double getGravity() {
        return 0;
    }

    @Override
    protected void initDataTracker(DataTracker.Builder builder) {
        super.initDataTracker(builder);
        builder.add(DATA_ID_TYPE_DURATION, 0);
        builder.add(DATA_ID_TYPE_COOLDOWN, 0);
    }
    public float getRenderingRotation() {
        rotation += 0.5f;
        if(rotation >= 360) {
            rotation = 0;
        }
        return rotation;
    }
    private int getDuration() {
        return this.dataTracker.get(DATA_ID_TYPE_DURATION);
    }
    public void setDuration(int duration) {
        this.dataTracker.set(DATA_ID_TYPE_DURATION, duration);
    }

    private int getCooldown() {
        return this.dataTracker.get(DATA_ID_TYPE_COOLDOWN);
    }
    public void setCooldown(int duration) {
        this.dataTracker.set(DATA_ID_TYPE_COOLDOWN, duration);
    }

    protected boolean tryPickup(PlayerEntity player) {
        setDuration(0);
        return super.tryPickup(player) || this.isOwner(player) && player.getInventory().insertStack(this.asItemStack()) && player.getPos().isInRange(this.getPos(), 0.5);
    }

    private boolean isOwnerAlive() {
        Entity entity = this.getOwner();
        if (entity != null && entity.isAlive()) {
            return !(entity instanceof ServerPlayerEntity) || !entity.isSpectator();
        } else {
            return false;
        }
    }


    @Override
    public void tick() {
        World world = this.getWorld();
        if (world instanceof ServerWorld serverWorld) {

            if (getDuration() <= 0) {
                setDuration(0);
//                this.dropStack(this.asItemStack(), 0.1F);
                this.discard();
            }

            if (fireballCount < maxFireballCount) { // Fire up to 3 fireballs in burst
                if (getCooldown() <= 0) {
                    Entity owner = this.getOwner();
                    if (owner instanceof LivingEntity livingOwner && this.isOwnerAlive()) {
                        Entity closestHostile = findClosestHostile(serverWorld, owner, 16.0);
                        if (closestHostile != null) {
                            launchFireball(serverWorld, livingOwner, closestHostile);
                            fireballCount++; // Increment count after firing
                            setCooldown(burstCooldown); // Reset cooldown for next fireball in burst
                        }
                    }
                } else {
                    setCooldown(getCooldown() - 1); // Decrease cooldown
                }
            } else {
                // After firing 3 fireballs, reset the count and apply a longer cooldown
                if (getCooldown() <= 0) {
                    fireballCount = 0; // Reset the burst count
                    setCooldown(resetBurstCooldown); // Set longer cooldown after burst
                } else {
                    setCooldown(getCooldown() - 1); // Decrease cooldown
                }
            }

            Entity owner = this.getOwner();
            if (owner != null) {
                if (!this.isOwnerAlive()) {
                    setDuration(0);
//                    this.dropStack(this.asItemStack(), 0.1F);
                    this.discard();
                }
                addParticles(this, serverWorld);
                addParticles(this, serverWorld);
                addParticles(this, serverWorld);
                addParticles(this, serverWorld);
                setDuration(getDuration() - 1);
                Vec3d vec3d = Vec3DiffLerp((float) 0.1, owner, this.getPos());
                if (vec3d.length() > 0.02f)
                    this.setVelocity(vec3d);

            } else {
//                this.dropStack(this.asItemStack(), 0.1F);
                this.discard();
            }
        }
        super.tick();
    }

    private void launchFireball(ServerWorld world, LivingEntity owner, Entity target) {
        Vec3d direction = target.getPos().subtract(this.getPos()).normalize();

        SmallFireballEntity fireball = new SmallFireballEntity(world, owner, direction.multiply(2));
        fireball.setPosition(this.getX(), this.getY(), this.getZ());
        world.spawnEntity(fireball);

        // Play fireball launch sound
        world.playSound(null, this.getX(), this.getY(), this.getZ(),
                SoundEvents.ENTITY_BLAZE_SHOOT, SoundCategory.PLAYERS, 1.0F, 1.0F); // Adjust volume and pitch as needed
    }

    private Entity findClosestHostile(ServerWorld world, Entity owner, double radius) {
        double closestDistanceSquared = radius * radius; // Ensure using squared distance for comparison
        Entity closestHostile = null;

        // Define a predicate to filter hostile entities
        Predicate<Entity> predicate = entity -> entity instanceof HostileEntity && entity.isAlive();

        // Get all HostileEntity instances within the specified radius
        for (HostileEntity mob : world.getEntitiesByClass(HostileEntity.class, owner.getBoundingBox().expand(radius), predicate)) {
            double distanceSquared = owner.squaredDistanceTo(mob); // Calculate squared distance to the mob
            if (distanceSquared < closestDistanceSquared) {
                closestDistanceSquared = distanceSquared;
                closestHostile = mob; // Update the closest hostile entity
            }
        }
        return closestHostile; // Return the closest hostile entity found
    }

    public final Vec3d Vec3DiffLerp(float delta, Entity owner, Vec3d selfPos) {
        return owner.getPos().add(0, 3.5, 0).subtract(selfPos).multiply(delta);
    }

    private void addParticles(MiniSunProjectileEntity miniSunProjectile, ServerWorld serverWorld) {
        Vec3d vec3d = miniSunProjectile.getPos();
        serverWorld.spawnParticles(ParticleTypes.FLAME,
                vec3d.x, vec3d.y,
                vec3d.z, 1, 0.25, 0.25, 0.25, 0.05);
    }

    public boolean shouldRender(double cameraX, double cameraY, double cameraZ) {
        return true;
    }


    @Override
    protected ItemStack getDefaultItemStack() {
        return new ItemStack(ModItems.MINI_SUN);
    }

    @Override
    public void writeCustomDataToNbt(NbtCompound nbt) {
        super.writeCustomDataToNbt(nbt);
        nbt.putInt("Duration", this.getDuration());
    }

    @Override
    public void readCustomDataFromNbt(NbtCompound nbt) {
        super.readCustomDataFromNbt(nbt);
        this.dataTracker.set(DATA_ID_TYPE_DURATION, nbt.getInt("Duration"));
        this.dataTracker.set(DATA_ID_TYPE_COOLDOWN, nbt.getInt("Cooldown"));
    }
}
