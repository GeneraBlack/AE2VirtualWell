package de.project.ae2virtualwell.util;

import appeng.api.ids.AEComponents;
import appeng.api.stacks.AEFluidKey;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.GenericStack;
import appeng.api.upgrades.IUpgradeInventory;
import appeng.api.upgrades.UpgradeInventories;
import appeng.core.definitions.AEItems;
import appeng.util.GenericContainerHelper;
import de.project.ae2virtualwell.cell.partition.WellCellPartition;
import de.project.ae2virtualwell.cell.partition.WellCellPartitionList;
import de.project.ae2virtualwell.recipe.WellDropRegistry;
import de.project.ae2virtualwell.registry.ModDataComponents;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Container;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidUtil;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.*;

public final class VirtualCellAdapter {

    private VirtualCellAdapter() {}

    public record UniversalPartition(
            ResourceLocation targetId,
            boolean isFluid,
            int percent,
            boolean voidSecondary
    ) {
        public static final StreamCodec<RegistryFriendlyByteBuf, UniversalPartition> STREAM_CODEC = StreamCodec.composite(
                ResourceLocation.STREAM_CODEC, UniversalPartition::targetId,
                ByteBufCodecs.BOOL, UniversalPartition::isFluid,
                ByteBufCodecs.VAR_INT, UniversalPartition::percent,
                ByteBufCodecs.BOOL, UniversalPartition::voidSecondary,
                UniversalPartition::new
        );
    }

    public static Item getItem(ResourceLocation id) {
        if (id == null) return Items.AIR;
        return BuiltInRegistries.ITEM.containsKey(id) ? BuiltInRegistries.ITEM.get(id) : Items.AIR;
    }

    public static Fluid getFluid(ResourceLocation id) {
        if (id == null) return Fluids.EMPTY;
        return BuiltInRegistries.FLUID.containsKey(id) ? BuiltInRegistries.FLUID.get(id) : Fluids.EMPTY;
    }

    public static DataComponentType<?> getDataComponentType(ResourceLocation id) {
        if (id == null) return null;
        return BuiltInRegistries.DATA_COMPONENT_TYPE.containsKey(id) ? BuiltInRegistries.DATA_COMPONENT_TYPE.get(id) : null;
    }

    public static boolean isVirtualStorageCell(ItemStack stack) {
        if (stack.isEmpty()) return false;
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        String ns = id.getNamespace();
        String path = id.getPath();
        return (ns.equals("ae2virtualmine") || ns.equals("ae2virtualgarden")
                || ns.equals("ae2virtualbattle") || ns.equals("ae2virtualwell"))
                && path.contains("_storage_cell_");
    }

    public static boolean isFluidCell(ItemStack stack) {
        if (stack.isEmpty()) return false;
        return BuiltInRegistries.ITEM.getKey(stack.getItem()).getNamespace().equals("ae2virtualwell");
    }

    public static boolean isVoidSecondaryCard(ItemStack stack) {
        if (stack.isEmpty()) return false;
        if (stack.is(AEItems.VOID_CARD.asItem())) return true;
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        return id.getNamespace().startsWith("ae2virtual") && id.getPath().equals("void_secondary_card");
    }

    public static boolean hasVoidSecondaryCard(IUpgradeInventory upgrades) {
        if (upgrades == null) return false;
        if (upgrades.isInstalled(AEItems.VOID_CARD.asItem())) return true;
        for (int i = 0; i < upgrades.size(); i++) {
            ItemStack stack = upgrades.getStackInSlot(i);
            if (isVoidSecondaryCard(stack)) return true;
        }
        return false;
    }

    public static boolean hasVoidCardInstalled(Container upgradeContainer, ItemStack cell) {
        if (upgradeContainer != null && upgradeContainer.getContainerSize() > 4) {
            if (isVoidSecondaryCard(upgradeContainer.getItem(4))) {
                return true;
            }
        }
        if (cell.isEmpty()) return false;
        var upgrades = UpgradeInventories.forItem(cell, 5);
        return hasVoidSecondaryCard(upgrades);
    }

