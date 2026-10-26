package net.follis.goldenscarabmod.entity.custom;

import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.particle.BlockStateParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.util.UUID;

import static net.follis.goldenscarabmod.pattern.GoldenSwordsPattern.SWORD_COUNT;

/** A sword of light: appears above its target, hovers and follows it, then falls and stabs. */
public class GoldenSwordProjectileEntity extends ProjectileEntity {
    // what the client needs to know in order to draw it
    private static final TrackedData<Integer> APPEAR_AT = DataTracker.registerData(GoldenSwordProjectileEntity.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Integer> FALL_AT = DataTracker.registerData(GoldenSwordProjectileEntity.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Boolean> STUCK = DataTracker.registerData(GoldenSwordProjectileEntity.class, TrackedDataHandlerRegistry.BOOLEAN);
    private static final TrackedData<Integer> TARGET_ID = DataTracker.registerData(GoldenSwordProjectileEntity.class, TrackedDataHandlerRegistry.INTEGER);
    /** The locked fall direction. All zeros until the sword starts to fall. */
    private static final TrackedData<Vector3f> FALL_DIR = DataTracker.registerData(GoldenSwordProjectileEntity.class, TrackedDataHandlerRegistry.VECTOR3F);

    // ---- tuning ----
    private static final double FOLLOW_FRACTION = 0.35;   // share of the remaining distance covered each tick while following
    private static final double MAX_FOLLOW_STEP = 1.2;    // blocks per tick, at most
    private static final double FALL_START_SPEED = 0.5;   // blocks per tick when the fall begins...
    private static final double FALL_ACCELERATION = 0.3;  // ...gaining this much every tick...
    private static final double FALL_MAX_SPEED = 2.6;      // ...up to this
    private static final float DAMAGE = 1.0F;              // per sword; five swords landing together make 15
    /** How long a sword stays stabbed in the ground before it disappears. */
    public static final int STUCK_TICKS = 40;

    private int ticks;        // ticks lived, counted on both sides
    private int stuckTicks;   // ticks spent stabbed in the ground, counted on both sides

    // server only
    @Nullable private UUID targetId;
    private Vec3d offset = Vec3d.ZERO;   // where this sword hovers, relative to the top of the target
    @Nullable private Vec3d fallDirection;
    private int fallTicks;

    public GoldenSwordProjectileEntity(EntityType<? extends GoldenSwordProjectileEntity> type, World world) {
        super(type, world);
        this.setNoGravity(true);
    }

    @Override
    protected void initDataTracker(DataTracker.Builder builder) {
        builder.add(APPEAR_AT, 0);
        builder.add(FALL_AT, 40);
        builder.add(STUCK, false);
        builder.add(TARGET_ID, -1);
        builder.add(FALL_DIR, new Vector3f());
    }

    /** Sets the sword up above its target. appearAt and fallAt are ticks counted from the moment it is created. */
    public void setup(LivingEntity caster, LivingEntity target, Vec3d offset, int appearAt, int fallAt) {
        this.setOwner(caster);
        this.targetId = target.getUuid();
        this.offset = offset;
        this.dataTracker.set(TARGET_ID, target.getId());
        this.dataTracker.set(APPEAR_AT, appearAt);
        this.dataTracker.set(FALL_AT, fallAt);
        this.setPosition(hoverPoint(target));
    }

    // ---- for the renderer ----
    public int getTicks() { return ticks; }
    public int getAppearAt() { return dataTracker.get(APPEAR_AT); }
    public int getFallAt() { return dataTracker.get(FALL_AT); }
    public boolean isStuck() { return dataTracker.get(STUCK); }
    public int getStuckTicks() { return stuckTicks; }

    /**
     * The direction the blade points in: the locked fall direction once the sword has started to fall,
     * otherwise toward the body of its target, which is where it will fall.
     */
    public Vec3d getAim(float tickDelta) {
        Vector3f locked = dataTracker.get(FALL_DIR);
        if (locked.lengthSquared() > 0.25F) return new Vec3d(locked.x(), locked.y(), locked.z());

        Entity target = getWorld().getEntityById(dataTracker.get(TARGET_ID));
        if (target instanceof LivingEntity living) {
            Vec3d toTarget = living.getLerpedPos(tickDelta).add(0, living.getHeight() * 0.5, 0)
                    .subtract(getLerpedPos(tickDelta));
            if (toTarget.lengthSquared() > 1.0E-4) return toTarget.normalize();
        }
        return new Vec3d(0, -1, 0);
    }

    @Override
    public void tick() {
        super.tick();
        ticks++;

        if (isStuck()) {
            stuckTicks++;
            if (!getWorld().isClient && stuckTicks >= STUCK_TICKS) discard();
            return;
        }
        // the server moves the sword and the position is sent to the clients; the client only draws it
        if (!(getWorld() instanceof ServerWorld world)) return;

        int appearAt = getAppearAt();
        SoundCategory category = SoundCategory.HOSTILE;
        if (getOwner() instanceof PlayerEntity) {
            category = SoundCategory.PLAYERS;
        }
        if (ticks < getFallAt()) {
            followTarget(world);
            if (ticks == appearAt) {
                world.spawnParticles(ParticleTypes.END_ROD, getX(), getY() + 0.7, getZ(), 8, 0.2, 0.3, 0.2, 0.02);
                world.playSound(null, getX(), getY(), getZ(), SoundEvents.ENTITY_EVOKER_CAST_SPELL,
                        category, 2F, 0.8F + 0.4F * appearAt);

            } else if (ticks > appearAt && ticks % 6 == 0) {
                world.spawnParticles(ParticleTypes.END_ROD, getX(), getY() + 0.7, getZ(), 1, 0.1, 0.3, 0.1, 0.0);
            }
        } else {
            fall(world);
        }
    }

    // ---- hovering ----

    private Vec3d hoverPoint(LivingEntity target) {
        return new Vec3d(target.getX() + offset.x, target.getBoundingBox().maxY + offset.y, target.getZ() + offset.z);
    }

    private void followTarget(ServerWorld world) {
        Entity target = targetId == null ? null : world.getEntity(targetId);
        if (!(target instanceof LivingEntity living) || !living.isAlive()) {
            // the target is gone: swords still to appear vanish, the others fall where they are
            if (ticks < getAppearAt()) discard();
            else dataTracker.set(FALL_AT, ticks);
            return;
        }
        Vec3d step = hoverPoint(living).subtract(getPos()).multiply(FOLLOW_FRACTION);
        if (step.length() > MAX_FOLLOW_STEP) step = step.normalize().multiply(MAX_FOLLOW_STEP);
        setPosition(getPos().add(step));
    }

    // ---- falling ----

    private void fall(ServerWorld world) {
        if (fallDirection == null) {
            fallDirection = lockOnDirection(world);
            dataTracker.set(FALL_DIR, fallDirection.toVector3f());
            fallTicks = 0;
            world.playSound(null, getX(), getY(), getZ(), SoundEvents.ITEM_TRIDENT_RIPTIDE_1,
                    SoundCategory.NEUTRAL, 1.0F, 1.6F);
        }
        double speed = Math.min(FALL_MAX_SPEED, FALL_START_SPEED + FALL_ACCELERATION * fallTicks++);
        Vec3d travel = fallDirection.multiply(speed);
        Vec3d start = getPos();   // the entity's position is the tip of the blade
        Vec3d end = start.add(travel);

        BlockHitResult blockHit = world.raycast(new RaycastContext(start, end,
                RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, this));
        EntityHitResult entityHit = ProjectileUtil.getEntityCollision(world, this, start, end,
                getBoundingBox().stretch(travel).expand(0.3), this::canHit);

        double blockDistance = blockHit.getType() == HitResult.Type.MISS
                ? Double.MAX_VALUE : blockHit.getPos().squaredDistanceTo(start);
        double entityDistance = entityHit == null
                ? Double.MAX_VALUE : entityHit.getPos().squaredDistanceTo(start);

        if (entityDistance < blockDistance) {
            stabEntity(world, entityHit);
        } else if (blockDistance < Double.MAX_VALUE) {
            stabBlock(world, blockHit);
        } else {
            setPosition(end);
            world.spawnParticles(ParticleTypes.END_ROD, getX(), getY() + 0.5, getZ(), 1, 0.05, 0.2, 0.05, 0.0);
        }
    }

    /** The swords aim at where the target is at the moment they start to fall. */
    private Vec3d lockOnDirection(ServerWorld world) {
        Entity target = targetId == null ? null : world.getEntity(targetId);
        if (target instanceof LivingEntity living && living.isAlive()) {
            Vec3d toTarget = living.getPos().add(0, living.getHeight() * 0.5, 0).subtract(getPos());
            if (toTarget.lengthSquared() > 1.0E-4) return toTarget.normalize();
        }
        return new Vec3d(0, -1, 0);
    }

    private void stabEntity(ServerWorld world, EntityHitResult hit) {
        Entity victim = hit.getEntity();
        victim.timeUntilRegen = 0;   // every sword counts, even when several land on the same tick
        victim.damage(getDamageSources().thrown(this, getOwner()), DAMAGE);

        Vec3d p = hit.getPos();
        world.spawnParticles(ParticleTypes.CRIT, p.x, p.y, p.z, 10, 0.2, 0.2, 0.2, 0.3);
        world.playSound(null, p.x, p.y, p.z, SoundEvents.ITEM_TRIDENT_HIT, SoundCategory.NEUTRAL, 1.0F, 1.4F);
        discard();
    }

    private void stabBlock(ServerWorld world, BlockHitResult hit) {
        Vec3d p = hit.getPos();
        setPosition(p.add(fallDirection.multiply(0.25)));   // sink the tip a little into the block
        dataTracker.set(STUCK, true);

        BlockState state = world.getBlockState(hit.getBlockPos());
        world.spawnParticles(new BlockStateParticleEffect(ParticleTypes.BLOCK, state), p.x, p.y, p.z, 12, 0.2, 0.1, 0.2, 0.1);
        world.spawnParticles(ParticleTypes.END_ROD, p.x, p.y + 0.2, p.z, 6, 0.1, 0.1, 0.1, 0.1);
        world.playSound(null, p.x, p.y, p.z, SoundEvents.ITEM_TRIDENT_HIT_GROUND, SoundCategory.NEUTRAL, 1.0F, 1.4F);
    }

    /** Swords only hurt living things, never their caster and never each other. */
    @Override
    protected boolean canHit(Entity entity) {
        return super.canHit(entity) && entity instanceof LivingEntity && entity != getOwner();
    }

    @Override
    public boolean isAttackable() {
        return false;
    }
}