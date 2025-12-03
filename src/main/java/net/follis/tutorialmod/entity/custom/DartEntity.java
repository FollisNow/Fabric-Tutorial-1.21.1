package net.follis.tutorialmod.entity.custom;

import net.follis.tutorialmod.entity.ModEntities;
import net.follis.tutorialmod.item.ModItems;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

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

    protected void onHit(LivingEntity target) {
        super.onHit(target);
        Entity entity = this.getEffectCause();
        target.addStatusEffect(new StatusEffectInstance(StatusEffects.BLINDNESS, 4 * 20), entity);
        target.addStatusEffect(new StatusEffectInstance(StatusEffects.POISON, 30, 4), entity);
    }

}
