package de.project.ae2virtualwell.registry;

import de.project.ae2virtualwell.AE2VirtualWell;
import de.project.ae2virtualwell.recipe.WellDropRecipe;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModRecipes {
    public static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS =
            DeferredRegister.create(Registries.RECIPE_SERIALIZER, AE2VirtualWell.MODID);

    public static final DeferredRegister<RecipeType<?>> RECIPE_TYPES =
            DeferredRegister.create(Registries.RECIPE_TYPE, AE2VirtualWell.MODID);

    public static final DeferredHolder<RecipeType<?>, RecipeType<WellDropRecipe>> WELL_DROP_TYPE =
            RECIPE_TYPES.register("well_drop", () -> new RecipeType<WellDropRecipe>() {
                @Override
                public String toString() {
                    return AE2VirtualWell.MODID + ":well_drop";
                }
            });

    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<WellDropRecipe>> WELL_DROP_SERIALIZER =
            SERIALIZERS.register("well_drop", () -> new RecipeSerializer<>(WellDropRecipe.CODEC, WellDropRecipe.STREAM_CODEC));
}
