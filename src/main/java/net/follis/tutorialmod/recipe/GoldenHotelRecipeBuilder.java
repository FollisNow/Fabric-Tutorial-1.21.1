package net.follis.tutorialmod.recipe;

import net.minecraft.item.Item;
import net.minecraft.item.ItemConvertible;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.Ingredient;
import net.minecraft.registry.tag.TagKey;

import java.util.List;

public class GoldenHotelRecipeBuilder {
    private final Item output;
    private final int count;
    private final List<Ingredient> inputs;
    private Ingredient inputMain;

    public GoldenHotelRecipeBuilder(ItemConvertible output, int count, List<Ingredient> inputs, Ingredient inputMain) {
        this.output = output.asItem();
        this.count = count;
        this.inputs = inputs;
        this.inputMain = inputMain;
    }

    public GoldenHotelRecipeBuilder create(ItemConvertible output, int count) {
        return new GoldenHotelRecipeBuilder(output, count, this.inputs, this.inputMain);
    }

    public GoldenHotelRecipeBuilder input(TagKey<Item> tag) {
        this.inputs.add(Ingredient.fromTag(tag));
        return this;
    }

    public void input(ItemConvertible itemProvider) {
        this.inputs.add(Ingredient.ofItems(itemProvider));
    }

    public void inputMain(ItemConvertible itemProvider) {
        this.inputMain = Ingredient.ofItems(itemProvider);
    }

    public Ingredient getInputMain() {return  this.inputMain;}
    public Ingredient getInput1() {return  this.inputs.get(0);}
    public Ingredient getInput2() {return  this.inputs.get(1);}
    public Item getOutputItem() {
        return this.output;
    }
    public ItemStack getOutputItemStack() {
        return new ItemStack(this.output, this.count);
    }
}
