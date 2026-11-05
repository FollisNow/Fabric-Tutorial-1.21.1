package net.follis.goldenscarabmod.util;

import net.follis.goldenscarabmod.block.ModBlocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.Ingredient;
import net.minecraft.recipe.RecipeEntry;
import net.minecraft.recipe.RecipeManager;
import net.minecraft.recipe.RecipeType;
import net.minecraft.recipe.StonecuttingRecipe;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.registry.Registries;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Gives every item of the {@code gold_blocks} tag a "value" (measured in full blocks) so ANY gold-like
 * block can be converted into ANY other at a lossless exchange rate, without writing N*N recipes.
 *
 * How the values are found (all from your existing stonecutting recipes, so datagen stays as is):
 * <ol>
 *   <li>Look at every {@code minecraft:stonecutting} recipe whose input AND result are in the tag.</li>
 *   <li>Items that no such recipe produces are "roots" and are worth 1 (gold block, gold bricks, ...).</li>
 *   <li>Walk the recipes from the roots: {@code 1 X -> N Y} means {@code value(Y) = value(X) / N}.
 *       (gold block -> 2 slabs: slab = 1/2. cut gold = 1/4, its slab = 1/8, ...)</li>
 * </ol>
 * The exchange rate between two items is the smallest whole numbers k, m with
 * {@code k * value(from) == m * value(to)}, so nothing is ever created or lost.
 *
 * The result is cached; call {@link #invalidate()} when data packs reload / the server stops.
 */
public final class GoldBlockValues {
    private GoldBlockValues() {}

    /** Exact fraction, always reduced, den > 0. */
    public record Fraction(long num, long den) {
        public static final Fraction ONE = new Fraction(1, 1);

        public static Fraction of(long num, long den) {
            long g = gcd(Math.abs(num), Math.abs(den));
            if (g > 1) { num /= g; den /= g; }
            return new Fraction(num, den);
        }

        public Fraction divide(long by) {
            return of(this.num, this.den * by);
        }
    }

    /** "Feed {@code inputCount} of the input to get {@code outputCount} of the template." */
    public record Exchange(int inputCount, int outputCount) {}

    private record Edge(Item out, int count) {}

    private static volatile Map<Item, Fraction> values;
    private static volatile int generation;

    /** Drop the cache (call on data pack reload and when the server stops). */
    public static void invalidate() {
        values = null;
        generation++;
    }

    /** Changes every time the cache is dropped: entities use it to drop their own craft caches. */
    public static int generation() {
        return generation;
    }

    @Nullable
    public static Exchange exchange(World world, Item from, Item to) {
        if (from == to) return null;

        Map<Item, Fraction> map = values(world);
        Fraction in = map.get(from);
        Fraction out = map.get(to);
        if (in == null || out == null) return null;

        // k * in.num/in.den == m * out.num/out.den  ->  k * in.num * out.den == m * out.num * in.den
        long left = in.num() * out.den();
        long right = out.num() * in.den();
        long g = gcd(left, right);
        long inputCount = right / g;
        long outputCount = left / g;

        if (inputCount < 1 || outputCount < 1) return null;
        if (inputCount > from.getMaxCount()) return null; // can't even hold that many in the golem's slot
        return new Exchange((int) inputCount, (int) outputCount);
    }

    private static Map<Item, Fraction> values(World world) {
        Map<Item, Fraction> result = values;
        if (result == null) {
            result = build(world);
            values = result;
        }
        return result;
    }

    private static Map<Item, Fraction> build(World world) {
        RecipeManager recipes = world.getRecipeManager();
        DynamicRegistryManager registries = world.getRegistryManager();

        Set<Item> pool = new HashSet<>();
        for (RegistryEntry<Item> entry : Registries.ITEM.iterateEntries(ModTags.Items.GOLD_BLOCKS)) {
            pool.add(entry.value());
        }

        Map<Item, List<Edge>> edges = new HashMap<>();
        Set<Item> produced = new HashSet<>();
        for (RecipeEntry<StonecuttingRecipe> entry : recipes.listAllOfType(RecipeType.STONECUTTING)) {
            StonecuttingRecipe recipe = entry.value();
            ItemStack result = recipe.getResult(registries);
            if (result.isEmpty() || !pool.contains(result.getItem())) continue;

            for (Ingredient ingredient : recipe.getIngredients()) {
                for (ItemStack stack : ingredient.getMatchingStacks()) {
                    Item in = stack.getItem();
                    if (in == result.getItem() || !pool.contains(in)) continue;
                    edges.computeIfAbsent(in, k -> new ArrayList<>()).add(new Edge(result.getItem(), result.getCount()));
                    produced.add(result.getItem());
                }
            }
        }

        Map<Item, Fraction> map = new HashMap<>();
        Deque<Item> queue = new ArrayDeque<>();
        for (Item item : pool) {
            if (!produced.contains(item)) { // roots: not made from another gold block by stonecutting
                map.put(item, rootValue(item));
                queue.add(item);
            }
        }
        while (!queue.isEmpty()) {
            Item in = queue.poll();
            Fraction value = map.get(in);
            for (Edge edge : edges.getOrDefault(in, List.of())) {
                if (map.containsKey(edge.out())) continue; // first derivation wins
                map.put(edge.out(), value.divide(edge.count()));
                queue.add(edge.out());
            }
        }
        return map;
    }

    /**
     * Value of a root, in full gold blocks. Roots that are CRAFTED from raw materials must be worth what
     * they cost, otherwise the golem turns cheap crafts into gold blocks (4 nuggets -> 1 block!).
     * A gold block = 9 ingots = 81 nuggets. Adjust to match your recipes.
     */
    private static Fraction rootValue(Item item) {
        if (item == ModBlocks.GOLD_BRICKS.asItem()) return Fraction.of(4, 9);     // 2x2 gold ingots
        if (item == ModBlocks.GOLDEN_BRICKS.asItem()) return Fraction.of(4, 81);  // 4 gold nuggets (+ stone bricks)
        return Fraction.ONE;                                                       // gold block and the like
    }

    private static long gcd(long a, long b) {
        while (b != 0) {
            long t = a % b;
            a = b;
            b = t;
        }
        return a == 0 ? 1 : a;
    }
}