package net.follis.tutorialmod.entity;

import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricEntityTypeBuilder;
import net.follis.tutorialmod.TutorialMod;
import net.follis.tutorialmod.entity.custom.*;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

import java.util.function.Consumer;

public class ModEntities {
    public static final EntityType<MantisEntity> MANTIS = Registry.register(Registries.ENTITY_TYPE,
            Identifier.of(TutorialMod.MOD_ID, "mantis"),
            EntityType.Builder.create(MantisEntity::new, SpawnGroup.CREATURE)
                    .dimensions(1f, 2.5f).build());

    public static final EntityType<LocustEntity> LOCUST = Registry.register(Registries.ENTITY_TYPE,
            Identifier.of(TutorialMod.MOD_ID, "locust"),
            EntityType.Builder.create(LocustEntity::new, SpawnGroup.CREATURE)
                    .dimensions(0.5f, 0.5f).build());

    public static final EntityType<AmethystBeeEntity> AMETHYST_BEE = Registry.register(Registries.ENTITY_TYPE,
            Identifier.of(TutorialMod.MOD_ID, "amethyst_bee"),
            EntityType.Builder.create(AmethystBeeEntity::new, SpawnGroup.CREATURE)
                    .dimensions(0.5f, 0.5f).build());

    public static final EntityType<BeetleEntity> BEETLE = Registry.register(Registries.ENTITY_TYPE,
            Identifier.of(TutorialMod.MOD_ID, "beetle"),
            EntityType.Builder.create(BeetleEntity::new, SpawnGroup.CREATURE)
                    .dimensions(0.5f, 0.5f).build());

    public static final EntityType<MothEntity> MOTH = Registry.register(Registries.ENTITY_TYPE,
            Identifier.of(TutorialMod.MOD_ID, "moth"),
            EntityType.Builder.create(MothEntity::new, SpawnGroup.CREATURE)
                    .dimensions(0.4f, 0.75f).build());


    public static final EntityType<TomahawkProjectileEntity> TOMAHAWK = Registry.register(Registries.ENTITY_TYPE,
            Identifier.of(TutorialMod.MOD_ID, "tomahawk"),
            EntityType.Builder.<TomahawkProjectileEntity>create(TomahawkProjectileEntity::new, SpawnGroup.MISC)
                    .dimensions(0.5f, 1.15f).build());

    public static final EntityType<BambooTrapEntity> BAMBOO_TRAP = Registry.register(Registries.ENTITY_TYPE,
            Identifier.of(TutorialMod.MOD_ID, "bamboo_trap"),
            EntityType.Builder.create(BambooTrapEntity::new, SpawnGroup.MISC).makeFireImmune()
                    .dimensions(0.4f, 0.2f).build());

    public static final EntityType<GoldenNeedleProjectileEntity> GOLDEN_NEEDLE = Registry.register(Registries.ENTITY_TYPE,
            Identifier.of(TutorialMod.MOD_ID, "golden_needle"),
            EntityType.Builder.<GoldenNeedleProjectileEntity>create(GoldenNeedleProjectileEntity::new, SpawnGroup.MISC)
                    .dimensions(0.5f, 0.5f).build());

    public static final EntityType<ChairEntity> CHAIR = Registry.register(Registries.ENTITY_TYPE,
            Identifier.of(TutorialMod.MOD_ID, "chair_entity"),
            EntityType.Builder.create(ChairEntity::new, SpawnGroup.MISC)
                    .dimensions(0.5f, 0.5f).build());



    public static void registerModEntities() {
        TutorialMod.LOGGER.info("Registering Mod Entities for " + TutorialMod.MOD_ID);
        initialize();
    }
    static void initialize() {
        FabricDefaultAttributeRegistry.register(ModEntities.BAMBOO_TRAP, BambooTrapEntity.setAttributes());
    }
}
