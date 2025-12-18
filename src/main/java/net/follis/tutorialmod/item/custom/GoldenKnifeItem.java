package net.follis.tutorialmod.item.custom;

import net.follis.tutorialmod.entity.ModEntities;
import net.follis.tutorialmod.entity.custom.SpiderlingEntity;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.SpiderEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.SwordItem;
import net.minecraft.item.ToolMaterial;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.stat.Stats;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;

import java.util.List;

public class GoldenKnifeItem extends SwordItem {
    public GoldenKnifeItem(ToolMaterial toolMaterial, Settings settings) {
        super(toolMaterial, settings);
    }

    final TextColor darkGold = TextColor.fromRgb(0xBC7F04); // Custom gold-like color
    @Override
    public Text getName() {
        return Text.translatable(this.getTranslationKey()).formatted(Formatting.GOLD);
    }
    @Override
    public Text getName(ItemStack stack) {
        return Text.translatable(this.getTranslationKey(stack)).formatted(Formatting.GOLD);
    }

    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
        tooltip.add(Text.translatable("tooltip.tutorialmod.golden_knife"));
        if(Screen.hasShiftDown()) {
            tooltip.add(Text.translatable("tooltip.tutorialmod.golden_knife.shift_down").withColor(darkGold.getRgb()));
        } else {
            tooltip.add(Text.translatable("tooltip.tutorialmod.shift_info"));
        }
        super.appendTooltip(stack, context, tooltip, type);
    }

    @Override
    public ActionResult useOnEntity(ItemStack stack, PlayerEntity user, LivingEntity entity, Hand hand) {
        if (entity.getWorld() instanceof ServerWorld serverWorld && entity instanceof SpiderEntity spider && !user.getItemCooldownManager().isCoolingDown(this)) {
            spider.damage(spider.getDamageSources().playerAttack(user), Float.MAX_VALUE);
            stack.setDamage(stack.getDamage()+1);
            user.getItemCooldownManager().set(this, 20);
            user.incrementStat(Stats.USED.getOrCreateStat(this));
            for (int i = 0; i < 3; i++) {
                SpiderlingEntity spiderling = new SpiderlingEntity(ModEntities.SPIDERLING, serverWorld);
                spiderling.setBaby(true);
                spiderling.refreshPositionAndAngles(entity.getPos(), 0, 0);
                serverWorld.spawnEntity(spiderling);
            }
        }
        return super.useOnEntity(stack, user, entity, hand);
    }
}
