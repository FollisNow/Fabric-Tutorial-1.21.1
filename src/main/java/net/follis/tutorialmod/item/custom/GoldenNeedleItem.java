package net.follis.tutorialmod.item.custom;

import net.fabricmc.fabric.api.item.v1.EnchantingContext;
import net.follis.tutorialmod.component.ModDataComponentTypes;
import net.follis.tutorialmod.entity.custom.GoldenNeedleProjectileEntity;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.item.*;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Position;
import net.minecraft.world.World;

import java.util.List;

public class GoldenNeedleItem extends SwordItem implements ProjectileItem {
    public GoldenNeedleItem(ToolMaterial toolMaterial, SwordItem.Settings settings) {
        super(toolMaterial, settings);
    }
    @Override
    public Text getName() {
        return Text.translatable(this.getTranslationKey()).formatted(Formatting.GOLD);
    }
    @Override
    public Text getName(ItemStack stack) {
        return Text.translatable(this.getTranslationKey(stack)).formatted(Formatting.GOLD);
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        if (world instanceof ServerWorld serverWorld) {
            ItemStack stack = user.getStackInHand(hand);
            stack.damage(1, user, LivingEntity.getSlotForHand(user.getActiveHand()));

            world.playSound(null, user.getX(), user.getY(), user.getZ(), SoundEvents.ENTITY_SNOWBALL_THROW, SoundCategory.NEUTRAL, 0.5f, 0.4f / (world.getRandom().nextFloat() * 0.4f + 0.8f));
            GoldenNeedleProjectileEntity goldenNeedleProjectile = new GoldenNeedleProjectileEntity(world, user, stack);
            goldenNeedleProjectile.setVelocity(user, user.getPitch(), user.getYaw(), 0.0f, 3f, 0f);

            if (serverWorld.getEntityById(user.getMainHandStack().getOrDefault(ModDataComponentTypes.ENTITY_ID_CODEC, 0)) instanceof  LivingEntity livingEntity) {
                goldenNeedleProjectile.setTarget(livingEntity.getId());
                goldenNeedleProjectile.setDuration(getCurrentStacks(user));
                goldenNeedleProjectile.setVelocity(goldenNeedleProjectile.getVelocity().multiply(0.4));
            }
            resetStacks(user);
            resetTarget(user);
            user.getInventory().removeOne(stack);
            world.spawnEntity(goldenNeedleProjectile);
        }
        return super.use(world, user, hand);
    }

    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
        super.appendTooltip(stack, context, tooltip, type);
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
            user.getMainHandStack().set(ModDataComponentTypes.GOLDEN_NEEDLE_STACKS_CODEC, stackCount + 60);
        } else {
            user.getMainHandStack().set(ModDataComponentTypes.ENTITY_ID_CODEC, entityId);
            user.getMainHandStack().set(ModDataComponentTypes.GOLDEN_NEEDLE_STACKS_CODEC, 60);
        }
    }
    private int getCurrentStacks(LivingEntity user) {
        return user.getMainHandStack().getOrDefault(ModDataComponentTypes.GOLDEN_NEEDLE_STACKS_CODEC, 0);
    }
    private void resetStacks(LivingEntity user) {
        user.getMainHandStack().set(ModDataComponentTypes.GOLDEN_NEEDLE_STACKS_CODEC, 0);
    }
    private void resetTarget(LivingEntity user) {
        user.getMainHandStack().set(ModDataComponentTypes.ENTITY_ID_CODEC, 0);
    }

    @Override
    public boolean canBeEnchantedWith(ItemStack stack, RegistryEntry<Enchantment> enchantment, EnchantingContext context) {
        return super.canBeEnchantedWith(stack, enchantment, context);
    }

    @Override
    public ProjectileEntity createEntity(World world, Position pos, ItemStack stack, Direction direction) {
        GoldenNeedleProjectileEntity goldenNeedleProjectile = new GoldenNeedleProjectileEntity(world, pos.getX(), pos.getY(), pos.getZ(), stack.copyWithCount(1));
        goldenNeedleProjectile.pickupType = PersistentProjectileEntity.PickupPermission.ALLOWED;
        return goldenNeedleProjectile;
    }
}