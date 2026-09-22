package de.project.ae2virtualwell.recipe;

import appeng.api.stacks.AEFluidKey;
import appeng.api.stacks.GenericStack;
import appeng.util.GenericContainerHelper;
import de.project.ae2virtualwell.registry.ModRecipes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidUtil;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class WellDropRegistry {

    private static final Map<Fluid, List<WellDropEntry>> BUILTIN_DROPS = new HashMap<>();
    private static final Map<Fluid, List<WellDropEntry>> DYNAMIC_CACHE = new HashMap<>();

    static {
        registerDefaults();
    }

    private static void registerDefaults() {
        // Water
        BUILTIN_DROPS.put(Fluids.WATER, List.of(
                new WellDropEntry(Fluids.WATER, 100, 1000, 1000)
        ));

        // Lava
        BUILTIN_DROPS.put(Fluids.LAVA, List.of(
                new WellDropEntry(Fluids.LAVA, 100, 1000, 1000)
        ));

        // Check if milk is registered as a fluid (e.g. neoforge:milk)
        for (Fluid fluid : BuiltInRegistries.FLUID) {
            Identifier id = BuiltInRegistries.FLUID.getKey(fluid);
            if (id != null && id.getPath().equals("milk")) {
                BUILTIN_DROPS.put(normalizeFluid(fluid), List.of(
                        new WellDropEntry(normalizeFluid(fluid), 100, 1000, 1000)
                ));
            }
        }
    }

    public static Fluid normalizeFluid(Fluid fluid) {
        if (fluid instanceof FlowingFluid flowing) {
            return flowing.getSource();
        }
        return fluid;
    }

    public static boolean isValidFluidTarget(@Nullable Fluid fluid, @Nullable Level level) {
        if (fluid == null || fluid == Fluids.EMPTY) {
            return false;
        }
        Fluid normalized = normalizeFluid(fluid);

        // 1. Built-in defaults (Water, Lava, Milk)
        if (BUILTIN_DROPS.containsKey(normalized)) {
            return true;
        }

        // 2. Dynamic cache
        if (DYNAMIC_CACHE.containsKey(normalized)) {
            return !DYNAMIC_CACHE.get(normalized).isEmpty();
        }

        // 3. Custom datapack recipes
        net.minecraft.server.MinecraftServer server = null;
        if (level != null && level.getServer() != null) {
            server = level.getServer();
        } else if (net.neoforged.neoforge.server.ServerLifecycleHooks.getCurrentServer() != null) {
            server = net.neoforged.neoforge.server.ServerLifecycleHooks.getCurrentServer();
        }

        Level queryLevel = level != null ? level : (server != null ? server.overworld() : null);
        if (server != null && queryLevel != null) {
            ItemStack bucketStack = new ItemStack(normalized.getBucket());
            if (!bucketStack.isEmpty()) {
                SingleRecipeInput input = new SingleRecipeInput(bucketStack);
                Optional<RecipeHolder<WellDropRecipe>> match = server.getRecipeManager().getRecipeFor(
                        ModRecipes.WELL_DROP_TYPE.get(),
                        input,
                        queryLevel
                );
                if (match.isPresent()) {
                    return true;
                }
            }
        }

        // 4. Dynamic discovery fallback (only if enabled in config)
        return de.project.ae2virtualwell.config.VirtualWellConfig.isDynamicFallbackEnabled();
    }

    public static boolean isValidFluidTarget(@Nullable Fluid fluid) {
        return isValidFluidTarget(fluid, null);
    }

    @Nullable
    public static Fluid extractFluidFromItem(ItemStack stack) {
        if (stack.isEmpty()) {
            return null;
        }

        // 1. Check generic container helper / NeoForge fluid capability
        GenericStack contained = GenericContainerHelper.getContainedFluidStack(stack);
        if (contained != null && contained.what() instanceof AEFluidKey fluidKey) {
            return normalizeFluid(fluidKey.getFluid());
        }

        Optional<FluidStack> fluidContained = FluidUtil.getFluidContained(stack);
        if (fluidContained.isPresent() && !fluidContained.get().isEmpty()) {
            return normalizeFluid(fluidContained.get().getFluid());
        }

        // 2. Direct vanilla item checks
        if (stack.is(Items.WATER_BUCKET)) {
            return Fluids.WATER;
        }
        if (stack.is(Items.LAVA_BUCKET)) {
            return Fluids.LAVA;
        }
        if (stack.is(Items.MILK_BUCKET)) {
            for (Fluid fluid : BuiltInRegistries.FLUID) {
                Identifier id = BuiltInRegistries.FLUID.getKey(fluid);
                if (id != null && id.getPath().equals("milk")) {
                    return normalizeFluid(fluid);
                }
            }
        }

        return null;
    }

    public static List<WellDropEntry> getDropEntries(Fluid target, @Nullable Level level) {
        if (target == null || target == Fluids.EMPTY) {
            return Collections.emptyList();
        }

        Fluid normalized = normalizeFluid(target);

        if (DYNAMIC_CACHE.containsKey(normalized)) {
            return DYNAMIC_CACHE.get(normalized);
        }

        if (!isValidFluidTarget(normalized, level)) {
            return Collections.emptyList();
        }

        // 1. Check custom datapack recipes using fluid's bucket item (takes priority over builtin defaults)
        net.minecraft.server.MinecraftServer server = null;
        if (level != null && level.getServer() != null) {
            server = level.getServer();
        } else if (net.neoforged.neoforge.server.ServerLifecycleHooks.getCurrentServer() != null) {
            server = net.neoforged.neoforge.server.ServerLifecycleHooks.getCurrentServer();
        }

        Level queryLevel = level != null ? level : (server != null ? server.overworld() : null);

        if (server != null && queryLevel != null) {
            ItemStack bucketStack = new ItemStack(normalized.getBucket());
            if (!bucketStack.isEmpty()) {
                SingleRecipeInput input = new SingleRecipeInput(bucketStack);
                Optional<RecipeHolder<WellDropRecipe>> match = server.getRecipeManager().getRecipeFor(
                        ModRecipes.WELL_DROP_TYPE.get(),
                        input,
                        queryLevel
                );
                if (match.isPresent()) {
                    List<WellDropEntry> recipeDrops = match.get().value().drops();
                    DYNAMIC_CACHE.put(normalized, recipeDrops);
                    return recipeDrops;
                }
            }
        }

        // 2. Built-in hardcoded defaults (Water, Lava, Milk) as fallback
        if (BUILTIN_DROPS.containsKey(normalized)) {
            List<WellDropEntry> builtin = BUILTIN_DROPS.get(normalized);
            if (queryLevel != null) {
                DYNAMIC_CACHE.put(normalized, builtin);
            }
            return builtin;
        }

        // 3. Dynamic discovery: only if dynamic fallback is enabled in config
        if (de.project.ae2virtualwell.config.VirtualWellConfig.isDynamicFallbackEnabled()) {
            List<WellDropEntry> generated = List.of(
                    new WellDropEntry(normalized, 100, 1000, 1000)
            );
            if (queryLevel != null) {
                DYNAMIC_CACHE.put(normalized, generated);
            }
            return generated;
        }

        if (queryLevel != null) {
            DYNAMIC_CACHE.put(normalized, Collections.emptyList());
        }
        return Collections.emptyList();
    }

    @Nullable
    public static WellDropEntry rollDrop(List<WellDropEntry> entries, RandomSource random) {
        if (entries == null || entries.isEmpty()) {
            return null;
        }

        int totalWeight = 0;
        for (WellDropEntry entry : entries) {
            totalWeight += entry.weight();
        }

        if (totalWeight <= 0) {
            return null;
        }

        int roll = random.nextInt(totalWeight);
        int current = 0;
        for (WellDropEntry entry : entries) {
            current += entry.weight();
            if (roll < current) {
                return entry;
            }
        }

        return entries.get(0);
    }

    public static void clearCache() {
        DYNAMIC_CACHE.clear();
    }
}
