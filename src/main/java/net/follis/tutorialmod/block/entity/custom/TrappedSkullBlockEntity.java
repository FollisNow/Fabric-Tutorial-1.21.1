package net.follis.tutorialmod.block.entity.custom;

import net.minecraft.block.*;
import net.minecraft.block.entity.BlockEntity;
import net.follis.tutorialmod.block.entity.ModBlockEntities;
import net.minecraft.component.type.PotionContentsComponent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ArrowEntity;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket;
import net.minecraft.potion.Potions;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.*;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;
import net.minecraft.world.event.BlockPositionSource;
import net.minecraft.world.event.GameEvent;
import net.minecraft.world.event.PositionSource;
import net.minecraft.world.event.Vibrations;
import net.minecraft.world.event.listener.GameEventListener;
import org.jetbrains.annotations.Nullable;

public class TrappedSkullBlockEntity extends BlockEntity
        implements GameEventListener.Holder<Vibrations.VibrationListener>, Vibrations {

    private static final double TRACK_RANGE = 12.0;

    private final VibrationListener listener = new VibrationListener(this);
    private final Vibrations.ListenerData listenerData = new Vibrations.ListenerData();
    private final Vibrations.Callback callback = new Callback();

    private float targetYaw;
    private float yaw;          // smoothed, used by the renderer (client)
    private float prevYaw;
    private int shootCooldown;

    private static final float WALL_MAX_TURN = 75f;

    public TrappedSkullBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.TRAPPED_SKULL_BE, pos, state);
        this.targetYaw = this.yaw = this.prevYaw = baseYaw(state);
    }

    private static float baseYaw(BlockState state) {
        if (state.getBlock() instanceof WallSkullBlock) {
            return state.get(WallSkullBlock.FACING).getOpposite().asRotation();
        }
        return RotationPropertyHelper.toDegrees(state.get(SkullBlock.ROTATION));
    }

    private void lookAt(Vec3d target) {
        if (getCachedState().getBlock() instanceof WallSkullBlock) return;   // wall heads stay fixed

        Vec3d d = target.subtract(Vec3d.ofCenter(pos));
        float newYaw = (float) (MathHelper.atan2(-d.x, d.z) * 180.0 / Math.PI) + 180f;

        if (Math.abs(MathHelper.subtractAngles(newYaw, targetYaw)) > 2f) {
            targetYaw = newYaw;
            markDirty();
            world.updateListeners(pos, getCachedState(), getCachedState(), Block.NOTIFY_LISTENERS);
        }
    }

    public static void tick(World world, BlockPos pos, BlockState state, TrappedSkullBlockEntity be) {
        if (world.isClient) {
            be.prevYaw = be.yaw;
            be.yaw = MathHelper.lerpAngleDegrees(0.25f, be.yaw, be.targetYaw);
            return;
        }
        Vibrations.Ticker.tick(world, be.listenerData, be.callback);
        if (be.shootCooldown > 0) be.shootCooldown--;

        if (world.getTime() % 4 == 0 && !(state.getBlock() instanceof WallSkullBlock)) {
            LivingEntity target = be.findClosestTarget(world);
            if (target != null) be.lookAt(target.getEyePos());
        }
    }
    @Nullable
    private LivingEntity findClosestTarget(World world) {
        Vec3d center = headCenter();
        double size = TRACK_RANGE * 2;
        Box box = Box.of(center, size, size, size);

        LivingEntity best = null;
        double bestDist = TRACK_RANGE * TRACK_RANGE;
        for (LivingEntity e : world.getEntitiesByClass(LivingEntity.class, box,
                e -> e.isAlive() && !e.isSpectator() && !(e instanceof ArmorStandEntity))) {
            double d = e.squaredDistanceTo(center);
            // Distance first, so the raycast only runs for entities that would actually be closer
            if (d < bestDist && hasLineOfSight(world, e.getEyePos())) {
                best = e;
                bestDist = d;
            }
        }
        return best;
    }
    private static final double AIM_LIFT = 0.05;   // upward tilt, as a fraction of horizontal speed

    private void shootAhead(ServerWorld world) {
        if (shootCooldown > 0) return;
        shootCooldown = 40;

        float rad = targetYaw * MathHelper.RADIANS_PER_DEGREE;
        Vec3d dir = new Vec3d(MathHelper.sin(rad), 0, -MathHelper.cos(rad));

        // Reuse the head-centre helper and nudge up, so it leaves from the face rather than the base
        Vec3d from = headCenter().add(0, 0.1, 0).add(dir.multiply(0.4));

        ItemStack ammo = PotionContentsComponent.createStack(Items.TIPPED_ARROW, Potions.POISON);
        /*
        ItemStack ammo = new ItemStack(Items.TIPPED_ARROW);
        ammo.set(DataComponentTypes.POTION_CONTENTS, new PotionContentsComponent(
        Optional.empty(), Optional.empty(),
        List.of(new StatusEffectInstance(StatusEffects.SLOWNESS, 100, 1))));
         */

        ArrowEntity arrow = new ArrowEntity(world, from.x, from.y, from.z, ammo, null);
        arrow.setVelocity(dir.x, AIM_LIFT, dir.z, 1.6f, 0f);
        arrow.pickupType = PersistentProjectileEntity.PickupPermission.DISALLOWED;
        world.spawnEntity(arrow);
        world.playSound(null, pos, SoundEvents.ENTITY_SKELETON_SHOOT, SoundCategory.BLOCKS, 1f, 1f);
    }
    private static final double ARROW_SPEED = 1.6;   // blocks per tick, matches the setVelocity speed
    private static final double LEAD_FACTOR = 1.1;   // >1 compensates for arrow drag; tune in game

    private Vec3d predictTarget(Entity target) {
        Vec3d start = headCenter();
        Vec3d vel = target.getPos().subtract(target.prevX, target.prevY, target.prevZ);   // blocks per tick

        Vec3d predicted = target.getEyePos();
        for (int i = 0; i < 3; i++) {   // refine: the further it leads, the longer the flight
            double ticks = predicted.distanceTo(start) / ARROW_SPEED;
            predicted = target.getEyePos().add(vel.x * ticks * LEAD_FACTOR, 0, vel.z * ticks * LEAD_FACTOR);
        }
        return predicted;
    }
    // --- client sync ---
    @Override
    public BlockEntityUpdateS2CPacket toUpdatePacket() { return BlockEntityUpdateS2CPacket.create(this); }
    @Override
    public NbtCompound toInitialChunkDataNbt(RegistryWrapper.WrapperLookup l) { return createNbt(l); }
    @Override
    protected void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup l) {
        super.writeNbt(nbt, l);
        nbt.putFloat("TargetYaw", targetYaw);
        // also persist listenerData like SculkSensorBlockEntity does, if you want in-flight vibrations to survive reloads
    }
    @Override
    protected void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup l) {
        super.readNbt(nbt, l);
        targetYaw = nbt.getFloat("TargetYaw");
    }

    public float getRenderYaw(float tickDelta) { return MathHelper.lerpAngleDegrees(tickDelta, prevYaw, yaw); }

    // --- Vibrations / listener plumbing ---
    @Override public VibrationListener getEventListener() { return listener; }
    @Override public Vibrations.ListenerData getVibrationListenerData() { return listenerData; }
    @Override public Vibrations.Callback getVibrationCallback() { return callback; }

    private class Callback implements Vibrations.Callback {
        @Override public PositionSource getPositionSource() { return new BlockPositionSource(pos); }
        @Override public int getRange() { return 12; }

        @Override
        public void accept(ServerWorld world, BlockPos source, RegistryEntry<GameEvent> event,
                           @Nullable Entity sourceEntity, @Nullable Entity entity, float distance) {
            Vec3d aim = sourceEntity != null ? predictTarget(sourceEntity) : Vec3d.ofCenter(source);

            // Wall heads can't turn, so only fire when the target is about to cross their line
            if (getCachedState().getBlock() instanceof WallSkullBlock && !inFiringLane(aim)) return;

            lookAt(aim);
            shootAhead(world);
        }
        @Override
        public boolean accepts(ServerWorld world, BlockPos eventPos, RegistryEntry<GameEvent> event, GameEvent.Emitter emitter) {
            // Ignore events at the skull's own position (e.g. it being placed or broken)
            if (eventPos.equals(pos)) return false;

            // Ignore arrows so the head doesn't trigger itself
            if (emitter.sourceEntity() instanceof PersistentProjectileEntity) return false;

            // Only react while it's able to shoot
            if (shootCooldown > 0) return false;

            // Only react to sources it can see
            Entity source = emitter.sourceEntity();
            Vec3d from = source != null ? source.getEyePos() : Vec3d.ofCenter(eventPos);
            if (getCachedState().getBlock() instanceof WallSkullBlock) {
                Vec3d toSource = from.subtract(headCenter());
                float yawToSource = (float) (MathHelper.atan2(toSource.x, -toSource.z) * 180.0 / Math.PI);
                if (Math.abs(MathHelper.subtractAngles(yawToSource, targetYaw)) > 60f) return false;
            }

            return hasLineOfSight(world, from);
        }
    }
    private Vec3d headCenter() {
        double y = pos.getY() + (getCachedState().getBlock() instanceof WallSkullBlock ? 0.5 : 0.25);
        return new Vec3d(pos.getX() + 0.5, y, pos.getZ() + 0.5);
    }

    private boolean hasLineOfSight(World world, Vec3d from) {
        BlockHitResult hit = world.raycast(new RaycastContext(
                from, headCenter(), RaycastContext.ShapeType.VISUAL, RaycastContext.FluidHandling.NONE,
                ShapeContext.absent()));
        return hit.getType() == HitResult.Type.MISS || hit.getBlockPos().equals(pos);
    }
    private static final double LANE_HALF_WIDTH = 0.6;   // how far off its firing line a target can be and still get shot

    private boolean inFiringLane(Vec3d point) {
        float rad = targetYaw * MathHelper.RADIANS_PER_DEGREE;
        double fx = MathHelper.sin(rad);
        double fz = -MathHelper.cos(rad);

        Vec3d rel = point.subtract(headCenter());
        double along = rel.x * fx + rel.z * fz;           // distance in front of the head
        double lateral = Math.abs(rel.x * fz - rel.z * fx); // distance off the firing line
        return along > 0.5 && lateral < LANE_HALF_WIDTH;
    }
}
