package net.follis.tutorialmod.entity.custom;

import net.follis.tutorialmod.component.ModDataComponentTypes;
import net.follis.tutorialmod.entity.ModEntities;
import net.follis.tutorialmod.item.ModItems;
import net.follis.tutorialmod.item.custom.CaddisflyCocoonItem;
import net.follis.tutorialmod.util.IBugVariants;
import net.minecraft.entity.AnimationState;
import net.minecraft.entity.EntityData;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.ai.goal.AnimalMateGoal;
import net.minecraft.entity.ai.goal.SwimGoal;
import net.minecraft.entity.ai.goal.TemptGoal;
import net.minecraft.entity.ai.goal.WanderAroundFarGoal;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.passive.PassiveEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.LocalDifficulty;
import net.minecraft.world.ServerWorldAccess;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class LarvaeEntity extends AnimalEntity implements IBugVariants {
    public final AnimationState idleAnimationState = new AnimationState();
    private int idleAnimationTimeout = 0;

    private static final TrackedData<Integer> DATA_ID_TYPE_VARIANT = DataTracker.registerData(LarvaeEntity.class, TrackedDataHandlerRegistry.INTEGER);

    private static final int NO_MATERIAL = -1;
    private static final TrackedData<Integer> MATERIAL_SLOT_0 = DataTracker.registerData(LarvaeEntity.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Integer> MATERIAL_SLOT_1 = DataTracker.registerData(LarvaeEntity.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Integer> MATERIAL_SLOT_2 = DataTracker.registerData(LarvaeEntity.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Integer> MATERIAL_SLOT_3 = DataTracker.registerData(LarvaeEntity.class, TrackedDataHandlerRegistry.INTEGER);

    private static final List<TrackedData<Integer>> MATERIAL_SLOTS =
            List.of(MATERIAL_SLOT_0, MATERIAL_SLOT_1, MATERIAL_SLOT_2, MATERIAL_SLOT_3);

    public LarvaeEntity(EntityType<? extends AnimalEntity> entityType, World world) {
        super(entityType, world);
    }

    @Override
    protected void initGoals() {
        this.goalSelector.add(0, new SwimGoal(this));
        
        this.goalSelector.add(3, new AnimalMateGoal(this, 0.3D));
        this.goalSelector.add(4, new TemptGoal(this, 0.35D, this::foodSelector, false));
        this.goalSelector.add(5, new WanderAroundFarGoal(this, 0.3D));
    }

    private boolean foodSelector(ItemStack stack) {
        return stack.isIn(ItemTags.BEE_FOOD);
    }
    
    @Override
    public boolean isBreedingItem(ItemStack stack) {
        return foodSelector(stack);
    }
    
    public static DefaultAttributeContainer.Builder createAttributes() {
        return MobEntity.createMobAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 4)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.35)
                .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 1)
                .add(EntityAttributes.GENERIC_FOLLOW_RANGE, 20);
    }

    private void setupAnimationStates() {
        if (this.idleAnimationTimeout <= 0) {
            this.idleAnimationTimeout = 60;
            this.idleAnimationState.start(this.age);
        } else {
            --this.idleAnimationTimeout;
        }
    }
    @Override
    public void tick() {
        super.tick();

        if (this.getWorld().isClient()) {
            this.setupAnimationStates();
        }
    }
    public List<CocoonMaterial> getCocoonSegments() {
        List<CocoonMaterial> result = new ArrayList<>();
        for (TrackedData<Integer> slot : MATERIAL_SLOTS) {
            int ordinal = this.dataTracker.get(slot);
            result.add(ordinal == NO_MATERIAL ? null : CocoonMaterial.values()[ordinal]);
        }
        return result;
    }
    public void setCocoonSegment(int index, @Nullable CocoonMaterial material) {
        this.dataTracker.set(MATERIAL_SLOTS.get(index), material == null ? NO_MATERIAL : material.ordinal());
    }
    @Override
    public void onDeath(DamageSource damageSource) {
        super.onDeath(damageSource);
        if (!this.isBaby()) {
            List<CocoonMaterial> segments = this.getCocoonSegments();
            boolean hasAnyMaterial = segments.stream().anyMatch(m -> m != null);
            if (hasAnyMaterial) {
                ItemStack cocoonStack = new ItemStack(ModItems.CADDISFLY_COCOON);
                cocoonStack.set(ModDataComponentTypes.COCOON, CaddisflyCocoonItem.CocoonData.fromEntitySegments(segments));
                this.dropStack(cocoonStack);
            }
        }
    }
    @Override
    public @Nullable PassiveEntity createChild(ServerWorld world, PassiveEntity entity) {
        LarvaeEntity baby = ModEntities.LARVAE.create(world);
        if (baby != null && entity instanceof LarvaeEntity larvae) {
            baby.setVariant(this.getVariant());
        }
        return baby;
    }

    @Override
    protected void initDataTracker(DataTracker.Builder builder) {
        super.initDataTracker(builder);
        builder.add(DATA_ID_TYPE_VARIANT, 0);
        for (TrackedData<Integer> slot : MATERIAL_SLOTS) {
            builder.add(slot, NO_MATERIAL);
        }
    }

    public LarvaeVariant getVariant() {
        return LarvaeVariant.byId(this.getTypeVariant() & 255);
    }

    @Override
    public int getTypeVariant() {
        return this.dataTracker.get(DATA_ID_TYPE_VARIANT);
    }

    public void setVariant(LarvaeVariant variant) {
        this.dataTracker.set(DATA_ID_TYPE_VARIANT, variant.getId() & 255);
    }


    @Override
    protected void mobTick() {
        super.mobTick();
    }

    @Override
    public void writeCustomDataToNbt(NbtCompound nbt) {
        super.writeCustomDataToNbt(nbt);
        nbt.putInt("Variant", this.getTypeVariant());
        for (int i = 0; i < MATERIAL_SLOTS.size(); i++) {
            nbt.putInt("MaterialSlot" + i, this.dataTracker.get(MATERIAL_SLOTS.get(i)));
        }
    }

    @Override
    public void readCustomDataFromNbt(NbtCompound nbt) {
        super.readCustomDataFromNbt(nbt);
        this.dataTracker.set(DATA_ID_TYPE_VARIANT, nbt.getInt("Variant"));
        for (int i = 0; i < MATERIAL_SLOTS.size(); i++) {
            String key = "MaterialSlot" + i;
            if (nbt.contains(key)) {
                this.dataTracker.set(MATERIAL_SLOTS.get(i), nbt.getInt(key));
            }
        }
    }

    @Override
    public EntityData initialize(ServerWorldAccess world, LocalDifficulty difficulty, SpawnReason spawnReason,
                                 @Nullable EntityData entityData) {

        LarvaeVariant variant;
        variant = LarvaeVariant.REGULAR;

        this.setVariant(variant);
        return super.initialize(world, difficulty, spawnReason, entityData);
    }
}
