package net.follis.tutorialmod.item.custom;

import net.follis.tutorialmod.component.ModDataComponentTypes;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.mob.PhantomEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.stat.Stats;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.world.event.GameEvent;

public class PhantasmalCursedEyeItem extends AbstractCursedEye{
    public static final int TIME_TO_CURSE = 100;
    private int curseStacks = 0;

    public PhantasmalCursedEyeItem(Settings settings) {
        super(settings);
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        if (!world.isClient()) {
            user.getStackInHand(hand).set(ModDataComponentTypes.IS_USING, true);
            user.getStackInHand(hand).set(ModDataComponentTypes.IS_TRIGGERED, false);
            this.curseStacks = 0;
        }
        user.incrementStat(Stats.USED.getOrCreateStat(this));
        return super.use(world, user, hand);
    }

    @Override
    public void onStoppedUsing(ItemStack stack, World world, LivingEntity user, int remainingUseTicks) {
        if (!world.isClient()) {
            stack.set(ModDataComponentTypes.IS_USING, false);
            this.curseStacks = 0;
            stack.set(ModDataComponentTypes.IS_TRIGGERED, false);
        }
    }

    @Override
    public void inventoryTick(ItemStack stack, World world, Entity entity, int slot, boolean selected) {
        if (entity instanceof ServerPlayerEntity player) {
            if (stack.getOrDefault(ModDataComponentTypes.IS_USING, false)) {
                if (this.curseStacks < TIME_TO_CURSE) {
                    if (tryToCurse(player)) {
                        this.curseStacks++;
                    } else {
                        this.curseStacks = this.curseStacks > 0 ? this.curseStacks-1: 0;
                    }
                } else {
                    stack.set(ModDataComponentTypes.IS_TRIGGERED, true);
                    if (!tryToCurse(player))
                        this.curseStacks = 0;
                }
            }

            if (stack.getOrDefault(ModDataComponentTypes.IS_TRIGGERED, false)) {
                LivingEntity entityHit = pseudoRaycast(player);
                if (entityHit != null && !player.getItemCooldownManager().isCoolingDown(this)) {
                    applyEffects(entityHit);
                    player.getItemCooldownManager().set(this, 100);
                    player.incrementStat(Stats.USED.getOrCreateStat(this));
                    world.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.BLOCK_BEACON_ACTIVATE, SoundCategory.PLAYERS, 0.6f, 1.6f / (world.getRandom().nextFloat() * 0.4f + 0.4f));
                } else {
                    stack.set(ModDataComponentTypes.IS_TRIGGERED, false);
                }
            }
        }
        super.inventoryTick(stack, world, entity, slot, selected);
    }

    protected void applyEffects(LivingEntity entityHit) {
        Vec3d pos = entityHit.getPos();
        World world = entityHit.getWorld();

        if (world instanceof ServerWorld serverWorld) {
            for (int i = 0; i < 3; i++) {
            Entity phantomEntity = new PhantomEntity(EntityType.PHANTOM, world) {
                @Override
                public boolean shouldDropXp() {
                    return false;
                }
                @Override
                protected boolean shouldDropLoot() {
                    return false;
                }
            };
                phantomEntity.refreshPositionAndAngles(pos.add(0, 1, 0).addRandom(world.getRandom(), 0.3f), 0, 0);
                serverWorld.spawnEntity(phantomEntity);
                serverWorld.emitGameEvent(GameEvent.ENTITY_ACTION, pos, GameEvent.Emitter.of(phantomEntity, serverWorld.getBlockState(BlockPos.ofFloored(pos))));
            }
            serverWorld.playSoundAtBlockCenter(BlockPos.ofFloored(pos), SoundEvents.ENTITY_ENDER_EYE_DEATH, SoundCategory.HOSTILE, 1.0F, 1.0F, true);

        }
    }
}
