package net.follis.tutorialmod.block;

import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.follis.tutorialmod.TutorialMod;
import net.follis.tutorialmod.block.custom.*;
import net.follis.tutorialmod.sound.ModSounds;
import net.follis.tutorialmod.world.tree.ModSaplingGenerators;
import net.minecraft.block.*;
import net.minecraft.block.piston.PistonBehavior;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroups;
import net.minecraft.item.TallBlockItem;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.util.ColorCode;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.intprovider.UniformIntProvider;
import net.minecraft.world.gen.feature.TreeConfiguredFeatures;

public class ModBlocks {
    public static final Block PINK_GARNET_BLOCK = registerBlock("pink_garnet_block",
            new Block(AbstractBlock.Settings.create().strength(4f)
                    .requiresTool().sounds(BlockSoundGroup.AMETHYST_BLOCK)));
    public static final Block RAW_PINK_GARNET_BLOCK = registerBlock("raw_pink_garnet_block",
            new Block(AbstractBlock.Settings.create().strength(3f)
                    .requiresTool()));

    public static final Block PINK_GARNET_ORE = registerBlock("pink_garnet_ore",
            new ExperienceDroppingBlock(UniformIntProvider.create(2, 5),
                    AbstractBlock.Settings.create().strength(3f).requiresTool()));
    public static final Block PINK_GARNET_DEEPSLATE_ORE = registerBlock("pink_garnet_deepslate_ore",
            new ExperienceDroppingBlock(UniformIntProvider.create(3, 6),
                    AbstractBlock.Settings.create().strength(4f).requiresTool().sounds(BlockSoundGroup.DEEPSLATE)));

    public static final Block PINK_GARNET_END_ORE = registerBlock("pink_garnet_end_ore",
            new ExperienceDroppingBlock(UniformIntProvider.create(4, 8),
                    AbstractBlock.Settings.create().strength(7f).requiresTool()));
    public static final Block PINK_GARNET_NETHER_ORE = registerBlock("pink_garnet_nether_ore",
            new ExperienceDroppingBlock(UniformIntProvider.create(1, 5),
                    AbstractBlock.Settings.create().strength(3f).requiresTool()));

    public static final Block MAGIC_BLOCK = registerBlock("magic_block",
            new MagicBlock(AbstractBlock.Settings.create().strength(1f).requiresTool().sounds(ModSounds.MAGIC_BLOCK_SOUNDS)));

    public static final Block PINK_GARNET_STAIRS = registerBlock("pink_garnet_stairs",
            new StairsBlock(ModBlocks.PINK_GARNET_BLOCK.getDefaultState(),
                    AbstractBlock.Settings.create().strength(2f).requiresTool()));
    public static final Block PINK_GARNET_SLAB = registerBlock("pink_garnet_slab",
            new SlabBlock(AbstractBlock.Settings.create().strength(2f).requiresTool()));

    public static final Block PINK_GARNET_BUTTON = registerBlock("pink_garnet_button",
            new ButtonBlock(BlockSetType.IRON, 2, AbstractBlock.Settings.create().strength(2f).requiresTool().noCollision()));
    public static final Block PINK_GARNET_PRESSURE_PLATE = registerBlock("pink_garnet_pressure_plate",
            new PressurePlateBlock(BlockSetType.IRON, AbstractBlock.Settings.create().strength(2f).requiresTool()));

    public static final Block PINK_GARNET_FENCE = registerBlock("pink_garnet_fence",
            new FenceBlock(AbstractBlock.Settings.create().strength(2f).requiresTool()));
    public static final Block PINK_GARNET_FENCE_GATE = registerBlock("pink_garnet_fence_gate",
            new FenceGateBlock(WoodType.ACACIA, AbstractBlock.Settings.create().strength(2f).requiresTool()));
    public static final Block PINK_GARNET_WALL = registerBlock("pink_garnet_wall",
            new WallBlock(AbstractBlock.Settings.create().strength(2f).requiresTool()));

    public static final Block PINK_GARNET_DOOR = registerBlock("pink_garnet_door",
            new DoorBlock(BlockSetType.IRON, AbstractBlock.Settings.create().strength(2f).requiresTool().nonOpaque()));
    public static final Block PINK_GARNET_TRAPDOOR = registerBlock("pink_garnet_trapdoor",
            new TrapdoorBlock(BlockSetType.IRON, AbstractBlock.Settings.create().strength(2f).requiresTool().nonOpaque()));

