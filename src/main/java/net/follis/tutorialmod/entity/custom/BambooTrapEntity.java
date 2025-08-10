package net.follis.tutorialmod.entity.custom;

import net.follis.tutorialmod.effect.ModEffects;
import net.follis.tutorialmod.item.ModItems;
import net.minecraft.block.Block;
import net.minecraft.block.piston.PistonBehavior;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.*;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.tag.DamageTypeTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.Box;
import net.minecraft.world.World;
import net.minecraft.world.event.GameEvent;

import java.util.List;
import java.util.function.Predicate;

public class BambooTrapEntity extends MobEntity {
    public final AnimationState openState = new AnimationState();
    public final AnimationState closedState = new AnimationState();
    private static final TrackedData<Boolean> IS_CLOSED = DataTracker.registerData(BambooTrapEntity.class, TrackedDataHandlerRegistry.BOOLEAN);
    private final StatusEffectInstance effect = new StatusEffectInstance(ModEffects.APPLY_BLEEDING, 1, 0); // 200 ticks, amplifier 0
    private long lastHitTime;
    private static final Predicate<Entity> BAMBOO_TRAP_PREDICATE;

    public BambooTrapEntity(EntityType<? extends MobEntity> entityType, World world) {
        super(entityType, world);
    }

    public static DefaultAttributeContainer.Builder setAttributes() {
        return LivingEntity.createLivingAttributes()
                .add(EntityAttributes.GENERIC_FOLLOW_RANGE, 1)
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 4)
                .add(EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE, 1.0f)
                .add(EntityAttributes.GENERIC_ARMOR, 0.0f)
                .add(EntityAttributes.GENERIC_ARMOR_TOUGHNESS, 15.0f);
    }

    @Override
    public boolean damage(DamageSource source, float amount) {
        if (this.isRemoved()) {
            return false;
        } else {
            World var4 = this.getWorld();
            if (var4 instanceof ServerWorld serverWorld) {
                if (source.isIn(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
                    this.kill();
                    return false;
                } else if (!this.isInvulnerableTo(source)) {
                    if (source.isIn(DamageTypeTags.IS_EXPLOSION)) {
                        this.onBreak(serverWorld, source);
                        this.kill();
                        return false;
                    } else if (source.isIn(DamageTypeTags.IGNITES_ARMOR_STANDS)) {
                        if (this.isOnFire()) {
                            this.updateHealth(serverWorld, source, 0.15F);
                        } else {
                            this.setOnFireFor(5.0F);
                        }

                        return false;
                    } else if (source.isIn(DamageTypeTags.BURNS_ARMOR_STANDS) && this.getHealth() > 0.5F) {
                        this.updateHealth(serverWorld, source, 4.0F);
                        return false;
                    } else {
                        boolean bl = source.isIn(DamageTypeTags.CAN_BREAK_ARMOR_STAND);
                        boolean bl2 = source.isIn(DamageTypeTags.ALWAYS_KILLS_ARMOR_STANDS);
                        if (!bl && !bl2) {
                            return false;
                        } else {
                            Entity var7 = source.getAttacker();
                            if (var7 instanceof PlayerEntity playerEntity) {
                                if (!playerEntity.getAbilities().allowModifyWorld) {
                                    return false;
                                }
                            }

                            if (source.isSourceCreativePlayer()) {
                                this.playBreakSound();
                                this.kill();
                                return true;
                            } else {
                                long l = serverWorld.getTime();
                                if (l - this.lastHitTime > 5L && !bl2) {
                                    serverWorld.sendEntityStatus(this, (byte)32);
                                    this.emitGameEvent(GameEvent.ENTITY_DAMAGE, source.getAttacker());
                                    this.lastHitTime = l;
                                } else {
                                    this.breakAndDropItem(serverWorld, source);
                                    this.kill();
                                }

                                return true;
                            }
                        }
                    }
                } else {
                    return false;
                }
            } else {
                return false;
            }
        }
    }

    @Override
    public void handleStatus(byte status) {
        if (status == 32) {
            if (this.getWorld().isClient) {
                this.getWorld().playSound(this.getX(), this.getY(), this.getZ(), SoundEvents.ENTITY_ARMOR_STAND_HIT, this.getSoundCategory(), 0.3F, 1.0F, false);
                this.lastHitTime = this.getWorld().getTime();
            }
        } else {
            super.handleStatus(status);
        }
    }
    public PistonBehavior getPistonBehavior() {
        return  PistonBehavior.IGNORE;
    }
    public boolean canAvoidTraps() {
        return true;
    }
    public boolean isMobOrPlayer() {
        return false;
    }
    public boolean isPushable() {
        return false;
    }
    protected void pushAway(Entity entity) {
    }
    protected void tickCramming() {
        for(Entity entity : this.getWorld().getOtherEntities(this, this.getBoundingBox(), BAMBOO_TRAP_PREDICATE)) {
            if (this.squaredDistanceTo(entity) <= 0.2) {
                entity.pushAwayFrom(this);
            }
        }

    }
    @Override
    protected void mobTick() {
        super.mobTick();
        Box box = Box.of(this.getPos(), 0.5,0.5,0.5);

        List<LivingEntity> nearbyEntities = this.getWorld().getEntitiesByClass(
                LivingEntity.class,
                box,
                entity -> !(entity instanceof BambooTrapEntity)
        );

        if (!nearbyEntities.isEmpty()) {
            for (LivingEntity entity : nearbyEntities) {
                entity.addStatusEffect(effect);
            }
            this.breakItem();
        }


    }

    private void onBreak(ServerWorld world, DamageSource damageSource) {
        this.playBreakSound();
        this.drop(world, damageSource);

    }

    private void breakAndDropItem(ServerWorld world, DamageSource damageSource) {
        ItemStack itemStack = new ItemStack(ModItems.BAMBOO_TRAP);
        itemStack.set(DataComponentTypes.CUSTOM_NAME, this.getCustomName());
        Block.dropStack(this.getWorld(), this.getBlockPos(), itemStack);
        this.onBreak(world, damageSource);
    }

    private void breakItem() {
        this.playBreakSound();
        this.kill();
    }

    private void playBreakSound() {
        this.getWorld().playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.BLOCK_CHAIN_BREAK, this.getSoundCategory(), 1.0F, 1.0F);
    }

    public void kill() {
        this.remove(RemovalReason.KILLED);
        this.emitGameEvent(GameEvent.ENTITY_DIE);
    }


    private void updateAnimationStates() { // using this for now but will be removed upon onCollision added
        if (this.isClosed()) {
            this.closedState.startIfNotRunning(this.age);
            this.openState.stop();
        } else {
            this.openState.startIfNotRunning(this.age);
            this.closedState.stop();
        }
    }

    @Override
    public void tick() {
        super.tick();

        if (this.getWorld().isClient()) {
            this.updateAnimationStates();
        }
    }

    private void updateHealth(ServerWorld world, DamageSource damageSource, float amount) {
        float f = this.getHealth();
        f -= amount;
        if (f <= 0.5F) {
            this.kill();
        } else {
            this.setHealth(f);
            this.emitGameEvent(GameEvent.ENTITY_DAMAGE, damageSource.getAttacker());
        }

    }

    private boolean isClosed() {
        return this.dataTracker.get(IS_CLOSED);
    }

    private void setState(boolean isClosed) {
        this.dataTracker.set(IS_CLOSED, isClosed);
    }

    @Override
    protected void initDataTracker(DataTracker.Builder builder) {
        super.initDataTracker(builder);
        builder.add(IS_CLOSED, false);
    }

    @Override
    public void writeCustomDataToNbt(NbtCompound nbt) {
        super.writeCustomDataToNbt(nbt);
        nbt.putBoolean("IsClosed", this.isClosed());
    }

    @Override
    public void readCustomDataFromNbt(NbtCompound nbt) {
        super.readCustomDataFromNbt(nbt);
        this.dataTracker.set(IS_CLOSED, nbt.getBoolean("IsClosed"));
    }

    static {
        BAMBOO_TRAP_PREDICATE = (entity) -> entity instanceof BambooTrapEntity;
    }
}
