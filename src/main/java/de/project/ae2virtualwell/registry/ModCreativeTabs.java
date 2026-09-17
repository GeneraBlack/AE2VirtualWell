package de.project.ae2virtualwell.registry;

import de.project.ae2virtualwell.AE2VirtualWell;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, AE2VirtualWell.MODID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> TAB =
            CREATIVE_MODE_TABS.register("ae2virtualwell_tab", () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.ae2virtualwell"))
                    .icon(() -> new ItemStack(ModItems.WELL_CELL_4K.get()))
                    .displayItems((parameters, output) -> {
                        output.accept(ModItems.WELL_CELL_HOUSING.get());
                        output.accept(ModItems.WELL_COMPONENT_1K.get());
                        output.accept(ModItems.WELL_COMPONENT_4K.get());
                        output.accept(ModItems.WELL_COMPONENT_16K.get());
                        output.accept(ModItems.WELL_COMPONENT_64K.get());
                        output.accept(ModItems.WELL_COMPONENT_256K.get());
                        output.accept(ModItems.WELL_CELL_1K.get());
                        output.accept(ModItems.WELL_CELL_4K.get());
                        output.accept(ModItems.WELL_CELL_16K.get());
                        output.accept(ModItems.WELL_CELL_64K.get());
                        output.accept(ModItems.WELL_CELL_256K.get());
                    })
                    .build());
}