    public static final Block PINK_GARNET_LAMP = registerBlock("pink_garnet_lamp",
            new PinkGarnetLampBlock(AbstractBlock.Settings.create()
                    .strength(1f).requiresTool().luminance(state -> state.get(PinkGarnetLampBlock.CLICKED) ? 15 : 0)));

    public static final Block CAULIFLOWER_CROP = registerBlockWithoutBlockItem("cauliflower_crop",
            new CauliflowerCropBlock(AbstractBlock.Settings.create().noCollision()
                    .ticksRandomly().breakInstantly().sounds(BlockSoundGroup.CROP).pistonBehavior(PistonBehavior.DESTROY).mapColor(MapColor.DARK_GREEN)));

    public static final Block HONEY_BERRY_BUSH = registerBlockWithoutBlockItem("honey_berry_bush",
            new HoneyBerryBushBlock(AbstractBlock.Settings.copy(Blocks.SWEET_BERRY_BUSH)));

    public static final Block CRYSTAL_MUSHROOM = registerBlock("crystal_mushroom",
            new MushroomPlantBlock(TreeConfiguredFeatures.HUGE_BROWN_MUSHROOM, AbstractBlock.Settings.create().mapColor(MapColor.PURPLE).noCollision().ticksRandomly().breakInstantly().sounds(BlockSoundGroup.GRASS).luminance((state) -> 1).postProcess(Blocks::always).pistonBehavior(PistonBehavior.DESTROY)));


    // The Goldening

    public static final Block GOLDEN_DIRT = registerBlock("golden_dirt",
            new Block(AbstractBlock.Settings.copy(Blocks.DIRT)));
    public static final Block GOLDEN_SAND = registerBlock("golden_sand",
            new ColoredFallingBlock(new ColorCode(14406560),AbstractBlock.Settings.copy(Blocks.SAND)));

    public static final Block GOLDEN_SAPLING = registerBlock("golden_sapling",
            new ModSaplingBlock(ModSaplingGenerators.GOLDEN_TREE, AbstractBlock.Settings.copy(Blocks.OAK_SAPLING), ModBlocks.GOLDEN_DIRT));
    public static final Block GOLDEN_LEAVES = registerBlock("golden_leaves",
            new GoldenLeavesBlock(AbstractBlock.Settings.copy(Blocks.CHERRY_LEAVES)));

    public static final Block GOLDEN_LOG = registerBlock("golden_log",
            new PillarBlock(AbstractBlock.Settings.copy(Blocks.OAK_LOG)));
    public static final Block GOLDEN_WOOD = registerBlock("golden_wood",
            new PillarBlock(AbstractBlock.Settings.copy(Blocks.OAK_WOOD)));
    public static final Block STRIPPED_GOLDEN_LOG = registerBlock("stripped_golden_log",
            new PillarBlock(AbstractBlock.Settings.copy(Blocks.STRIPPED_OAK_LOG)));
    public static final Block STRIPPED_GOLDEN_WOOD = registerBlock("stripped_golden_wood",
            new PillarBlock(AbstractBlock.Settings.copy(Blocks.STRIPPED_OAK_WOOD)));

    // GOLDEN WOOD
    public static final Block GOLDEN_PLANKS = registerBlock("golden_planks",
            new Block(AbstractBlock.Settings.copy(Blocks.OAK_PLANKS)));
    public static final Block GOLDEN_STAIRS = registerBlock("golden_stairs",
            new SittableStairsBlock(ModBlocks.GOLDEN_PLANKS.getDefaultState(), AbstractBlock.Settings.create().strength(2f)));
    public static final Block GOLDEN_SLAB = registerBlock("golden_slab",
            new SittableSlabBlock(AbstractBlock.Settings.create().strength(2f)));
    public static final Block GOLDEN_FENCE = registerBlock("golden_fence",
            new FenceBlock(AbstractBlock.Settings.create().strength(2f)));
    public static final Block GOLDEN_FENCE_GATE = registerBlock("golden_fence_gate",
            new FenceGateBlock(WoodType.ACACIA, AbstractBlock.Settings.create().strength(2f)));

