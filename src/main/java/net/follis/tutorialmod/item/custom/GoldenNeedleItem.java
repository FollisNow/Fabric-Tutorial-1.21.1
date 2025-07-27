package net.follis.tutorialmod.item.custom;

import net.follis.tutorialmod.component.ModDataComponentTypes;
import net.follis.tutorialmod.entity.ModEntities;
import net.follis.tutorialmod.entity.custom.LockingEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.SwordItem;
import net.minecraft.item.ToolMaterial;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.world.World;

import java.util.List;

public class GoldenNeedleItem extends SwordItem {
    public GoldenNeedleItem(ToolMaterial toolMaterial, Settings settings) {
        super(toolMaterial, settings);
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        if (world instanceof ServerWorld serverWorld) {
            HitResult hit = user.raycast(20, 0, false);
            if (hit.getType() == HitResult.Type.BLOCK) {
                if (getCurrentStacks(user) > 0 && serverWorld.getEntityById(user.getMainHandStack().getOrDefault(ModDataComponentTypes.ENTITY_ID_CODEC, 0)) instanceof  LivingEntity livingEntity) {
                    BlockHitResult blockHit = (BlockHitResult) hit;
                    BlockPos pos = blockHit.getBlockPos();
                    LockingEntity lockingEntity;
                    List<LockingEntity> entities = world.getEntitiesByType(ModEntities.LOCK, new Box(pos), lock -> true);

                    if(entities.isEmpty()) {
                        lockingEntity = ModEntities.LOCK.spawn(serverWorld, pos, SpawnReason.TRIGGERED);
                    } else {
                        lockingEntity = entities.getFirst();
                    }
                    assert lockingEntity != null;
                    lockingEntity.setTarget(livingEntity.getId());
                    lockingEntity.setDuration(getCurrentStacks(user));
                    resetStacks(user);
                }
            }
        }

        return super.use(world, user, hand);
    }

    @Override
    public void postDamageEntity(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (target != null && !target.isDead()) {
            updateNeedle(target.getId(), attacker);
        }
        super.postDamageEntity(stack, target, attacker);
    }

    private void updateNeedle(Integer entityId, LivingEntity user) {
        int lastEntityId = user.getMainHandStack().getOrDefault(ModDataComponentTypes.ENTITY_ID_CODEC, 0);
        if (lastEntityId == entityId){
            int stackCount = user.getMainHandStack().getOrDefault(ModDataComponentTypes.GOLDEN_NEEDLE_STACKS_CODEC, 0);
            user.getMainHandStack().set(ModDataComponentTypes.GOLDEN_NEEDLE_STACKS_CODEC, stackCount + 40);
        } else {
            user.getMainHandStack().set(ModDataComponentTypes.ENTITY_ID_CODEC, entityId);
            user.getMainHandStack().set(ModDataComponentTypes.GOLDEN_NEEDLE_STACKS_CODEC, 0);
        }
    }
    private int getCurrentStacks(LivingEntity user) {
        return user.getMainHandStack().getOrDefault(ModDataComponentTypes.GOLDEN_NEEDLE_STACKS_CODEC, 0);
    }
    private void resetStacks(LivingEntity user) {
        user.getMainHandStack().set(ModDataComponentTypes.GOLDEN_NEEDLE_STACKS_CODEC, 0);
    }
}