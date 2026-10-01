package net.follis.tutorialmod.entity.custom;

import net.follis.tutorialmod.entity.ModEntities;
import net.follis.tutorialmod.item.ModItems;
import net.follis.tutorialmod.item.custom.BugItem;
import net.follis.tutorialmod.util.IBugVariants;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.tag.EntityTypeTags;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import static net.follis.tutorialmod.item.ModItems.GENERIC_ARTHROPOD_FIGURINE;

public class DartEntity extends PersistentProjectileEntity {
    public DartEntity(EntityType<? extends PersistentProjectileEntity> entityType, World world) {
        super(entityType, world);
        this.setDamage(0.5);
        this.pickupType = PickupPermission.ALLOWED;
    }

    public DartEntity(World world, double x, double y, double z, ItemStack stack, @Nullable ItemStack shotFrom) {
        super(ModEntities.DART, x, y, z, world, stack, shotFrom);
        this.setDamage(0.5);
        this.pickupType = PickupPermission.ALLOWED;
    }

    @Override
    protected ItemStack getDefaultItemStack() {
        return new ItemStack(ModItems.DART);
    }

    @Override
    protected void onHit(LivingEntity target) {
        super.onHit(target);

        if (!this.getWorld().isClient) {
            Item bugItem = IBugVariants.bugItems.get(target.getType());
            if (bugItem == null && target.getType().isIn(EntityTypeTags.ARTHROPOD)) {
                bugItem = GENERIC_ARTHROPOD_FIGURINE;
            }

            if (bugItem != null) {
                target.setHealth(target.getMaxHealth()); // undo the dart's own damage before snapshotting NBT

                ItemStack figurine = BugItem.createFigurine(bugItem, target);
                Vec3d pos = target.getPos();
                World world = this.getWorld();
                target.discard();

                if (!figurine.isEmpty()) {
                    ItemEntity itemEntity = new ItemEntity(world, pos.getX(), pos.getY(), pos.getZ(), figurine);
                    itemEntity.setToDefaultPickupDelay();
                    world.spawnEntity(itemEntity);
                }
                return;
            }
        }

        Entity entity = this.getEffectCause();
        target.addStatusEffect(new StatusEffectInstance(StatusEffects.BLINDNESS, 4 * 20), entity);
        target.addStatusEffect(new StatusEffectInstance(StatusEffects.POISON, 30, 4), entity);
    }
}
