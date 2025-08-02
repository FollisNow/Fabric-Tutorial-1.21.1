package net.follis.tutorialmod.entity.custom;

import net.follis.tutorialmod.component.ModDataComponentTypes;
import net.follis.tutorialmod.entity.ModEntities;
import net.follis.tutorialmod.item.ModItems;
import net.follis.tutorialmod.particle.ModParticles;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.MovementType;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

public class GoldenNeedleProjectileEntity extends PersistentProjectileEntity {
    private static final TrackedData<Byte> LOYALTY;
    private static final TrackedData<Boolean> ENCHANTED;
    private static final TrackedData<Integer> DATA_ID_TYPE_TARGETID =
            DataTracker.registerData(GoldenNeedleProjectileEntity.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Integer> DATA_ID_TYPE_DURATION =
            DataTracker.registerData(GoldenNeedleProjectileEntity.class, TrackedDataHandlerRegistry.INTEGER);
    private final double groundVelocityFactor = 0.005;
    private boolean dealtDamage;
    public int returnTimer;

    public GoldenNeedleProjectileEntity(EntityType<? extends PersistentProjectileEntity> entityType, World world) {
        super(entityType, world);

    }

    public GoldenNeedleProjectileEntity(World world, LivingEntity owner, ItemStack stack) {
        super(ModEntities.GOLDEN_NEEDLE, owner, world, stack, null);
        this.dataTracker.set(LOYALTY, this.getLoyalty(stack));
        this.dataTracker.set(ENCHANTED, stack.hasGlint());
    }

    public GoldenNeedleProjectileEntity(World world, double x, double y, double z, ItemStack stack) {
        super(ModEntities.GOLDEN_NEEDLE, x, y, z, world, stack, stack);
        this.dataTracker.set(LOYALTY, this.getLoyalty(stack));
        this.dataTracker.set(ENCHANTED, stack.hasGlint());
    }


    @Override
    protected void initDataTracker(DataTracker.Builder builder) {
        super.initDataTracker(builder);
        builder.add(DATA_ID_TYPE_TARGETID, 0);
        builder.add(DATA_ID_TYPE_DURATION, 0);
        builder.add(LOYALTY, (byte)0);
        builder.add(ENCHANTED, false);
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

    protected boolean tryPickup(PlayerEntity player) {
        this.getItemStack().set(ModDataComponentTypes.ENTITY_ID_CODEC, 0);
        this.getItemStack().set(ModDataComponentTypes.GOLDEN_NEEDLE_STACKS_CODEC, 0);
        setTarget(0);
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
        if (getDuration() <= 0 && this.inGroundTime > 20) {
            this.dealtDamage = true;
            this.getItemStack().set(ModDataComponentTypes.ENTITY_ID_CODEC, 0);
            this.getItemStack().set(ModDataComponentTypes.GOLDEN_NEEDLE_STACKS_CODEC, 0);
            setDuration(0);
            setTarget(0);
        }

        Entity entity = this.getOwner();
        int i = this.dataTracker.get(LOYALTY);
        if (i > 0 && this.dealtDamage && entity != null && getDuration() <= 0 && (this.isNoClip() || this.inGroundTime > 20)) {
            if (!this.isOwnerAlive()) {
                if (!this.getWorld().isClient && this.pickupType == PickupPermission.ALLOWED) {
                    this.getItemStack().set(ModDataComponentTypes.ENTITY_ID_CODEC, 0);
                    this.getItemStack().set(ModDataComponentTypes.GOLDEN_NEEDLE_STACKS_CODEC, 0);
                    setTarget(0);
                    setDuration(0);
                    this.dropStack(this.asItemStack(), 0.1F);
                }
                this.discard();
            } else {
                // LOYALTY LOGIC
                this.getItemStack().set(ModDataComponentTypes.ENTITY_ID_CODEC, 0);
                this.getItemStack().set(ModDataComponentTypes.GOLDEN_NEEDLE_STACKS_CODEC, 0);
                setTarget(0);
                setDuration(0);
                this.setNoClip(true);

                Vec3d vec3d = entity.getEyePos().subtract(this.getPos());
                this.setPos(this.getX(), this.getY() + vec3d.y * 0.015 * (double)i, this.getZ());
                if (this.getWorld().isClient) {
                    this.lastRenderY = this.getY();
                }

                double d = 0.05 * (double)i;
                this.setVelocity(this.getVelocity().multiply(0.95).add(vec3d.normalize().multiply(d)));
                if (this.returnTimer == 0) {
                    this.playSound(SoundEvents.ITEM_TRIDENT_RETURN, 7.0F, 3.0F);
                }
                ++this.returnTimer;
            }
        }



        if (getTargetId() != 0){
            World world = this.getWorld();
            Entity target = world.getEntityById(getTargetId());
            if (target != null && target.isAlive() && getDuration() > 0) {
                Vec3d diff = this.getPos().subtract(target.getPos()).multiply(0.1);
                Vec3d appliedForce = diff;

                if (this.inGround) {
                    if (this.getPos().distanceTo(target.getPos()) < 2.2) {
                        appliedForce = diff.multiply(this.groundVelocityFactor);
                        if (!world.isClient && this.getPos().distanceTo(target.getPos()) >= 2.0 && this.inGroundTime > 20) {
                            setDuration(0);
                            world.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.BLOCK_CHAIN_BREAK, SoundCategory.PLAYERS, 0.5f, 0.4f / (world.getRandom().nextFloat() * 0.4f + 0.8f));
                        }
                    }
                }


                if (target instanceof PlayerEntity playerEntity) {
                    playerEntity.addVelocity(appliedForce);
                    if (playerEntity.isOnGround()) {
                        playerEntity.move(MovementType.SELF, appliedForce);
                    }
                } else {
                    target.setVelocity(appliedForce);
                }
                if (world instanceof ServerWorld serverWorld){
                    addParticles(this, target, serverWorld);
                    addParticles(this, target, serverWorld);
                    addParticles(this, target, serverWorld);
                    addParticles(this, target, serverWorld);
                    setDuration(getDuration() - 1);
                }
            }
        }
        super.tick();
    }

    public boolean shouldRender(double cameraX, double cameraY, double cameraZ) {
        return true;
    }

    private void addParticles(GoldenNeedleProjectileEntity needleProjectile, Entity target, ServerWorld serverWorld) {
        Vec3d vec3d = Vec3Lerp((float) serverWorld.random.nextInt(20) / 20, target, needleProjectile.getPos());
        serverWorld.spawnParticles(ModParticles.GOLDEN_CHAIN_PARTICLE,
                vec3d.x, vec3d.y,
                vec3d.z, 1, 0, 0, 0, 0);
    }

    public final Vec3d Vec3Lerp(float delta, Entity target, Vec3d selfPos) {
        double d = MathHelper.lerp(delta, selfPos.x, target.getPos().x);
        double e = MathHelper.lerp(delta, selfPos.y, target.getPos().y + target.getHeight() / 2);
        double f = MathHelper.lerp(delta, selfPos.z, target.getPos().z);
        return new Vec3d(d, e, f);
    }

    @Override
    public void onPlayerCollision(PlayerEntity player) {
        if (this.isOwner(player) || this.getOwner() == null) {
            super.onPlayerCollision(player);
        }
    }

    @Override
    @Nullable
    protected EntityHitResult getEntityCollision(Vec3d currentPosition, Vec3d nextPosition) {
        return this.dealtDamage ? null : super.getEntityCollision(currentPosition, nextPosition);
    }

    @Override
    protected void onEntityHit(EntityHitResult entityHitResult) {
        if (entityHitResult.getEntity().getId() != getTargetId()){
            setTarget(entityHitResult.getEntity().getId());
            setDuration(90);
            setVelocity(this.getVelocity().multiply(0.4));
        } else {
            setDuration(getDuration() + 90);
        }
    }

    private byte getLoyalty(ItemStack stack) {
        World var3 = this.getWorld();
        if (var3 instanceof ServerWorld serverWorld) {
            return (byte)MathHelper.clamp(EnchantmentHelper.getTridentReturnAcceleration(serverWorld, stack, this), 0, 127);
        } else {
            return 0;
        }
    }

    @Override
    protected ItemStack getDefaultItemStack() {
        return new ItemStack(ModItems.GOLDEN_NEEDLE);
    }

    @Override
    public ItemStack getWeaponStack() {
        return this.getItemStack();
    }

    @Override
    public void writeCustomDataToNbt(NbtCompound nbt) {
        super.writeCustomDataToNbt(nbt);
        nbt.putInt("Target", this.getTargetId());
        nbt.putInt("Duration", this.getDuration());
        nbt.putBoolean("DealtDamage", this.dealtDamage);
    }

    @Override
    public void readCustomDataFromNbt(NbtCompound nbt) {
        super.readCustomDataFromNbt(nbt);
        this.dealtDamage = nbt.getBoolean("DealtDamage");
        this.dataTracker.set(LOYALTY, this.getLoyalty(this.getItemStack()));
        this.dataTracker.set(DATA_ID_TYPE_TARGETID, nbt.getInt("Target"));
        this.dataTracker.set(DATA_ID_TYPE_DURATION, nbt.getInt("Duration"));
    }
    static {
        LOYALTY = DataTracker.registerData(GoldenNeedleProjectileEntity.class, TrackedDataHandlerRegistry.BYTE);
        ENCHANTED = DataTracker.registerData(GoldenNeedleProjectileEntity.class, TrackedDataHandlerRegistry.BOOLEAN);
    }
}
