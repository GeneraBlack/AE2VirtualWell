package de.project.ae2virtualwell.registry;

import de.project.ae2virtualwell.AE2VirtualWell;
import de.project.ae2virtualwell.cell.VirtualWellCellItem;
import de.project.ae2virtualwell.cell.WellCellTier;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(AE2VirtualWell.MODID);

    // Housing
    public static final DeferredItem<Item> WELL_CELL_HOUSING =
            ITEMS.registerSimpleItem("well_cell_housing");

    // Storage Components
    public static final DeferredItem<Item> WELL_COMPONENT_1K =
            ITEMS.registerSimpleItem("well_cell_component_1k");
    public static final DeferredItem<Item> WELL_COMPONENT_4K =
            ITEMS.registerSimpleItem("well_cell_component_4k");
    public static final DeferredItem<Item> WELL_COMPONENT_16K =
            ITEMS.registerSimpleItem("well_cell_component_16k");
    public static final DeferredItem<Item> WELL_COMPONENT_64K =
            ITEMS.registerSimpleItem("well_cell_component_64k");
    public static final DeferredItem<Item> WELL_COMPONENT_256K =
            ITEMS.registerSimpleItem("well_cell_component_256k");

    // Complete Storage Cells
    public static final DeferredItem<VirtualWellCellItem> WELL_CELL_1K =
            ITEMS.registerItem("well_storage_cell_1k", props -> new VirtualWellCellItem(WellCellTier.TIER_1K, props));
    public static final DeferredItem<VirtualWellCellItem> WELL_CELL_4K =
            ITEMS.registerItem("well_storage_cell_4k", props -> new VirtualWellCellItem(WellCellTier.TIER_4K, props));
    public static final DeferredItem<VirtualWellCellItem> WELL_CELL_16K =
            ITEMS.registerItem("well_storage_cell_16k", props -> new VirtualWellCellItem(WellCellTier.TIER_16K, props));
    public static final DeferredItem<VirtualWellCellItem> WELL_CELL_64K =
            ITEMS.registerItem("well_storage_cell_64k", props -> new VirtualWellCellItem(WellCellTier.TIER_64K, props));
    public static final DeferredItem<VirtualWellCellItem> WELL_CELL_256K =
            ITEMS.registerItem("well_storage_cell_256k", props -> new VirtualWellCellItem(WellCellTier.TIER_256K, props));
}
