package de.project.ae2virtualwell.cell;

import appeng.api.config.FuzzyMode;
import appeng.api.ids.AEComponents;
import appeng.api.stacks.AEFluidKey;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKeyType;
import appeng.api.stacks.GenericStack;
import appeng.api.stacks.KeyCounter;
import appeng.api.storage.StorageCells;
import appeng.api.storage.cells.ICellWorkbenchItem;
import appeng.api.storage.cells.StorageCell;
import appeng.api.upgrades.IUpgradeInventory;
import appeng.api.upgrades.UpgradeInventories;
import appeng.core.AEConfig;
import appeng.core.localization.Tooltips;
import appeng.items.contents.CellConfig;
import appeng.items.storage.StorageCellTooltipComponent;
import appeng.util.ConfigInventory;
import de.project.ae2virtualwell.config.VirtualWellConfig;
import de.project.ae2virtualwell.recipe.WellDropRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Consumer;

public class VirtualWellCellItem extends Item implements ICellWorkbenchItem {

    private final WellCellTier tier;

    public VirtualWellCellItem(WellCellTier tier, Properties properties) {
        super(properties.stacksTo(1));
        this.tier = tier;
    }

    public WellCellTier getTier() {
        return tier;
    }

    public int getBytes(ItemStack stack) {
        return tier.getTotalBytes();
    }

    public int getBytesPerType(ItemStack stack) {
        return tier.getBytesPerType();
    }

    public int getTotalTypes(ItemStack stack) {
        return tier.getTotalTypes();
    }

    public double getIdleDrain() {
        return tier.getIdleDrain();
    }

    @Override
    public IUpgradeInventory getUpgrades(ItemStack stack) {
        return UpgradeInventories.forItem(stack, 4);
    }

    @Override
    public ConfigInventory getConfigInventory(ItemStack stack) {
        return CellConfig.create(Set.of(AEKeyType.fluids(), AEKeyType.items()), stack);
    }

    @Override
    public FuzzyMode getFuzzyMode(ItemStack stack) {
        return stack.getOrDefault(AEComponents.STORAGE_CELL_FUZZY_MODE, FuzzyMode.IGNORE_ALL);
    }

    @Override
    public void setFuzzyMode(ItemStack stack, FuzzyMode mode) {
        stack.set(AEComponents.STORAGE_CELL_FUZZY_MODE, mode);
    }

    public static Component getFluidDisplayName(Fluid fluid) {
        try {
            return fluid.getFluidType().getDescription();
        } catch (Throwable ignored) {
            return Component.translatable(fluid.defaultFluidState().createLegacyBlock().getBlock().getDescriptionId());
        }
    }

    @Nullable
    public Fluid getConfiguredFluid(ItemStack stack) {
        List<GenericStack> config = stack.get(AEComponents.STORAGE_CELL_CONFIG_INV);
        if (config != null && !config.isEmpty()) {
            for (GenericStack entry : config) {
                if (entry != null) {
                    if (entry.what() instanceof AEFluidKey fluidKey) {
                        return WellDropRegistry.normalizeFluid(fluidKey.getFluid());
                    } else if (entry.what() instanceof AEItemKey itemKey) {
                        Fluid fluid = WellDropRegistry.extractFluidFromItem(itemKey.toStack());
                        if (fluid != null) {
                            return WellDropRegistry.normalizeFluid(fluid);
                        }
                    }
                }
            }
        }
        return null;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> lines, TooltipFlag flag) {
        super.appendHoverText(stack, context, display, lines, flag);

        StorageCell cell = StorageCells.getCellInventory(stack, null);
        if (cell instanceof VirtualWellCellInventory wellInv) {
            lines.accept(Tooltips.bytesUsed(wellInv.getUsedBytes(), wellInv.getTotalBytes()));
            lines.accept(Tooltips.typesUsed(wellInv.getStoredFluidTypes(), wellInv.getTotalFluidTypes()));
        } else {
            lines.accept(Tooltips.bytesUsed(0, tier.getTotalBytes()));
            lines.accept(Tooltips.typesUsed(0, tier.getTotalTypes()));
        }

        int yieldMb = tier.getGenerationMilliBuckets();
        int intervalTicks = VirtualWellConfig.BASE_TICK_INTERVAL.get();
        double seconds = intervalTicks / 20.0;

        lines.accept(Component.translatable("tooltip.ae2virtualwell.tier", tier.getTierName())
                .withStyle(ChatFormatting.AQUA));
        lines.accept(Component.translatable("tooltip.ae2virtualwell.production", yieldMb, String.format(Locale.ROOT, "%.1f", seconds))
                .withStyle(ChatFormatting.GRAY));

        Fluid configured = getConfiguredFluid(stack);
        if (configured != null) {
            lines.accept(Component.translatable("tooltip.ae2virtualwell.configured_target", getFluidDisplayName(configured))
                    .withStyle(ChatFormatting.DARK_AQUA));
        } else {
            lines.accept(Component.translatable("tooltip.ae2virtualwell.not_configured")
                    .withStyle(ChatFormatting.DARK_GRAY));
        }
    }

