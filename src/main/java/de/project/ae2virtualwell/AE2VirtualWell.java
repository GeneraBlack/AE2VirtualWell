package de.project.ae2virtualwell;

import appeng.api.networking.GridServices;
import appeng.api.storage.StorageCells;
import de.project.ae2virtualwell.cell.VirtualWellCellHandler;
import de.project.ae2virtualwell.config.VirtualWellConfig;
import de.project.ae2virtualwell.network.IVirtualWellGridService;
import de.project.ae2virtualwell.network.VirtualWellGridService;
import de.project.ae2virtualwell.registry.ModCreativeTabs;
import de.project.ae2virtualwell.registry.ModItems;
import de.project.ae2virtualwell.registry.ModRecipes;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mod(AE2VirtualWell.MODID)
public class AE2VirtualWell {
    public static final String MODID = "ae2virtualwell";
    public static final Logger LOGGER = LoggerFactory.getLogger(MODID);

    public AE2VirtualWell(IEventBus modEventBus, ModContainer modContainer) {
        LOGGER.info("Initializing AE2 Virtual Well");

        // Register Config
        modContainer.registerConfig(ModConfig.Type.COMMON, VirtualWellConfig.SPEC);

        // Register Registries
        ModItems.ITEMS.register(modEventBus);
        ModCreativeTabs.CREATIVE_MODE_TABS.register(modEventBus);
        ModRecipes.SERIALIZERS.register(modEventBus);
        ModRecipes.RECIPE_TYPES.register(modEventBus);

        // Register Grid Service during mod init
        GridServices.register(IVirtualWellGridService.class, VirtualWellGridService.class);

        // Register Setup Listener
        modEventBus.addListener(this::commonSetup);
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            LOGGER.info("Registering AE2 Virtual Well Storage Cell Handler");
            StorageCells.addCellHandler(new VirtualWellCellHandler());
        });
    }
}
