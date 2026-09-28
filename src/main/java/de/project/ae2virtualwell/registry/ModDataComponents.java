package de.project.ae2virtualwell.registry;

import de.project.ae2virtualwell.AE2VirtualWell;
import de.project.ae2virtualwell.cell.partition.WellCellPartitionList;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModDataComponents {
    public static final DeferredRegister<DataComponentType<?>> DATA_COMPONENTS =
            DeferredRegister.create(Registries.DATA_COMPONENT_TYPE, AE2VirtualWell.MODID);

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<WellCellPartitionList>> PARTITIONS =
            DATA_COMPONENTS.register("partitions", () -> DataComponentType.<WellCellPartitionList>builder()
                    .persistent(WellCellPartitionList.CODEC)
                    .networkSynchronized(WellCellPartitionList.STREAM_CODEC)
                    .build());
}
