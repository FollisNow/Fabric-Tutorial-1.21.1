package net.follis.goldenscarabmod.entity.custom;

import net.follis.goldenscarabmod.effect.ModEffects;
import net.follis.goldenscarabmod.effect.VenomEffects;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.StringIdentifiable;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

/** Pottery sherd a scorpion can assimilate, and what its sting does with it. */
public enum ScorpionShard implements StringIdentifiable {
    NONE("none", null, 0, null),
    ANGLER("angler", Items.ANGLER_POTTERY_SHERD, 2, () -> StatusEffects.GLOWING),
    ARCHER("archer", Items.ARCHER_POTTERY_SHERD, 2, () -> VenomEffects.ARCHER),
    ARMS_UP("arms_up", Items.ARMS_UP_POTTERY_SHERD, 2, () -> VenomEffects.ARMS_UP),
    BLADE("blade", Items.BLADE_POTTERY_SHERD, 2, () -> VenomEffects.BLADE),
    BREWER("brewer", Items.BREWER_POTTERY_SHERD, 0, null),   // purge, handled in applyTo
    BURN("burn", Items.BURN_POTTERY_SHERD, 0, null),         // fire, handled in applyTo
    DANGER("danger", Items.DANGER_POTTERY_SHERD, 2, () -> VenomEffects.DANGER),
    EXPLORER("explorer", Items.EXPLORER_POTTERY_SHERD, 2, () -> VenomEffects.EXPLORER),
    FLOW("flow", Items.FLOW_POTTERY_SHERD, 2, () -> VenomEffects.FLOW),
    FRIEND("friend", Items.FRIEND_POTTERY_SHERD, 2, () -> ModEffects.HATRED),
    GUSTER("guster", Items.GUSTER_POTTERY_SHERD, 2, () -> VenomEffects.GUSTER),
    HEART("heart", Items.HEART_POTTERY_SHERD, 2, () -> VenomEffects.HEART),
    HEARTBREAK("heartbreak", Items.HEARTBREAK_POTTERY_SHERD, 2, () -> VenomEffects.HEARTBREAK),
    HOWL("howl", Items.HOWL_POTTERY_SHERD, 2, () -> VenomEffects.HOWL),
    MINER("miner", Items.MINER_POTTERY_SHERD, 2, () -> VenomEffects.MINER),
    MOURNER("mourner", Items.MOURNER_POTTERY_SHERD, 2, () -> VenomEffects.MOURNER),
    PLENTY("plenty", Items.PLENTY_POTTERY_SHERD, 2, () -> VenomEffects.PLENTY),
    PRIZE("prize", Items.PRIZE_POTTERY_SHERD, 2, () -> VenomEffects.PRIZE),
    SCRAPE("scrape", Items.SCRAPE_POTTERY_SHERD, 2, () -> VenomEffects.SCRAPE),
    SHEAF("sheaf", Items.SHEAF_POTTERY_SHERD, 2, () -> VenomEffects.SHEAF),
    SHELTER("shelter", Items.SHELTER_POTTERY_SHERD, 2, () -> VenomEffects.SHELTER),
    SKULL("skull", Items.SKULL_POTTERY_SHERD, 2, () -> VenomEffects.SKULL),
    SNORT("snort", Items.SNORT_POTTERY_SHERD, 2, () -> VenomEffects.SNORT);

    private static final Map<Item, ScorpionShard> BY_ITEM = new HashMap<>();
    static {
        for (ScorpionShard shard : values()) {
            if (shard.sherd != null) BY_ITEM.put(shard.sherd, shard);
        }
    }

    private final String name;
    private final Item sherd;
    private final int minutes;
    private final Supplier<RegistryEntry<StatusEffect>> effect;

    ScorpionShard(String name, Item sherd, int minutes, Supplier<RegistryEntry<StatusEffect>> effect) {
        this.name = name;
        this.sherd = sherd;
        this.minutes = minutes;
        this.effect = effect;
    }

    public Item getSherd() { return this.sherd; }
    public int getId() { return this.ordinal(); }

    @Override
    public String asString() { return this.name; }

    public static ScorpionShard byId(int id) {
        ScorpionShard[] all = values();
        return id >= 0 && id < all.length ? all[id] : NONE;
    }

    public static ScorpionShard byName(String name) {
        for (ScorpionShard shard : values()) {
            if (shard.name.equals(name)) return shard;
        }
        return NONE;
    }

    /** The shard this item is, or null if it is not a supported pottery sherd. */
    public static ScorpionShard fromStack(ItemStack stack) {
        return stack.isEmpty() ? null : BY_ITEM.get(stack.getItem());
    }

    /** Called on the server when the scorpion's sting lands. */
    public void applyTo(LivingEntity target, LivingEntity source) {
        switch (this) {
            case NONE -> { }
            case BREWER -> target.clearStatusEffects();
            case BURN -> target.setOnFireFor(8);
            default -> target.addStatusEffect(
                    new StatusEffectInstance(this.effect.get(), this.minutes * 60 * 20), source);
        }
    }

    public int getMinutes() { return this.minutes; }

    public String getTooltipKey() { return "tooltip.goldenscarabmod.shard." + this.name; }

    /** Tooltip colour: the venom effect's own colour, with fixed ones for the two without an effect. */
    public int getColor() {
        return switch (this) {
            case NONE -> 0xAAAAAA;
            case BREWER -> 0xB06FD6;
            case BURN -> 0xFF6A00;
            default -> this.effect.get().value().getColor();
        };
    }
}