    //GOLD
    public static final Block GOLD_STAIRS = registerBlock("gold_stairs",
            new SittableStairsBlock(Blocks.GOLD_BLOCK.getDefaultState(),
                    AbstractBlock.Settings.create().strength(2f).requiresTool()));
    public static final Block GOLD_SLAB = registerBlock("gold_slab",
            new SittableSlabBlock(AbstractBlock.Settings.create().strength(2f).requiresTool()));
    public static final Block GOLD_BUTTON = registerBlock("gold_button",
            new ButtonBlock(BlockSetType.IRON, 2, AbstractBlock.Settings.create().strength(2f).requiresTool().noCollision()));
    public static final Block GOLD_FENCE = registerBlock("gold_fence",
            new FenceBlock(AbstractBlock.Settings.create().strength(2f).requiresTool()));
    public static final Block GOLD_FENCE_GATE = registerBlock("gold_fence_gate",
            new FenceGateBlock(WoodType.ACACIA, AbstractBlock.Settings.create().strength(2f).requiresTool()));
    public static final Block GOLD_WALL = registerBlock("gold_wall",
            new WallBlock(AbstractBlock.Settings.create().strength(2f).requiresTool()));

    // GOLDEN BRICKS
    public static final Block GOLDEN_BRICKS = registerBlock("golden_bricks",
            new Block(AbstractBlock.Settings.copy(Blocks.STONE_BRICKS).strength(4).requiresTool()));
    public static final Block GOLDEN_BRICK_STAIRS = registerBlock("golden_brick_stairs",
            new SittableStairsBlock(ModBlocks.GOLDEN_BRICKS.getDefaultState(),
                    AbstractBlock.Settings.create().strength(2f).requiresTool()));
    public static final Block GOLDEN_BRICK_SLAB = registerBlock("golden_brick_slab",
            new SittableSlabBlock(AbstractBlock.Settings.create().strength(2f).requiresTool()));
    public static final Block GOLDEN_BRICK_WALL = registerBlock("golden_brick_wall",
            new WallBlock(AbstractBlock.Settings.create().strength(2f).requiresTool()));

    // GOLD BRICKS
    public static final Block GOLD_BRICKS = registerBlock("gold_bricks",
            new Block(AbstractBlock.Settings.copy(Blocks.BRICKS).strength(4).requiresTool()));
    public static final Block GOLD_BRICK_STAIRS = registerBlock("gold_brick_stairs",
            new SittableStairsBlock(ModBlocks.GOLD_BRICKS.getDefaultState(),
                    AbstractBlock.Settings.create().strength(2f).requiresTool()));
    public static final Block GOLD_BRICK_SLAB = registerBlock("gold_brick_slab",
            new SittableSlabBlock(AbstractBlock.Settings.create().strength(2f).requiresTool()));
    public static final Block GOLD_BRICK_WALL = registerBlock("gold_brick_wall",
            new WallBlock(AbstractBlock.Settings.create().strength(2f).requiresTool()));

    // COBBLED GOLD
    public static final Block COBBLED_GOLD = registerBlock("cobbled_gold",
            new Block(AbstractBlock.Settings.copy(Blocks.COBBLESTONE).strength(4).requiresTool()));
    public static final Block COBBLED_GOLD_STAIRS = registerBlock("cobbled_gold_stairs",
            new SittableStairsBlock(ModBlocks.COBBLED_GOLD.getDefaultState(),
                    AbstractBlock.Settings.create().strength(2f).requiresTool()));
    public static final Block COBBLED_GOLD_SLAB = registerBlock("cobbled_gold_slab",
            new SittableSlabBlock(AbstractBlock.Settings.create().strength(2f).requiresTool()));
    public static final Block COBBLED_GOLD_WALL = registerBlock("cobbled_gold_wall",
            new WallBlock(AbstractBlock.Settings.create().strength(2f).requiresTool()));

