package net.follis.tutorialmod.item.custom;

import net.follis.tutorialmod.entity.custom.DartEntity;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ProjectileItem;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Position;
import net.minecraft.world.World;

public class DartItem extends Item implements ProjectileItem {
    public DartItem(Item.Settings settings) {
        super(settings);
    }

    public ProjectileEntity createEntity(World world, Position pos, ItemStack stack, Direction direction) {
        DartEntity dartEntity = new DartEntity(world, pos.getX(), pos.getY(), pos.getZ(), stack.copyWithCount(1), null);
        dartEntity.pickupType = PersistentProjectileEntity.PickupPermission.ALLOWED;
        return dartEntity;
    }
}
