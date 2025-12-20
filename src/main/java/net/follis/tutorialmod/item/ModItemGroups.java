package net.follis.tutorialmod.item;

import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.follis.tutorialmod.TutorialMod;
import net.follis.tutorialmod.block.ModBlocks;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public class ModItemGroups {
    public static final ItemGroup PINK_GARNET_ITEMS_GROUP = Registry.register(Registries.ITEM_GROUP,
            Identifier.of(TutorialMod.MOD_ID, "pink_garnet_items"),
            FabricItemGroup.builder().icon(() -> new ItemStack(ModItems.PINK_GARNET))
                    .displayName(Text.translatable("itemgroup.tutorialmod.pink_garnet_items"))
                    .entries((displayContext, entries) -> {
                        entries.add(ModItems.PINK_GARNET);
                        entries.add(ModItems.RAW_PINK_GARNET);

                        entries.add(ModItems.CHISEL);
                        entries.add(ModItems.CAULIFLOWER);
                        entries.add(ModItems.LOCUST_GOLD);
                        entries.add(ModItems.LOCUST_DREAM);
                        entries.add(ModItems.LOCUST_GRASSHOPPER);
                        entries.add(ModItems.LOCUST_RED);
                        entries.add(ModItems.GRILLED_LOCUST);

                        entries.add(ModItems.GOLDEN_GLOW_BERRIES);
                        entries.add(ModItems.GOLDEN_SWEET_BERRIES);
                        entries.add(ModItems.GOLDEN_SPIDER_EYE);

                        entries.add(ModItems.STARLIGHT_ASHES);

                        entries.add(ModItems.PINK_GARNET_SWORD);
                        entries.add(ModItems.PINK_GARNET_PICKAXE);
                        entries.add(ModItems.PINK_GARNET_SHOVEL);
                        entries.add(ModItems.PINK_GARNET_AXE);
                        entries.add(ModItems.PINK_GARNET_HOE);

                        entries.add(ModItems.PINK_GARNET_HAMMER);

                        entries.add(ModItems.PINK_GARNET_HELMET);
                        entries.add(ModItems.PINK_GARNET_CHESTPLATE);
                        entries.add(ModItems.PINK_GARNET_LEGGINGS);
                        entries.add(ModItems.PINK_GARNET_BOOTS);

                        entries.add(ModItems.PINK_GARNET_HORSE_ARMOR);
                        entries.add(ModItems.KAUPEN_SMITHING_TEMPLATE);

                        entries.add(ModItems.KAUPEN_BOW);
                        entries.add(ModItems.BAR_BRAWL_MUSIC_DISC);

                        entries.add(ModItems.CAULIFLOWER_SEEDS);
                        entries.add(ModItems.HONEY_BERRIES);

                        entries.add(ModItems.TOMAHAWK);
                        entries.add(ModItems.DART);
                        entries.add(ModItems.DART_SHOOTER);
                        entries.add(ModItems.SPECTRE_STAFF);

                        entries.add(ModItems.BUG_JAR);
                        entries.add(ModItems.CURSED_JAR);
                        entries.add(ModItems.BAMBOO_TRAP);
                        entries.add(ModItems.VISION_MONOCLE);
                        entries.add(ModItems.ZAMPONA);

                        entries.add(ModItems.CHITIN);

                        entries.add(ModItems.MANTIS_SPAWN_EGG);
                        entries.add(ModItems.BEETLE_SPAWN_EGG);
                        entries.add(ModItems.SPIDERLING_SPAWN_EGG);
                        entries.add(ModItems.MOTH_SPAWN_EGG);
                        entries.add(ModItems.LOCUST_SPAWN_EGG);
                        entries.add(ModItems.AMETHYST_BEE_SPAWN_EGG);

                        entries.add(ModItems.GOLDEN_NEEDLE);
                        entries.add(ModItems.MINI_SUN);
                        entries.add(ModItems.WOODEN_MACUAHUITL);
                        entries.add(ModItems.GOLDEN_MACUAHUITL);
                        entries.add(ModItems.GOLDEN_KNIFE);

                    }).build());

    public static final ItemGroup PINK_GARNET_BLOCKS_GROUP = Registry.register(Registries.ITEM_GROUP,
            Identifier.of(TutorialMod.MOD_ID, "pink_garnet_blocks"),
            FabricItemGroup.builder().icon(() -> new ItemStack(ModBlocks.PINK_GARNET_BLOCK))
                    .displayName(Text.translatable("itemgroup.tutorialmod.pink_garnet_blocks"))
                    .entries((displayContext, entries) -> {
                        entries.add(ModBlocks.PINK_GARNET_BLOCK);
                        entries.add(ModBlocks.RAW_PINK_GARNET_BLOCK);

                        entries.add(ModBlocks.PINK_GARNET_ORE);
                        entries.add(ModBlocks.PINK_GARNET_DEEPSLATE_ORE);

                        entries.add(ModBlocks.MAGIC_BLOCK);

                        entries.add(ModBlocks.PINK_GARNET_STAIRS);
                        entries.add(ModBlocks.PINK_GARNET_SLAB);

                        entries.add(ModBlocks.PINK_GARNET_BUTTON);
                        entries.add(ModBlocks.PINK_GARNET_PRESSURE_PLATE);

                        entries.add(ModBlocks.PINK_GARNET_FENCE);
                        entries.add(ModBlocks.PINK_GARNET_FENCE_GATE);
                        entries.add(ModBlocks.PINK_GARNET_WALL);

                        entries.add(ModBlocks.PINK_GARNET_DOOR);
                        entries.add(ModBlocks.PINK_GARNET_TRAPDOOR);

                        entries.add(ModBlocks.PINK_GARNET_LAMP);


                        //GOLDEN NATURE
                        entries.add(ModBlocks.GOLDEN_DIRT);
                        entries.add(ModBlocks.GOLDEN_SAND);
                        entries.add(ModBlocks.GOLDEN_SAPLING);
                        entries.add(ModBlocks.GOLDEN_LEAVES);
                        entries.add(ModBlocks.GOLDEN_LOG);
                        entries.add(ModBlocks.GOLDEN_WOOD);
                        entries.add(ModBlocks.STRIPPED_GOLDEN_LOG);
                        entries.add(ModBlocks.STRIPPED_GOLDEN_WOOD);

                        //GOLDEN WOOD
                        entries.add(ModBlocks.GOLDEN_PLANKS);
                        entries.add(ModBlocks.GOLDEN_STAIRS);
                        entries.add(ModBlocks.GOLDEN_SLAB);
                        entries.add(ModBlocks.GOLDEN_FENCE);
                        entries.add(ModBlocks.GOLDEN_FENCE_GATE);

                        //GOLD
                        entries.add(ModBlocks.GOLD_STAIRS);
                        entries.add(ModBlocks.GOLD_SLAB);
                        entries.add(ModBlocks.GOLD_BUTTON);
                        entries.add(ModBlocks.GOLD_FENCE);
                        entries.add(ModBlocks.GOLD_FENCE_GATE);
                        entries.add(ModBlocks.GOLD_WALL);

                        //GOLDEN BRICKS
                        entries.add(ModBlocks.GOLDEN_BRICKS);
                        entries.add(ModBlocks.GOLDEN_BRICK_STAIRS);
                        entries.add(ModBlocks.GOLDEN_BRICK_SLAB);
                        entries.add(ModBlocks.GOLDEN_BRICK_WALL);

                        //GOLD BRICKS
                        entries.add(ModBlocks.GOLD_BRICKS);
                        entries.add(ModBlocks.GOLD_BRICK_STAIRS);
                        entries.add(ModBlocks.GOLD_BRICK_SLAB);
                        entries.add(ModBlocks.GOLD_BRICK_WALL);

                        //COBBLED GOLD
                        entries.add(ModBlocks.COBBLED_GOLD);
                        entries.add(ModBlocks.COBBLED_GOLD_STAIRS);
                        entries.add(ModBlocks.COBBLED_GOLD_SLAB);
                        entries.add(ModBlocks.COBBLED_GOLD_WALL);

                        //CUT GOLD
                        entries.add(ModBlocks.CUT_GOLD);
                        entries.add(ModBlocks.CUT_GOLD_STAIRS);
                        entries.add(ModBlocks.CUT_GOLD_SLAB);
                        entries.add(ModBlocks.CUT_GOLD_WALL);

                        //GOLD LARGE BRICKS
                        entries.add(ModBlocks.GOLD_LARGE_BRICKS);
                        entries.add(ModBlocks.GOLD_LARGE_BRICK_STAIRS);
                        entries.add(ModBlocks.GOLD_LARGE_BRICK_SLAB);
                        entries.add(ModBlocks.GOLD_LARGE_BRICK_WALL);

                        //GOLD MISC
                        entries.add(ModBlocks.CRUMBLED_GOLD);
                        entries.add(ModBlocks.CHISELED_GOLD);
                        entries.add(ModBlocks.CHISELED_GOLD_BRICKS);
                        entries.add(ModBlocks.SHAPED_GOLD);
                        entries.add(ModBlocks.ENGRAVED_GOLD);
                        entries.add(ModBlocks.SCULPTED_GOLD);
                        entries.add(ModBlocks.GOLD_CHAIN);
                        entries.add(ModBlocks.GOLD_LANTERN);

                        entries.add(ModBlocks.CRYSTAL_MUSHROOM);
                        entries.add(ModBlocks.CHAIR);
                        entries.add(ModBlocks.GOLDEN_PEDESTAL);
                        entries.add(ModBlocks.GOLDEN_HOTEL);
                        entries.add(ModBlocks.GROWTH_CHAMBER);
                        entries.add(ModBlocks.AMETHYST_BEE_HIVE);


                    }).build());


    public static void registerItemGroups() {
        TutorialMod.LOGGER.info("Registering Item Groups for " + TutorialMod.MOD_ID);
    }
}