    // CUT GOLD
    public static final Block CUT_GOLD = registerBlock("cut_gold",
            new Block(AbstractBlock.Settings.copy(Blocks.CUT_COPPER).strength(4).requiresTool()));
    public static final Block CUT_GOLD_STAIRS = registerBlock("cut_gold_stairs",
            new SittableStairsBlock(ModBlocks.CUT_GOLD.getDefaultState(),
                    AbstractBlock.Settings.create().strength(2f).requiresTool()));
    public static final Block CUT_GOLD_SLAB = registerBlock("cut_gold_slab",
            new SittableSlabBlock(AbstractBlock.Settings.create().strength(2f).requiresTool()));
    public static final Block CUT_GOLD_WALL = registerBlock("cut_gold_wall",
            new WallBlock(AbstractBlock.Settings.create().strength(2f).requiresTool()));

    // GOLD LARGE BRICKS
    public static final Block GOLD_LARGE_BRICKS = registerBlock("gold_large_bricks",
            new Block(AbstractBlock.Settings.copy(Blocks.STONE_BRICKS).strength(4).requiresTool()));
    public static final Block GOLD_LARGE_BRICK_STAIRS = registerBlock("gold_large_brick_stairs",
            new SittableStairsBlock(ModBlocks.GOLD_LARGE_BRICKS.getDefaultState(),
                    AbstractBlock.Settings.create().strength(2f).requiresTool()));
    public static final Block GOLD_LARGE_BRICK_SLAB = registerBlock("gold_large_brick_slab",
            new SittableSlabBlock(AbstractBlock.Settings.create().strength(2f).requiresTool()));
    public static final Block GOLD_LARGE_BRICK_WALL = registerBlock("gold_large_brick_wall",
            new WallBlock(AbstractBlock.Settings.create().strength(2f).requiresTool()));

    //GOLD MISC
    public static final Block CRUMBLED_GOLD = registerBlock("crumbled_gold",
            new Block(AbstractBlock.Settings.copy(Blocks.COBBLED_DEEPSLATE).strength(4).requiresTool()));
    public static final Block CHISELED_GOLD = registerBlock("chiseled_gold",
            new Block(AbstractBlock.Settings.copy(Blocks.CHISELED_TUFF).strength(4).requiresTool()));
    public static final Block CHISELED_GOLD_BRICKS = registerBlock("chiseled_gold_bricks",
            new Block(AbstractBlock.Settings.copy(Blocks.CHISELED_TUFF_BRICKS).strength(4).requiresTool()));

    public static final Block SHAPED_GOLD = registerBlock("shaped_gold",
            new Block(AbstractBlock.Settings.copy(Blocks.SANDSTONE).strength(4).requiresTool()));
    public static final Block ENGRAVED_GOLD = registerBlock("engraved_gold",
            new Block(AbstractBlock.Settings.copy(Blocks.CHISELED_SANDSTONE).strength(4).requiresTool()));
    public static final Block SCULPTED_GOLD = registerBlock("sculpted_gold",
            new Block(AbstractBlock.Settings.copy(Blocks.CUT_SANDSTONE).strength(4).requiresTool()));
    public static final Block GOLD_CHAIN = registerBlockWithoutBlockItem("gold_chain",
            new ChainBlock(AbstractBlock.Settings.copy(Blocks.CHAIN).nonOpaque()));
    public static final Block GOLD_LANTERN = registerBlock("gold_lantern",
            new LanternBlock(AbstractBlock.Settings.copy(Blocks.LANTERN)));

    public static final Block YELLOW_MARIGOLD = registerBlock("yellow_marigold",
            new FlowerBlock(StatusEffects.ABSORPTION, 100, AbstractBlock.Settings.copy(Blocks.ALLIUM).nonOpaque().noCollision()));
    public static final Block POTTED_YELLOW_MARIGOLD = registerBlockWithoutBlockItem("potted_yellow_marigold",
            new FlowerPotBlock(YELLOW_MARIGOLD, AbstractBlock.Settings.copy(Blocks.POTTED_ALLIUM).nonOpaque()));

    public static final Block AURORA_FIRE_MARIGOLD = registerBlock("aurora_fire_marigold",
            new FlowerBlock(StatusEffects.FIRE_RESISTANCE, 200, AbstractBlock.Settings.copy(Blocks.ALLIUM).nonOpaque().noCollision()));
    public static final Block POTTED_AURORA_FIRE_MARIGOLD = registerBlockWithoutBlockItem("potted_aurora_fire_marigold",
            new FlowerPotBlock(AURORA_FIRE_MARIGOLD, AbstractBlock.Settings.copy(Blocks.POTTED_ALLIUM).nonOpaque()));

