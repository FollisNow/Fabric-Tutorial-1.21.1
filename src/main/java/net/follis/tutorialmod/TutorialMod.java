package net.follis.tutorialmod;

import net.fabricmc.api.ModInitializer;

import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.fabricmc.fabric.api.object.builder.v1.trade.TradeOfferHelper;
import net.fabricmc.fabric.api.registry.*;
import net.follis.tutorialmod.block.ModBlocks;
import net.follis.tutorialmod.block.entity.ModBlockEntities;
import net.follis.tutorialmod.component.ModDataComponentTypes;
import net.follis.tutorialmod.datagen.ModDispenserBehaviourProvider;
import net.follis.tutorialmod.datagen.ModPointOfInterestTypes;
import net.follis.tutorialmod.effect.ModEffects;
import net.follis.tutorialmod.enchantment.ModEnchantmentEffects;
import net.follis.tutorialmod.entity.ModEntities;
import net.follis.tutorialmod.entity.custom.AmethystBeeEntity;
import net.follis.tutorialmod.entity.custom.LocustEntity;
import net.follis.tutorialmod.entity.custom.MantisEntity;
import net.follis.tutorialmod.item.ModItemGroups;
import net.follis.tutorialmod.item.ModItems;
import net.follis.tutorialmod.particle.ModParticles;
import net.follis.tutorialmod.potion.ModPotions;
import net.follis.tutorialmod.recipe.ModRecipes;
import net.follis.tutorialmod.screen.ModScreenHandlers;
import net.follis.tutorialmod.sound.ModSounds;
import net.follis.tutorialmod.util.HammerUsageEvent;
import net.follis.tutorialmod.util.ModLootTableModifiers;
import net.follis.tutorialmod.villager.ModVillagers;
import net.follis.tutorialmod.world.gen.ModWorldGeneration;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.passive.SheepEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.potion.Potions;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.village.TradeOffer;
import net.minecraft.village.TradedItem;
import net.minecraft.village.VillagerProfession;
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
		ModSounds.registerSounds();

		ModEffects.registerEffects();
		ModPotions.registerPotions();

		ModEnchantmentEffects.registerEnchantmentEffects();
		ModWorldGeneration.generateModWorldGen();

		ModEntities.registerModEntities();
		ModVillagers.registerVillagers();
		ModPointOfInterestTypes.registerPointOfInterestTypes();

		ModParticles.registerParticles();
		ModLootTableModifiers.modifyLootTables();

		ModBlockEntities.registerBlockEntities();
		ModScreenHandlers.registerScreenHandlers();

		ModRecipes.registerRecipes();
		ModDispenserBehaviourProvider.registerDispenserBehaviour();

		FuelRegistry.INSTANCE.add(ModItems.STARLIGHT_ASHES, 600);

		PlayerBlockBreakEvents.BEFORE.register(new HammerUsageEvent());
		AttackEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
			if(entity instanceof SheepEntity sheepEntity) {
				if(player.getMainHandStack().getItem() == Items.END_ROD) {
					player.sendMessage(Text.literal("The Player just hit a sheep with an END ROD! YOU SICK FRICK!"));
					player.getMainHandStack().decrement(1);
					sheepEntity.addStatusEffect(new StatusEffectInstance(StatusEffects.POISON, 600, 6));
				}

				return ActionResult.PASS;
			}

            return ActionResult.PASS;
        });

		FabricBrewingRecipeRegistryBuilder.BUILD.register(builder -> {
			builder.registerPotionRecipe(Potions.MUNDANE, Items.SLIME_BALL, ModPotions.SLIMEY_POTION);
			builder.registerPotionRecipe(Potions.MUNDANE, Items.AMETHYST_SHARD, ModPotions.ATTRACTION_POTION);
			builder.registerPotionRecipe(Potions.MUNDANE, Items.MANGROVE_ROOTS, ModPotions.REJUVENATION_POTION);
			builder.registerPotionRecipe(Potions.MUNDANE, Items.CRIMSON_FUNGUS, ModPotions.HATRED_POTION);
			builder.registerPotionRecipe(Potions.MUNDANE, Items.WARPED_FUNGUS, ModPotions.LOVE_POTION);
			builder.registerPotionRecipe(Potions.MUNDANE, Items.BROWN_MUSHROOM, ModPotions.SPORULATION_POTION);
			builder.registerPotionRecipe(ModPotions.HATRED_POTION, Items.FERMENTED_SPIDER_EYE, ModPotions.LOVE_POTION);
			builder.registerPotionRecipe(ModPotions.LOVE_POTION, Items.FERMENTED_SPIDER_EYE, ModPotions.HATRED_POTION);
		});



		CompostingChanceRegistry.INSTANCE.add(ModItems.CAULIFLOWER, 0.5f);
		CompostingChanceRegistry.INSTANCE.add(ModItems.CAULIFLOWER_SEEDS, 0.25f);
		CompostingChanceRegistry.INSTANCE.add(ModItems.HONEY_BERRIES, 0.15f);
		CompostingChanceRegistry.INSTANCE.add(ModItems.LOCUST, 0.25f);
		CompostingChanceRegistry.INSTANCE.add(ModItems.GRILLED_LOCUST, 0.15f);


		StrippableBlockRegistry.register(ModBlocks.GOLDEN_LOG, ModBlocks.STRIPPED_GOLDEN_LOG);
		StrippableBlockRegistry.register(ModBlocks.GOLDEN_WOOD, ModBlocks.STRIPPED_GOLDEN_WOOD);

		FlammableBlockRegistry.getDefaultInstance().add(ModBlocks.GOLDEN_LOG, 5, 5);
		FlammableBlockRegistry.getDefaultInstance().add(ModBlocks.GOLDEN_WOOD, 5, 5);
		FlammableBlockRegistry.getDefaultInstance().add(ModBlocks.STRIPPED_GOLDEN_LOG, 5, 5);
		FlammableBlockRegistry.getDefaultInstance().add(ModBlocks.STRIPPED_GOLDEN_WOOD, 5, 5);
		FlammableBlockRegistry.getDefaultInstance().add(ModBlocks.GOLDEN_PLANKS, 5, 20);
		FlammableBlockRegistry.getDefaultInstance().add(ModBlocks.GOLDEN_LEAVES, 30, 60);

		FabricDefaultAttributeRegistry.register(ModEntities.MANTIS, MantisEntity.createAttributes());
		FabricDefaultAttributeRegistry.register(ModEntities.LOCUST, LocustEntity.createAttributes());
		FabricDefaultAttributeRegistry.register(ModEntities.AMETHYST_BEE, AmethystBeeEntity.createAttributes());

		TradeOfferHelper.registerVillagerOffers(VillagerProfession.FARMER, 1, factories -> {
			factories.add((entity, random) -> new TradeOffer(
					new TradedItem(Items.EMERALD, 3),
					new ItemStack(ModItems.CAULIFLOWER, 8), 7, 2, 0.04f));

			factories.add((entity, random) -> new TradeOffer(
					new TradedItem(Items.DIAMOND, 9),
					new ItemStack(ModItems.CAULIFLOWER_SEEDS, 2), 3, 4, 0.04f));
		});

		TradeOfferHelper.registerVillagerOffers(VillagerProfession.FARMER, 2, factories -> {
			factories.add((entity, random) -> new TradeOffer(
					new TradedItem(Items.EMERALD, 12),
					new ItemStack(ModItems.HONEY_BERRIES, 5), 4, 7, 0.04f));
		});

		TradeOfferHelper.registerVillagerOffers(ModVillagers.KAUPENGER, 1, factories -> {
			factories.add((entity, random) -> new TradeOffer(
					new TradedItem(Items.EMERALD, 10),
					new ItemStack(ModItems.CHISEL, 1), 4, 7, 0.04f));

			factories.add((entity, random) -> new TradeOffer(
					new TradedItem(Items.EMERALD, 16),
					new ItemStack(ModItems.RAW_PINK_GARNET, 1), 4, 7, 0.04f));
		});

		TradeOfferHelper.registerVillagerOffers(ModVillagers.KAUPENGER, 2, factories -> {
			factories.add((entity, random) -> new TradeOffer(
					new TradedItem(Items.EMERALD, 10),
					new ItemStack(ModItems.CHISEL, 1), 4, 7, 0.04f));

			factories.add((entity, random) -> new TradeOffer(
					new TradedItem(ModItems.PINK_GARNET, 16),
					new ItemStack(ModItems.TOMAHAWK, 1), 3, 12, 0.09f));
		});

		TradeOfferHelper.registerWanderingTraderOffers(1, factories -> {
			factories.add((entity, random) -> new TradeOffer(
					new TradedItem(Items.EMERALD, 10),
					new ItemStack(ModItems.CHISEL, 1), 4, 7, 0.04f));

			factories.add((entity, random) -> new TradeOffer(
					new TradedItem(ModItems.PINK_GARNET, 16),
					new ItemStack(ModItems.TOMAHAWK, 1), 3, 12, 0.09f));

			factories.add((entity, random) -> new TradeOffer(
					new TradedItem(Items.EMERALD, 4),
					new ItemStack(ModItems.BUG_JAR, 1), 3, 12, 0.09f));
		});
	}
	//Les fourmies attrapents les objets par terre et peuvent construire des structures
}