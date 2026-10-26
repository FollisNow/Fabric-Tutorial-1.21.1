package net.follis.goldenscarabmod.item.custom;

import net.follis.goldenscarabmod.pattern.GoldenSwordsPattern;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

public class GoldenSwordsStaffItem extends Item {
    private static final double RANGE = 32.0;
    /** The whole attack lasts about 4 seconds; this keeps casts from piling up. Set to 0 to remove it. */
    private static final int COOLDOWN_TICKS = 120;

    public GoldenSwordsStaffItem(Settings settings) {
        super(settings);
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);
        LivingEntity target = findTarget(user);

        if (target == null) {
            if (!world.isClient) {
                world.playSound(null, user.getBlockPos(), SoundEvents.BLOCK_DISPENSER_FAIL, SoundCategory.PLAYERS, 0.8F, 1.4F);
            }
            return TypedActionResult.fail(stack);
        }
        if (world instanceof ServerWorld serverWorld) {
            GoldenSwordsPattern.summon(serverWorld, user, target);
            if (COOLDOWN_TICKS > 0) user.getItemCooldownManager().set(this, COOLDOWN_TICKS);
        }
        return TypedActionResult.success(stack, world.isClient());
    }

    /** The living entity under the crosshair, if no block is in the way. */
    @Nullable
    private static LivingEntity findTarget(PlayerEntity user) {
        Vec3d start = user.getEyePos();
        Vec3d look = user.getRotationVec(1.0F);
        Vec3d end = start.add(look.multiply(RANGE));
        Box area = user.getBoundingBox().stretch(look.multiply(RANGE)).expand(1.0);

        EntityHitResult hit = ProjectileUtil.raycast(user, start, end, area,
                e -> e instanceof LivingEntity && e.isAlive() && !e.isSpectator() && e != user, RANGE * RANGE);
        if (hit == null) return null;

        BlockHitResult wall = user.getWorld().raycast(new RaycastContext(start, hit.getPos(),
                RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, user));
        return wall.getType() == HitResult.Type.MISS ? (LivingEntity) hit.getEntity() : null;
    }
}