    public static final Block BLUE_MARIGOLD = registerBlock("blue_marigold",
            new FlowerBlock(StatusEffects.WATER_BREATHING, 200, AbstractBlock.Settings.copy(Blocks.ALLIUM).nonOpaque().noCollision()));
    public static final Block POTTED_BLUE_MARIGOLD = registerBlockWithoutBlockItem("potted_blue_marigold",
            new FlowerPotBlock(BLUE_MARIGOLD, AbstractBlock.Settings.copy(Blocks.POTTED_ALLIUM).nonOpaque()));

    public static final Block FOLLY_DIANTHUS = registerBlock("folly_dianthus",
            new FlowerBlock(StatusEffects.INSTANT_HEALTH, 20, AbstractBlock.Settings.copy(Blocks.ALLIUM).nonOpaque().noCollision()));
    public static final Block POTTED_FOLLY_DIANTHUS = registerBlockWithoutBlockItem("potted_folly_dianthus",
            new FlowerPotBlock(FOLLY_DIANTHUS, AbstractBlock.Settings.copy(Blocks.POTTED_ALLIUM).nonOpaque()));

    public static final Block PINK_DIANTHUS = registerBlock("pink_dianthus",
            new FlowerBlock(StatusEffects.REGENERATION, 100, AbstractBlock.Settings.copy(Blocks.ALLIUM).nonOpaque().noCollision()));
    public static final Block POTTED_PINK_DIANTHUS = registerBlockWithoutBlockItem("potted_pink_dianthus",
            new FlowerPotBlock(PINK_DIANTHUS, AbstractBlock.Settings.copy(Blocks.POTTED_ALLIUM).nonOpaque()));

    public static final Block GOLD_ROSE_BUSH = registerBlock("gold_rose_bush",
            new TallFlowerBlock(AbstractBlock.Settings.copy(Blocks.ROSE_BUSH)));


    public static final Block CHAIR = registerBlock("chair",
            new ChairBlock(AbstractBlock.Settings.create().nonOpaque()));

    public static final Block GOLDEN_PEDESTAL = registerBlock("golden_pedestal",
            new GoldenPedestalBlock(AbstractBlock.Settings.create().nonOpaque().strength(3f).requiresTool()));

    public static final Block GOLDEN_HOTEL = registerBlock("golden_hotel",
            new GoldenHotelBlock(AbstractBlock.Settings.create().nonOpaque().strength(3f).requiresTool()));


    public static final Block GROWTH_CHAMBER = registerBlock("growth_chamber",
            new GrowthChamberBlock(AbstractBlock.Settings.create().strength(3f).requiresTool()));

    public static final Block AMETHYST_BEE_HIVE = registerBlock("amethyst_bee_hive",
            new AmethystBeeHiveBlock(AbstractBlock.Settings.create().strength(4f).requiresTool()));


    private static Block registerBlockWithoutBlockItem(String name, Block block) {
        return Registry.register(Registries.BLOCK, Identifier.of(TutorialMod.MOD_ID, name), block);
    }

    private static Block registerBlock(String name, Block block) {
        registerBlockItem(name, block);
        return Registry.register(Registries.BLOCK, Identifier.of(TutorialMod.MOD_ID, name), block);
    }

    private static void registerBlockItem(String name, Block block) {
        Registry.register(Registries.ITEM, Identifier.of(TutorialMod.MOD_ID, name),
                new BlockItem(block, new Item.Settings()));
    }

    private static void registerTallBlockItem(String name, Block block) {
        Registry.register(Registries.ITEM, Identifier.of(TutorialMod.MOD_ID, name),
                new TallBlockItem(block, new Item.Settings()));
    }

    public static void registerModBlocks() {
        TutorialMod.LOGGER.info("Registering Mod Blocks for " + TutorialMod.MOD_ID);

        ItemGroupEvents.modifyEntriesEvent(ItemGroups.BUILDING_BLOCKS).register(entries -> {
            entries.add(ModBlocks.PINK_GARNET_BLOCK);
            entries.add(ModBlocks.RAW_PINK_GARNET_BLOCK);
        });
    }
}