    public static long getCellTotalBytes(ItemStack cell) {
        if (cell.isEmpty()) return 0;
        String path = BuiltInRegistries.ITEM.getKey(cell.getItem()).getPath();
        if (path.startsWith("256k")) return 262144;
        if (path.startsWith("64k")) return 65536;
        if (path.startsWith("16k")) return 16384;
        if (path.startsWith("4k")) return 4096;
        return 1024;
    }

    public static String getCellTierName(ItemStack cell) {
        if (cell.isEmpty()) return "";
        String path = BuiltInRegistries.ITEM.getKey(cell.getItem()).getPath();
        if (path.startsWith("256k")) return "256k";
        if (path.startsWith("64k")) return "64k";
        if (path.startsWith("16k")) return "16k";
        if (path.startsWith("4k")) return "4k";
        return "1k";
    }

    public static List<UniversalPartition> readPartitions(ItemStack cell) {
        if (cell.isEmpty() || !isVirtualStorageCell(cell)) {
            return List.of();
        }

        ResourceLocation cellId = BuiltInRegistries.ITEM.getKey(cell.getItem());
        String ns = cellId.getNamespace();

        // Local mod fast path
        if (ns.equals("ae2virtualwell")) {
            if (cell.has(ModDataComponents.PARTITIONS.get())) {
                WellCellPartitionList list = cell.get(ModDataComponents.PARTITIONS.get());
                if (list != null && !list.isEmpty()) {
                    List<UniversalPartition> result = new ArrayList<>();
                    for (WellCellPartition p : list.partitions()) {
                        result.add(new UniversalPartition(
                                BuiltInRegistries.FLUID.getKey(p.target()),
                                true,
                                p.percent(),
                                p.voidSecondary()
                        ));
                    }
                    return result;
                }
            }
            return List.of();
        }

        // Foreign mod path via component lookup & reflection
        DataComponentType<?> compType = getDataComponentType(ResourceLocation.fromNamespaceAndPath(ns, "partitions"));
        if (compType != null && cell.has(compType)) {
            Object listObj = cell.get(compType);
            if (listObj != null) {
                try {
                    Method partitionsMethod = listObj.getClass().getMethod("partitions");
                    List<?> list = (List<?>) partitionsMethod.invoke(listObj);
                    if (list != null) {
                        List<UniversalPartition> result = new ArrayList<>();
                        for (Object p : list) {
                            Method targetMethod = p.getClass().getMethod("target");
                            Method percentMethod = p.getClass().getMethod("percent");
                            Method voidMethod = p.getClass().getMethod("voidSecondary");
                            Object target = targetMethod.invoke(p);
                            int percent = (Integer) percentMethod.invoke(p);
                            boolean voidSec = (Boolean) voidMethod.invoke(p);
                            boolean isFluid = (target instanceof Fluid);
                            ResourceLocation targetId = isFluid
                                    ? BuiltInRegistries.FLUID.getKey((Fluid) target)
                                    : BuiltInRegistries.ITEM.getKey((Item) target);
                            result.add(new UniversalPartition(targetId, isFluid, percent, voidSec));
                        }
                        return result;
                    }
                } catch (Exception e) {
                    // Ignore or log debug
                }
            }
        }

        return List.of();
    }

