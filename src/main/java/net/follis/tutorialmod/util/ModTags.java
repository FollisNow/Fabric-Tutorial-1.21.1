package net.follis.tutorialmod.util;

import net.follis.tutorialmod.TutorialMod;
import net.minecraft.block.Block;
import net.minecraft.entity.EntityType;
import net.minecraft.item.Item;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.Identifier;
import net.minecraft.world.poi.PointOfInterestType;

public class ModTags {
    public static class Blocks {
        public static final TagKey<Block> NEEDS_PINK_GARNET_TOOL = createTag("needs_pink_garnet_tool");
        public static final TagKey<Block> INCORRECT_FOR_PINK_GARNET_TOOL = createTag("incorrect_for_pink_garnet_tool");

        public static final TagKey<Block> NEEDS_GOLDEN_TOOL = createTag("needs_golden_tool");
        public static final TagKey<Block> INCORRECT_FOR_GOLDEN_TOOL = createTag("incorrect_for_golden_tool");

        private static TagKey<Block> createTag(String name) {
            return TagKey.of(RegistryKeys.BLOCK, Identifier.of(TutorialMod.MOD_ID, name));
        }
    }

    public static class Items {
        public static final TagKey<Item> TRANSFORMABLE_ITEMS = createTag("transformable_items");

        public static final TagKey<Item> GOLDEN_ITEMS = createTag("golden_items");

        public static final TagKey<Item> LOCUST_ITEMS = createTag("locust_items");
        private static TagKey<Item> createTag(String name) {
            return TagKey.of(RegistryKeys.ITEM, Identifier.of(TutorialMod.MOD_ID, name));
        }
    }

    public static class PointOfInterestTypes {
        public static final TagKey<PointOfInterestType> AMETHYST_BEE_HOME = createTag("amethyst_bee_home");

        private static TagKey<PointOfInterestType> createTag(String name) {
            return TagKey.of(RegistryKeys.POINT_OF_INTEREST_TYPE, Identifier.of(TutorialMod.MOD_ID, name));
        }
    }

    public static class EntityTypes {
        public static  final TagKey<EntityType<?>> BUGS = createTag("bugs");
        private static TagKey<EntityType<?>> createTag(String name) {
            return TagKey.of(RegistryKeys.ENTITY_TYPE, Identifier.of(TutorialMod.MOD_ID, name));
        }
    }
}
