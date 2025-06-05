package net.follis.tutorialmod;

import net.fabricmc.api.ModInitializer;

import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.registry.FuelRegistry;
import net.follis.tutorialmod.block.ModBlocks;
import net.follis.tutorialmod.component.ModDataComponentTypes;
import net.follis.tutorialmod.item.ModItemGroups;
import net.follis.tutorialmod.item.ModItems;
import net.follis.tutorialmod.util.HammerUsageEvent;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.passive.SheepEntity;
import net.minecraft.item.Items;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// Very important comment
public class TutorialMod implements ModInitializer {
	public static final String MOD_ID = "tutorialmod";

	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		ModItemGroups.registerItemGroups();

		ModItems.registerModItems();
		ModBlocks.registerModBlocks();

		ModDataComponentTypes.registerDataComponentTypes();

		FuelRegistry.INSTANCE.add(ModItems.STARLIGHT_ASHES, 600);

		PlayerBlockBreakEvents.BEFORE.register(new HammerUsageEvent());

		AttackEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
			if(entity instanceof SheepEntity sheepEntity && !world.isClient) {
				if(player.getMainHandStack().getItem() == Items.END_ROD) {
					player.sendMessage(Text.literal("The Player just hit a sheep with an END ROD?!?!"));
					player.getMainHandStack().decrement(1);
					sheepEntity.addStatusEffect(new StatusEffectInstance(StatusEffects.HEALTH_BOOST, 9, 9));
				}
				return ActionResult.PASS;
			}
			return ActionResult.PASS;
		});
	}
}
// Step one if custom => create new class in custom, else add block/item into ModBlocks/ModItems
// Step two Add the corresponding entry to ModItemGroups for creative tabs
// Step three Add datagen for tags if block: mineable? etc., if new tags for items
// Step four Add datagen for LootTable if blocks (whether dropping itself or acting as ore)
// Step five Add datagen for Model e.g. Item models or blockstates
// Step six Add datagen for Recipes
// Step seven Run datagen

// If you wanna make it a golem mod, you'll need a crafting station for the golems, the golems item and entity with the
// ai and a way to order them (seals on chests or blocks under chests for instance)