    @SuppressWarnings("unchecked")
    public static void writePartitions(ItemStack cell, List<UniversalPartition> partitions) {
        if (cell.isEmpty() || !isVirtualStorageCell(cell)) {
            return;
        }

        ResourceLocation cellId = BuiltInRegistries.ITEM.getKey(cell.getItem());
        String ns = cellId.getNamespace();

        // Local mod fast path
        if (ns.equals("ae2virtualwell")) {
            if (partitions == null || partitions.isEmpty()) {
                cell.remove(ModDataComponents.PARTITIONS.get());
                cell.remove(AEComponents.STORAGE_CELL_CONFIG_INV);
            } else {
                List<WellCellPartition> list = new ArrayList<>();
                for (UniversalPartition up : partitions) {
                    Fluid f = getFluid(up.targetId());
                    if (f != null && f != Fluids.EMPTY) {
                        list.add(new WellCellPartition(f, up.percent(), up.voidSecondary()));
                    }
                }
                if (list.isEmpty()) {
                    cell.remove(ModDataComponents.PARTITIONS.get());
                    cell.remove(AEComponents.STORAGE_CELL_CONFIG_INV);
                } else {
                    cell.set(ModDataComponents.PARTITIONS.get(), new WellCellPartitionList(list));
                    cell.set(AEComponents.STORAGE_CELL_CONFIG_INV, List.of(new GenericStack(AEFluidKey.of(list.get(0).target()), 1)));
                }
            }
            return;
        }

        // Foreign mod reflection path
        DataComponentType compType = getDataComponentType(ResourceLocation.fromNamespaceAndPath(ns, "partitions"));
        if (compType == null) return;

        if (partitions == null || partitions.isEmpty()) {
            cell.remove(compType);
            cell.remove(AEComponents.STORAGE_CELL_CONFIG_INV);
            return;
        }

        try {
            String pClassName = switch (ns) {
                case "ae2virtualmine" -> "de.project.ae2virtualmine.cell.partition.MineCellPartition";
                case "ae2virtualgarden" -> "de.project.ae2virtualgarden.cell.partition.GardenCellPartition";
                case "ae2virtualbattle" -> "de.project.ae2virtualbattle.cell.partition.BattleCellPartition";
                case "ae2virtualwell" -> "de.project.ae2virtualwell.cell.partition.WellCellPartition";
                default -> null;
            };
            String lClassName = switch (ns) {
                case "ae2virtualmine" -> "de.project.ae2virtualmine.cell.partition.MineCellPartitionList";
                case "ae2virtualgarden" -> "de.project.ae2virtualgarden.cell.partition.GardenCellPartitionList";
                case "ae2virtualbattle" -> "de.project.ae2virtualbattle.cell.partition.BattleCellPartitionList";
                case "ae2virtualwell" -> "de.project.ae2virtualwell.cell.partition.WellCellPartitionList";
                default -> null;
            };
            if (pClassName == null) return;

            Class<?> pClass = Class.forName(pClassName);
            Class<?> lClass = Class.forName(lClassName);
            boolean isFluid = ns.equals("ae2virtualwell");
            Constructor<?> pCtor = pClass.getConstructor(isFluid ? Fluid.class : Item.class, int.class, boolean.class);
            Constructor<?> lCtor = lClass.getConstructor(List.class);

            List<Object> partitionObjects = new ArrayList<>();
            Object firstTarget = null;
            for (UniversalPartition up : partitions) {
                Object target = isFluid ? getFluid(up.targetId()) : getItem(up.targetId());
                if (target != null && target != (isFluid ? Fluids.EMPTY : Items.AIR)) {
                    Object pObj = pCtor.newInstance(target, up.percent(), up.voidSecondary());
                    partitionObjects.add(pObj);
                    if (firstTarget == null) firstTarget = target;
                }
            }

            if (partitionObjects.isEmpty()) {
                cell.remove(compType);
                cell.remove(AEComponents.STORAGE_CELL_CONFIG_INV);
            } else {
                Object lObj = lCtor.newInstance(partitionObjects);
                cell.set(compType, lObj);
                if (firstTarget instanceof Fluid fluid) {
                    cell.set(AEComponents.STORAGE_CELL_CONFIG_INV, List.of(new GenericStack(AEFluidKey.of(fluid), 1)));
                } else if (firstTarget instanceof Item item) {
                    cell.set(AEComponents.STORAGE_CELL_CONFIG_INV, List.of(new GenericStack(AEItemKey.of(item), 1)));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static boolean isValidTarget(ItemStack cell, ResourceLocation targetId, Level level) {
        if (cell.isEmpty() || targetId == null) return false;
        String ns = BuiltInRegistries.ITEM.getKey(cell.getItem()).getNamespace();

        if (ns.equals("ae2virtualwell")) {
            Fluid fluid = getFluid(targetId);
            if (fluid == null || fluid == Fluids.EMPTY) return false;
            return WellDropRegistry.isValidFluidTarget(fluid, level);
        }

        if (ns.equals("ae2virtualmine")) {
            Item item = getItem(targetId);
            if (item == null || item == Items.AIR) return false;
            try {
                Class<?> clazz = Class.forName("de.project.ae2virtualmine.registry.MineDropRegistry");
                Method m = clazz.getMethod("isValidMiningTarget", Item.class, Level.class);
                return (Boolean) m.invoke(null, item, level);
            } catch (Exception ignored) {
                return true;
            }
        }

        if (ns.equals("ae2virtualgarden")) {
            Item item = getItem(targetId);
            if (item == null || item == Items.AIR) return false;
            try {
                Class<?> clazz = Class.forName("de.project.ae2virtualgarden.recipe.GardenDropRegistry");
                Method m = clazz.getMethod("isValidSeed", Item.class, Level.class);
                return (Boolean) m.invoke(null, item, level);
            } catch (Exception ignored) {
                return true;
            }
        }

        if (ns.equals("ae2virtualbattle")) {
            Item item = getItem(targetId);
            if (item == null || item == Items.AIR) return false;
            try {
                Class<?> clazz = Class.forName("de.project.ae2virtualbattle.recipe.BattleDropRegistry");
                Method m = clazz.getMethod("isValidBattleTarget", Item.class, Level.class);
                return (Boolean) m.invoke(null, item, level);
            } catch (Exception ignored) {
                return true;
            }
        }

        return false;
    }

    public static Fluid extractFluidFromItem(ItemStack stack) {
        if (stack.isEmpty()) {
            return null;
        }
        GenericStack contained = GenericContainerHelper.getContainedFluidStack(stack);
        if (contained != null && contained.what() instanceof AEFluidKey fluidKey) {
            return normalizeFluid(fluidKey.getFluid());
        }
        Optional<FluidStack> fluidContained = FluidUtil.getFluidContained(stack);
        if (fluidContained.isPresent() && !fluidContained.get().isEmpty()) {
            return normalizeFluid(fluidContained.get().getFluid());
        }
        if (stack.is(Items.WATER_BUCKET)) {
            return Fluids.WATER;
        }
        if (stack.is(Items.LAVA_BUCKET)) {
            return Fluids.LAVA;
        }
        if (stack.getItem() instanceof BucketItem bucket) {
            return normalizeFluid(bucket.content);
        }
        return null;
    }

    public static Fluid normalizeFluid(Fluid fluid) {
        if (fluid instanceof FlowingFluid flowing) {
            return flowing.getSource();
        }
        return fluid;
    }

    public static ItemStack getTargetRenderStack(ResourceLocation targetId, boolean isFluid) {
        if (targetId == null) return ItemStack.EMPTY;
        if (isFluid) {
            Fluid f = getFluid(targetId);
            return (f != null && f != Fluids.EMPTY) ? new ItemStack(f.getBucket()) : ItemStack.EMPTY;
        } else {
            Item it = getItem(targetId);
            return (it != null && it != Items.AIR) ? new ItemStack(it) : ItemStack.EMPTY;
        }
    }

    public static Component getTargetDisplayName(ResourceLocation targetId, boolean isFluid) {
        if (targetId == null) return Component.literal("[Select]");
        if (isFluid) {
            Fluid f = getFluid(targetId);
            if (f == null || f == Fluids.EMPTY) return Component.literal("[Select]");
            try {
                return f.getFluidType().getDescription();
            } catch (Throwable ignored) {
                return Component.translatable(f.defaultFluidState().createLegacyBlock().getBlock().getDescriptionId());
            }
        } else {
            Item it = getItem(targetId);
            if (it == null || it == Items.AIR) return Component.literal("[Select]");
            return new ItemStack(it).getHoverName();
        }
    }
}
