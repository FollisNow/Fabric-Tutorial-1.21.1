package net.follis.tutorialmod.datagen;

import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricModelProvider;
import net.follis.tutorialmod.block.ModBlocks;
import net.follis.tutorialmod.block.custom.CauliflowerCropBlock;
import net.follis.tutorialmod.block.custom.HoneyBerryBushBlock;
import net.follis.tutorialmod.block.custom.PinkGarnetLampBlock;
import net.follis.tutorialmod.item.ModItems;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.data.client.*;
import net.minecraft.item.ArmorItem;
import net.minecraft.util.Identifier;

import java.lang.reflect.Field;
import java.util.Optional;

public class ModModelProvider extends FabricModelProvider {
    public ModModelProvider(FabricDataOutput output) {
        super(output);
    }

    @Override
    public void generateBlockStateModels(BlockStateModelGenerator blockStateModelGenerator) {
        BlockStateModelGenerator.BlockTexturePool pinkGarnetPool = blockStateModelGenerator.registerCubeAllModelTexturePool(ModBlocks.PINK_GARNET_BLOCK);
        blockStateModelGenerator.registerSimpleCubeAll(ModBlocks.RAW_PINK_GARNET_BLOCK);
        blockStateModelGenerator.registerSimpleCubeAll(ModBlocks.PINK_GARNET_ORE);
        blockStateModelGenerator.registerSimpleCubeAll(ModBlocks.PINK_GARNET_DEEPSLATE_ORE);

        blockStateModelGenerator.registerSimpleCubeAll(ModBlocks.PINK_GARNET_NETHER_ORE);
        blockStateModelGenerator.registerSimpleCubeAll(ModBlocks.PINK_GARNET_END_ORE);

        blockStateModelGenerator.registerSimpleCubeAll(ModBlocks.MAGIC_BLOCK);

        pinkGarnetPool.stairs(ModBlocks.PINK_GARNET_STAIRS);
        pinkGarnetPool.slab(ModBlocks.PINK_GARNET_SLAB);

        pinkGarnetPool.button(ModBlocks.PINK_GARNET_BUTTON);
        pinkGarnetPool.pressurePlate(ModBlocks.PINK_GARNET_PRESSURE_PLATE);

        pinkGarnetPool.fence(ModBlocks.PINK_GARNET_FENCE);
        pinkGarnetPool.fenceGate(ModBlocks.PINK_GARNET_FENCE_GATE);
        pinkGarnetPool.wall(ModBlocks.PINK_GARNET_WALL);

        blockStateModelGenerator.registerDoor(ModBlocks.PINK_GARNET_DOOR);
        blockStateModelGenerator.registerTrapdoor(ModBlocks.PINK_GARNET_TRAPDOOR);

        Identifier lampOffIdentifier = TexturedModel.CUBE_ALL.upload(ModBlocks.PINK_GARNET_LAMP, blockStateModelGenerator.modelCollector);
        Identifier lampOnIdentifier = blockStateModelGenerator.createSubModel(ModBlocks.PINK_GARNET_LAMP, "_on", Models.CUBE_ALL, TextureMap::all);
        blockStateModelGenerator.blockStateCollector.accept(VariantsBlockStateSupplier.create(ModBlocks.PINK_GARNET_LAMP)
                .coordinate(BlockStateModelGenerator.createBooleanModelMap(PinkGarnetLampBlock.CLICKED, lampOnIdentifier, lampOffIdentifier)));

        blockStateModelGenerator.registerCrop(ModBlocks.CAULIFLOWER_CROP, CauliflowerCropBlock.AGE, 0, 1, 2, 3, 4, 5, 6);

        blockStateModelGenerator.registerTintableCrossBlockStateWithStages(ModBlocks.HONEY_BERRY_BUSH, BlockStateModelGenerator.TintType.NOT_TINTED,
                HoneyBerryBushBlock.AGE, 0, 1, 2, 3);

        blockStateModelGenerator.registerTintableCross(ModBlocks.CRYSTAL_MUSHROOM, BlockStateModelGenerator.TintType.NOT_TINTED);
        blockStateModelGenerator.registerFlowerPotPlant(ModBlocks.YELLOW_MARIGOLD, ModBlocks.POTTED_YELLOW_MARIGOLD, BlockStateModelGenerator.TintType.NOT_TINTED);
        blockStateModelGenerator.registerFlowerPotPlant(ModBlocks.AURORA_FIRE_MARIGOLD, ModBlocks.POTTED_AURORA_FIRE_MARIGOLD, BlockStateModelGenerator.TintType.NOT_TINTED);
        blockStateModelGenerator.registerFlowerPotPlant(ModBlocks.BLUE_MARIGOLD, ModBlocks.POTTED_BLUE_MARIGOLD, BlockStateModelGenerator.TintType.NOT_TINTED);
        blockStateModelGenerator.registerDoubleBlock(ModBlocks.GOLD_ROSE_BUSH, BlockStateModelGenerator.TintType.NOT_TINTED);

        blockStateModelGenerator.registerSimpleCubeAll(ModBlocks.GOLDEN_DIRT);

        blockStateModelGenerator.registerLog(ModBlocks.GOLDEN_LOG).log(ModBlocks.GOLDEN_LOG).wood(ModBlocks.GOLDEN_WOOD);
        blockStateModelGenerator.registerLog(ModBlocks.STRIPPED_GOLDEN_LOG).log(ModBlocks.STRIPPED_GOLDEN_LOG).wood(ModBlocks.STRIPPED_GOLDEN_WOOD);

        blockStateModelGenerator.registerSingleton(ModBlocks.GOLDEN_LEAVES, TexturedModel.LEAVES);
        blockStateModelGenerator.registerTintableCross(ModBlocks.GOLDEN_SAPLING, BlockStateModelGenerator.TintType.NOT_TINTED);

        blockStateModelGenerator.registerSimpleCubeAll(ModBlocks.CRUMBLED_GOLD);
        blockStateModelGenerator.registerSimpleCubeAll(ModBlocks.GOLDEN_SAND);
        blockStateModelGenerator.registerSingleton(ModBlocks.CHISELED_GOLD, TexturedModel.CUBE_COLUMN);
        blockStateModelGenerator.registerSingleton(ModBlocks.CHISELED_GOLD_BRICKS, TexturedModel.CUBE_COLUMN);
        blockStateModelGenerator.registerSingleton(ModBlocks.SHAPED_GOLD, TexturedModel.CUBE_BOTTOM_TOP);
        blockStateModelGenerator.registerSingleton(ModBlocks.ENGRAVED_GOLD, TexturedModel.CUBE_COLUMN);
        blockStateModelGenerator.registerSingleton(ModBlocks.SCULPTED_GOLD, TexturedModel.CUBE_COLUMN);
        blockStateModelGenerator.registerItemModel(ModItems.GOLD_CHAIN);
        blockStateModelGenerator.registerAxisRotated(ModBlocks.GOLD_CHAIN, ModelIds.getBlockModelId(ModBlocks.GOLD_CHAIN));
        blockStateModelGenerator.registerLantern(ModBlocks.GOLD_LANTERN);

        BlockStateModelGenerator.BlockTexturePool goldPool = blockStateModelGenerator.registerCubeAllModelTexturePool(Blocks.GOLD_BLOCK);
        goldPool.stairs(ModBlocks.GOLD_STAIRS);
        goldPool.slab(ModBlocks.GOLD_SLAB);
        goldPool.button(ModBlocks.GOLD_BUTTON);
        goldPool.fence(ModBlocks.GOLD_FENCE);
        goldPool.fenceGate(ModBlocks.GOLD_FENCE_GATE);
        goldPool.wall(ModBlocks.GOLD_WALL);

        registerBlockModels(blockStateModelGenerator, "GOLDEN_BRICKS");
        registerBlockModels(blockStateModelGenerator, "GOLD_BRICKS");
        registerBlockModels(blockStateModelGenerator, "COBBLED_GOLD");
        registerBlockModels(blockStateModelGenerator, "CUT_GOLD");
        registerBlockModels(blockStateModelGenerator, "GOLD_LARGE_BRICKS");

        BlockStateModelGenerator.BlockTexturePool goldenWoodPool = blockStateModelGenerator.registerCubeAllModelTexturePool(ModBlocks.GOLDEN_PLANKS);
        goldenWoodPool.stairs(ModBlocks.GOLDEN_STAIRS);
        goldenWoodPool.slab(ModBlocks.GOLDEN_SLAB);
        goldenWoodPool.fence(ModBlocks.GOLDEN_FENCE);
        goldenWoodPool.fenceGate(ModBlocks.GOLDEN_FENCE_GATE);

        blockStateModelGenerator.registerNorthDefaultHorizontalRotation(ModBlocks.CHAIR);

        blockStateModelGenerator.registerSimpleCubeAll(ModBlocks.GROWTH_CHAMBER);

        blockStateModelGenerator.registerBeehive(ModBlocks.AMETHYST_BEE_HIVE, TextureMap::sideFrontEnd);


    }
    public void registerBlockModels(BlockStateModelGenerator blockStateModelGenerator, String inputName) {
        String singularName = inputName.endsWith("S") ? inputName.substring(0, inputName.length() - 1) : inputName;

        Block inputBlock = getBlockByName(inputName);

        // Register the cube all model texture pool for the input block
        BlockStateModelGenerator.BlockTexturePool texturePool = blockStateModelGenerator.registerCubeAllModelTexturePool(inputBlock);

        // Register stairs, slab, and wall models
        texturePool.stairs(getBlockByName(singularName + "_STAIRS"));
        texturePool.slab(getBlockByName(singularName + "_SLAB"));
        texturePool.wall(getBlockByName(singularName + "_WALL"));
    }

