package net.follis.tutorialmod.mixin;


import net.follis.tutorialmod.entity.custom.SpiderlingEntity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.FlyingItemEntity;
import net.minecraft.entity.projectile.thrown.PotionEntity;
import net.minecraft.entity.projectile.thrown.ThrownItemEntity;
import net.minecraft.util.math.Box;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PotionEntity.class)
public abstract class PotionEntityMixin extends ThrownItemEntity implements FlyingItemEntity {


    public PotionEntityMixin(EntityType<? extends ThrownItemEntity> entityType, World world) {
        super(entityType, world);
    }

    @Inject(at = @At("TAIL"), method = "applyWater")
    public void applyWaterMixin(CallbackInfo info) {
        Box box = this.getBoundingBox().expand(4.0, 2.0, 4.0);

        for (SpiderlingEntity spiderlingEntity : this.getWorld().getNonSpectatingEntities(SpiderlingEntity.class, box)) {
            spiderlingEntity.washEntity();
        }
    }
}
