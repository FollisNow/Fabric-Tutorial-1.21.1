package net.follis.tutorialmod.recipe;

import net.follis.tutorialmod.item.ModItems;
import net.follis.tutorialmod.item.custom.CaddisflyCocoonItem;
import net.follis.tutorialmod.component.ModDataComponentTypes;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.Ingredient;
import net.minecraft.recipe.RecipeSerializer;
import net.minecraft.recipe.SpecialCraftingRecipe;
import net.minecraft.recipe.book.CraftingRecipeCategory;
import net.minecraft.recipe.input.CraftingRecipeInput;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.world.World;

public class CaddisflyFluteRecipe extends SpecialCraftingRecipe {
    public CaddisflyFluteRecipe(CraftingRecipeCategory category) {
        super(category);
    }

    @Override
    public boolean matches(CraftingRecipeInput input, World world) {
        ItemStack cocoonStack = ItemStack.EMPTY;
        int nonEmptyCount = 0;

        for (int i = 0; i < input.getSize(); i++) {
            ItemStack stack = input.getStackInSlot(i);
            if (stack.isEmpty()) continue;
            nonEmptyCount++;

            if (stack.getItem() == ModItems.CADDISFLY_COCOON) {
                cocoonStack = stack;
            } else {
                return false; // any unexpected item present invalidates the match
            }
        }

        return nonEmptyCount == 1 && !cocoonStack.isEmpty();
    }

    @Override
    public ItemStack craft(CraftingRecipeInput input, RegistryWrapper.WrapperLookup lookup) {
        ItemStack cocoonStack = ItemStack.EMPTY;
        for (int i = 0; i < input.getSize(); i++) {
            ItemStack stack = input.getStackInSlot(i);
            if (stack.getItem() == ModItems.CADDISFLY_COCOON) {
                cocoonStack = stack;
                break;
            }
        }

        ItemStack result = new ItemStack(ModItems.CADDISFLY_FLUTE);
        CaddisflyCocoonItem.CocoonData data = cocoonStack.get(ModDataComponentTypes.COCOON);
        if (data != null) {
            result.set(ModDataComponentTypes.COCOON, data);
        }
        return result;
    }

    @Override
    public boolean fits(int width, int height) {
        return true;
    }

    @Override
    public DefaultedList<Ingredient> getIngredients() {
        DefaultedList<Ingredient> list = DefaultedList.of();
        list.add(Ingredient.ofItems(ModItems.CADDISFLY_COCOON));
        list.add(Ingredient.ofItems(ModItems.CADDISFLY_FLUTE));
        return list;
    }

    @Override
    public ItemStack getResult(RegistryWrapper.WrapperLookup registriesLookup) {
        return new ItemStack(ModItems.CADDISFLY_FLUTE); // recipe-book preview icon only
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipes.CADDISFLY_FLUTE_SERIALIZER;
    }
}