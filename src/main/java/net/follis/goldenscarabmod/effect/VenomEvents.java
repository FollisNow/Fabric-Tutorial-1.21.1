package net.follis.goldenscarabmod.effect;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.block.AbstractChestBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.Oxidizable;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.AxeItem;
import net.minecraft.item.HoneycombItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.world.World;

public final class VenomEvents {
    private VenomEvents() {}

    public static void register() {
        // MINER: client-side callback stops the mining from starting, BEFORE is the authoritative server check
        AttackBlockCallback.EVENT.register((player, world, hand, pos, direction) ->
                player.hasStatusEffect(VenomEffects.MINER) ? deny(player, world) : ActionResult.PASS);
        PlayerBlockBreakEvents.BEFORE.register((world, player, pos, state, be) ->
                !player.hasStatusEffect(VenomEffects.MINER));

        // PLENTY / SCRAPE / SHEAF
        UseBlockCallback.EVENT.register((player, world, hand, hit) -> {
            BlockState state = world.getBlockState(hit.getBlockPos());
            ItemStack stack = player.getStackInHand(hand);

            if (player.hasStatusEffect(VenomEffects.PLENTY) && state.getBlock() instanceof AbstractChestBlock<?>) {
                // vanilla doesn't open the chest when sneaking with something in hand, so don't block that case
                boolean holding = !player.getMainHandStack().isEmpty() || !player.getOffHandStack().isEmpty();
                if (!(player.shouldCancelInteraction() && holding)) return deny(player, world);
            }
            if (player.hasStatusEffect(VenomEffects.SHEAF) && stack.isOf(Items.BONE_MEAL)) {
                return deny(player, world);
            }
            if (player.hasStatusEffect(VenomEffects.SCRAPE) && stack.getItem() instanceof AxeItem
                    && (Oxidizable.getDecreasedOxidationState(state).isPresent()
                    || HoneycombItem.WAXED_TO_UNWAXED_BLOCKS.get().containsKey(state.getBlock()))) {
                return deny(player, world);
            }
            return ActionResult.PASS;
        });

        // MOURNER: the lethal blow is cancelled, the victim is left on 1 HP
        ServerLivingEntityEvents.ALLOW_DEATH.register((entity, source, amount) -> {
            if (source.getAttacker() instanceof LivingEntity attacker
                    && attacker.hasStatusEffect(VenomEffects.MOURNER)) {
                entity.setHealth(1.0F);
                return false;
            }
            return true;
        });
    }

    private static ActionResult deny(PlayerEntity player, World world) {
        if (world.isClient) {
            player.sendMessage(Text.translatable("message.goldenscarabmod.venom_blocked"), true);
        }
        return ActionResult.FAIL;
    }
}