package de.project.ae2virtualwell.registry;

import de.project.ae2virtualwell.AE2VirtualWell;
import de.project.ae2virtualwell.menu.VirtualPartitionerMenu;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModMenus {
    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(Registries.MENU, AE2VirtualWell.MODID);

    public static final DeferredHolder<MenuType<?>, MenuType<VirtualPartitionerMenu>> PARTITIONER_MENU =
            MENUS.register("virtual_partitioner", () -> IMenuTypeExtension.create(VirtualPartitionerMenu::new));
}
