package net.follis.goldenscarabmod.loot;

import net.follis.goldenscarabmod.GoldenScarabMod;
import net.minecraft.loot.LootTable;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;

public final class ModLootTables {
    public static final RegistryKey<LootTable> TEMPLE_COMMON   = key("chests/temple_common");
    public static final RegistryKey<LootTable> TEMPLE_RARE     = key("chests/temple_rare");
    public static final RegistryKey<LootTable> TEMPLE_SUPER_RARE     = key("chests/temple_super_rare");
    public static final RegistryKey<LootTable> TEMPLE_ULTRA_RARE     = key("chests/temple_ultra_rare");
    public static final RegistryKey<LootTable> TEMPLE_MINIBOSS = key("chests/temple_miniboss");

    private ModLootTables() {}

    private static RegistryKey<LootTable> key(String path) {
        return RegistryKey.of(RegistryKeys.LOOT_TABLE, Identifier.of(GoldenScarabMod.MOD_ID, path));
    }
}