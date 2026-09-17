package de.project.ae2virtualwell.registry;

import de.project.ae2virtualwell.AE2VirtualWell;
import de.project.ae2virtualwell.cell.VirtualWellCellItem;
import de.project.ae2virtualwell.cell.WellCellTier;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(AE2VirtualWell.MODID);

    // Housing
    public static final DeferredHolder<Item, Item> WELL_CELL_HOUSING =
            ITEMS.register("well_cell_housing", () -> new Item(new Item.Properties()));

    // Storage Components
    public static final DeferredHolder<Item, Item> WELL_COMPONENT_1K =
            ITEMS.register("well_cell_component_1k", () -> new Item(new Item.Properties()));
    public static final DeferredHolder<Item, Item> WELL_COMPONENT_4K =
            ITEMS.register("well_cell_component_4k", () -> new Item(new Item.Properties()));
    public static final DeferredHolder<Item, Item> WELL_COMPONENT_16K =
            ITEMS.register("well_cell_component_16k", () -> new Item(new Item.Properties()));
    public static final DeferredHolder<Item, Item> WELL_COMPONENT_64K =
            ITEMS.register("well_cell_component_64k", () -> new Item(new Item.Properties()));
    public static final DeferredHolder<Item, Item> WELL_COMPONENT_256K =
            ITEMS.register("well_cell_component_256k", () -> new Item(new Item.Properties()));

    // Complete Storage Cells
    public static final DeferredHolder<Item, VirtualWellCellItem> WELL_CELL_1K =
            ITEMS.register("well_storage_cell_1k", () -> new VirtualWellCellItem(WellCellTier.TIER_1K, new Item.Properties()));
    public static final DeferredHolder<Item, VirtualWellCellItem> WELL_CELL_4K =
            ITEMS.register("well_storage_cell_4k", () -> new VirtualWellCellItem(WellCellTier.TIER_4K, new Item.Properties()));
    public static final DeferredHolder<Item, VirtualWellCellItem> WELL_CELL_16K =
            ITEMS.register("well_storage_cell_16k", () -> new VirtualWellCellItem(WellCellTier.TIER_16K, new Item.Properties()));
    public static final DeferredHolder<Item, VirtualWellCellItem> WELL_CELL_64K =
            ITEMS.register("well_storage_cell_64k", () -> new VirtualWellCellItem(WellCellTier.TIER_64K, new Item.Properties()));
    public static final DeferredHolder<Item, VirtualWellCellItem> WELL_CELL_256K =
            ITEMS.register("well_storage_cell_256k", () -> new VirtualWellCellItem(WellCellTier.TIER_256K, new Item.Properties()));
}