    @Override
    public Optional<TooltipComponent> getTooltipImage(ItemStack stack) {
        StorageCell cell = StorageCells.getCellInventory(stack, null);
        if (!(cell instanceof VirtualWellCellInventory wellInv)) {
            return Optional.empty();
        }

        List<ItemStack> upgradeStacks = new ArrayList<>();
        try {
            if (AEConfig.instance().isTooltipShowCellUpgrades()) {
                for (ItemStack upgrade : getUpgrades(stack)) {
                    if (!upgrade.isEmpty()) {
                        upgradeStacks.add(upgrade);
                    }
                }
            }
        } catch (Throwable ignored) {
        }

        List<GenericStack> content = new ArrayList<>();
        try {
            if (AEConfig.instance().isTooltipShowCellContent()) {
                int maxCountShown = AEConfig.instance().getTooltipMaxCellContentShown();
                KeyCounter availableStacks = new KeyCounter();
                wellInv.getAvailableStacks(availableStacks);
                for (var entry : availableStacks) {
                    content.add(new GenericStack(entry.getKey(), entry.getLongValue()));
                }

                content.sort(Comparator.comparingLong(GenericStack::amount).reversed());
                boolean hasMoreContent = content.size() > maxCountShown;
                if (content.size() > maxCountShown) {
                    content = new ArrayList<>(content.subList(0, maxCountShown));
                }
                return Optional.of(new StorageCellTooltipComponent(upgradeStacks, content, hasMoreContent, true));
            }
        } catch (Throwable ignored) {
        }

        return Optional.of(new StorageCellTooltipComponent(upgradeStacks, Collections.emptyList(), false, true));
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        if (player != null && player.isShiftKeyDown()) {
            Level level = context.getLevel();
            BlockPos clickedPos = context.getClickedPos();
            BlockPos targetPos = clickedPos;

            FluidState state = level.getFluidState(clickedPos);
            if (state.isEmpty()) {
                // Check adjacent block in clicked direction
                targetPos = clickedPos.relative(context.getClickedFace());
                state = level.getFluidState(targetPos);
            }

            if (!state.isEmpty()) {
                Fluid fluid = WellDropRegistry.normalizeFluid(state.getType());
                if (WellDropRegistry.isValidFluidTarget(fluid)) {
                    if (!level.isClientSide()) {
                        ItemStack stack = context.getItemInHand();
                        stack.set(AEComponents.STORAGE_CELL_CONFIG_INV, List.of(new GenericStack(AEFluidKey.of(fluid), 1)));
                        player.sendOverlayMessage(Component.translatable("message.ae2virtualwell.sampled_configured",
                                getFluidDisplayName(fluid)).withStyle(ChatFormatting.AQUA));
                    }
                    return InteractionResult.SUCCESS;
                }
            }
        }
        return super.useOn(context);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        InteractionHand otherHand = hand == InteractionHand.MAIN_HAND ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
        ItemStack otherStack = player.getItemInHand(otherHand);

        if (player.isShiftKeyDown()) {
            if (!otherStack.isEmpty()) {
                // Quick-train using fluid container in off-hand
                Fluid fluid = WellDropRegistry.extractFluidFromItem(otherStack);
                if (WellDropRegistry.isValidFluidTarget(fluid)) {
                    if (!level.isClientSide()) {
                        stack.set(AEComponents.STORAGE_CELL_CONFIG_INV, List.of(new GenericStack(AEFluidKey.of(fluid), 1)));
                        player.sendOverlayMessage(Component.translatable("message.ae2virtualwell.configured",
                                getFluidDisplayName(fluid)).withStyle(ChatFormatting.AQUA));
                    }
                    return InteractionResult.SUCCESS;
                }
            } else {
                // Clear configuration
                if (!level.isClientSide()) {
                    stack.remove(AEComponents.STORAGE_CELL_CONFIG_INV);
                    player.sendOverlayMessage(Component.translatable("message.ae2virtualwell.cleared")
                            .withStyle(ChatFormatting.RED));
                }
                return InteractionResult.SUCCESS;
            }
        }

        return super.use(level, player, hand);
    }
}
