package de.project.ae2virtualwell.recipe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import de.project.ae2virtualwell.registry.ModRecipes;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;

import java.util.List;

public record WellDropRecipe(Ingredient target, int minTier, List<WellDropEntry> drops) implements Recipe<SingleRecipeInput> {

    @Override
    public boolean matches(SingleRecipeInput input, Level level) {
        return target.test(input.item());
    }

    @Override
    public ItemStack assemble(SingleRecipeInput input, HolderLookup.Provider registries) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return true;
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider registries) {
        return ItemStack.EMPTY;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipes.WELL_DROP_SERIALIZER.get();
    }

    @Override
    public RecipeType<?> getType() {
        return ModRecipes.WELL_DROP_TYPE.get();
    }

    public static class Serializer implements RecipeSerializer<WellDropRecipe> {
        public static final MapCodec<WellDropRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                Ingredient.CODEC.fieldOf("target").forGetter(WellDropRecipe::target),
                Codec.INT.optionalFieldOf("min_tier", 1).forGetter(WellDropRecipe::minTier),
                WellDropEntry.CODEC.listOf().fieldOf("drops").forGetter(WellDropRecipe::drops)
        ).apply(instance, WellDropRecipe::new));

        public static final StreamCodec<RegistryFriendlyByteBuf, WellDropRecipe> STREAM_CODEC = StreamCodec.composite(
                Ingredient.CONTENTS_STREAM_CODEC, WellDropRecipe::target,
                ByteBufCodecs.VAR_INT, WellDropRecipe::minTier,
                WellDropEntry.STREAM_CODEC.apply(ByteBufCodecs.list()), WellDropRecipe::drops,
                WellDropRecipe::new
        );

        @Override
        public MapCodec<WellDropRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, WellDropRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    }
}