    private Block getBlockByName(String name) {
        // Return the corresponding block from ModBlocks using reflection or a mapping strategy
        try {
            Field field = ModBlocks.class.getField(name);
            return (Block) field.get(null);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException("Block not found: " + name, e);
        }
    }

    @Override
    public void generateItemModels(ItemModelGenerator itemModelGenerator) {
        itemModelGenerator.register(ModItems.PINK_GARNET, Models.GENERATED);
        itemModelGenerator.register(ModItems.RAW_PINK_GARNET, Models.GENERATED);

        itemModelGenerator.register(ModItems.CAULIFLOWER, Models.GENERATED);
        itemModelGenerator.register(ModItems.LOCUST_GOLD, Models.GENERATED);
        itemModelGenerator.register(ModItems.LOCUST_DREAM, Models.GENERATED);
        itemModelGenerator.register(ModItems.LOCUST_GRASSHOPPER, Models.GENERATED);
        itemModelGenerator.register(ModItems.LOCUST_RED, Models.GENERATED);
        itemModelGenerator.register(ModItems.GRILLED_LOCUST, Models.GENERATED);

        itemModelGenerator.register(ModItems.GOLDEN_GLOW_BERRIES, Models.GENERATED);
        itemModelGenerator.register(ModItems.GOLDEN_SWEET_BERRIES, Models.GENERATED);
        itemModelGenerator.register(ModItems.GOLDEN_SPIDER_EYE, Models.GENERATED);

        itemModelGenerator.register(ModItems.BUG_JAR, Models.GENERATED);
        itemModelGenerator.register(ModItems.CURSED_JAR, Models.GENERATED);
        itemModelGenerator.register(ModItems.BAMBOO_TRAP, Models.GENERATED);
        itemModelGenerator.register(ModItems.CHITIN, Models.GENERATED);
        itemModelGenerator.register(ModItems.DART, Models.GENERATED);
        itemModelGenerator.register(ModItems.DART_SHOOTER, Models.GENERATED);

        itemModelGenerator.register(ModItems.HEATSTROKE_CURSED_EYE, Models.GENERATED);
        itemModelGenerator.register(ModItems.PHANTASMAL_CURSED_EYE, Models.GENERATED);

        itemModelGenerator.register(ModItems.ZAMPONA, Models.GENERATED);
        // itemModelGenerator.register(ModItems.CHISEL, Models.GENERATED);
        itemModelGenerator.register(ModItems.STARLIGHT_ASHES, Models.GENERATED);

        itemModelGenerator.register(ModItems.PINK_GARNET_SWORD, Models.HANDHELD);
        itemModelGenerator.register(ModItems.PINK_GARNET_PICKAXE, Models.HANDHELD);
        itemModelGenerator.register(ModItems.PINK_GARNET_SHOVEL, Models.HANDHELD);
        itemModelGenerator.register(ModItems.PINK_GARNET_AXE, Models.HANDHELD);
        itemModelGenerator.register(ModItems.PINK_GARNET_HOE, Models.HANDHELD);

        itemModelGenerator.register(ModItems.PINK_GARNET_HAMMER, Models.HANDHELD);

        itemModelGenerator.registerArmor(((ArmorItem) ModItems.PINK_GARNET_HELMET));
        itemModelGenerator.registerArmor(((ArmorItem) ModItems.PINK_GARNET_CHESTPLATE));
        itemModelGenerator.registerArmor(((ArmorItem) ModItems.PINK_GARNET_LEGGINGS));
        itemModelGenerator.registerArmor(((ArmorItem) ModItems.PINK_GARNET_BOOTS));

        itemModelGenerator.register(ModItems.PINK_GARNET_HORSE_ARMOR, Models.GENERATED);
        itemModelGenerator.register(ModItems.KAUPEN_SMITHING_TEMPLATE, Models.GENERATED);

        itemModelGenerator.register(ModItems.BAR_BRAWL_MUSIC_DISC, Models.GENERATED);

        itemModelGenerator.register(ModItems.MANTIS_SPAWN_EGG,
                new Model(Optional.of(Identifier.of("item/template_spawn_egg")), Optional.empty()));

        itemModelGenerator.register(ModItems.BEETLE_SPAWN_EGG,
                new Model(Optional.of(Identifier.of("item/template_spawn_egg")), Optional.empty()));

        itemModelGenerator.register(ModItems.SPIDERLING_SPAWN_EGG,
                new Model(Optional.of(Identifier.of("item/template_spawn_egg")), Optional.empty()));

      itemModelGenerator.register(ModItems.MOTH_SPAWN_EGG,
                new Model(Optional.of(Identifier.of("item/template_spawn_egg")), Optional.empty()));

        itemModelGenerator.register(ModItems.LOCUST_SPAWN_EGG,
                new Model(Optional.of(Identifier.of("item/template_spawn_egg")), Optional.empty()));

        itemModelGenerator.register(ModItems.AMETHYST_BEE_SPAWN_EGG,
                new Model(Optional.of(Identifier.of("item/template_spawn_egg")), Optional.empty()));

        itemModelGenerator.register(ModItems.GOLD_CARVER_SPAWN_EGG,
                new Model(Optional.of(Identifier.of("item/template_spawn_egg")), Optional.empty()));

        itemModelGenerator.register(ModItems.GOLDEN_NEEDLE, Models.HANDHELD);
        itemModelGenerator.register(ModItems.WOODEN_MACUAHUITL, Models.HANDHELD);
        itemModelGenerator.register(ModItems.GOLDEN_MACUAHUITL, Models.HANDHELD);
        itemModelGenerator.register(ModItems.GOLDEN_KNIFE, Models.HANDHELD);

        itemModelGenerator.register(ModItems.MINI_SUN, Models.GENERATED);
    }
}