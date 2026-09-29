package de.project.ae2virtualwell;

import appeng.api.networking.GridServices;
import appeng.api.storage.StorageCells;
import appeng.core.definitions.AEItems;
import de.project.ae2virtualwell.cell.VirtualWellCellHandler;
import de.project.ae2virtualwell.config.VirtualWellConfig;
import de.project.ae2virtualwell.network.IVirtualWellGridService;
import de.project.ae2virtualwell.network.VirtualWellGridService;
import de.project.ae2virtualwell.registry.ModBlockEntities;
import de.project.ae2virtualwell.registry.ModBlocks;
import de.project.ae2virtualwell.registry.ModCreativeTabs;
import de.project.ae2virtualwell.registry.ModDataComponents;
import de.project.ae2virtualwell.registry.ModItems;
import de.project.ae2virtualwell.registry.ModMenus;
import de.project.ae2virtualwell.registry.ModRecipes;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.fml.loading.FMLEnvironment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

@Mod(AE2VirtualWell.MODID)
public class AE2VirtualWell {
    public static final String MODID = "ae2virtualwell";
    public static final Logger LOGGER = LoggerFactory.getLogger(MODID);

    public AE2VirtualWell(IEventBus modEventBus, ModContainer modContainer) {
        LOGGER.info("Initializing AE2 Virtual Well");

        // Register Config
        modContainer.registerConfig(ModConfig.Type.COMMON, VirtualWellConfig.SPEC);

        // Register Registries
        ModBlocks.BLOCKS.register(modEventBus);
        ModBlockEntities.BLOCK_ENTITIES.register(modEventBus);
        ModMenus.MENUS.register(modEventBus);
        ModItems.ITEMS.register(modEventBus);
        ModCreativeTabs.CREATIVE_MODE_TABS.register(modEventBus);
        ModRecipes.SERIALIZERS.register(modEventBus);
        ModRecipes.RECIPE_TYPES.register(modEventBus);
        ModDataComponents.DATA_COMPONENTS.register(modEventBus);

        // Register Grid Service during mod init
        GridServices.register(IVirtualWellGridService.class, VirtualWellGridService.class);

        // Register Setup Listener
        modEventBus.addListener(this::commonSetup);
        modEventBus.addListener(de.project.ae2virtualwell.network.VirtualPartitionerNetworking::onRegisterPayloadHandlers);

        // Guard client-only screen registration
        if (FMLEnvironment.getDist().isClient()) {
            modEventBus.addListener(de.project.ae2virtualwell.client.VirtualPartitionerClient::onRegisterMenuScreens);
        }

        // Refresh recipe cache and clear dynamic cache when tags/datapacks update
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.event.TagsUpdatedEvent event) -> {
            de.project.ae2virtualwell.recipe.WellDropRegistry.clearCache();
            var server = net.neoforged.neoforge.server.ServerLifecycleHooks.getCurrentServer();
            if (server != null) {
                de.project.ae2virtualwell.recipe.WellDropRegistry.refreshRecipeCache(server.getRecipeManager());
                LOGGER.info("AE2 Virtual Well: Refreshed recipe cache");
            }
        });
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            LOGGER.info("Registering AE2 Virtual Well Storage Cell Handler");
            StorageCells.addCellHandler(new VirtualWellCellHandler());

            // Register Upgrades on all Well Storage Cells
            for (var cell : List.of(
                    ModItems.WELL_CELL_1K,
                    ModItems.WELL_CELL_4K,
                    ModItems.WELL_CELL_16K,
                    ModItems.WELL_CELL_64K,
                    ModItems.WELL_CELL_256K
            )) {
                appeng.api.upgrades.Upgrades.add(AEItems.SPEED_CARD.asItem(), cell.get(), 4);
                appeng.api.upgrades.Upgrades.add(ModItems.VOID_SECONDARY_CARD.get(), cell.get(), 1);
                appeng.api.upgrades.Upgrades.add(AEItems.VOID_CARD.asItem(), cell.get(), 1);

                for (String ns : List.of("ae2virtualmine", "ae2virtualgarden", "ae2virtualbattle")) {
                    net.minecraft.world.item.Item sisterCard = net.minecraft.core.registries.BuiltInRegistries.ITEM.get(
                            net.minecraft.resources.Identifier.fromNamespaceAndPath(ns, "void_secondary_card")
                    ).map(net.minecraft.core.Holder::value).orElse(net.minecraft.world.item.Items.AIR);
                    if (sisterCard != net.minecraft.world.item.Items.AIR) {
                        appeng.api.upgrades.Upgrades.add(sisterCard, cell.get(), 1);
                    }
                }
            }
        });
    }
}
