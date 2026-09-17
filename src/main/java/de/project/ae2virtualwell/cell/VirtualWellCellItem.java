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
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import org.jetbrains.annotations.Nullable;

import java.util.*;

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

    public static Component getFluidDisplayName(@Nullable Fluid fluid) {
        if (fluid == null) {
            return Component.empty();
        }
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
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> lines, TooltipFlag flag) {
        super.appendHoverText(stack, context, lines, flag);

        StorageCell cell = StorageCells.getCellInventory(stack, null);
        if (cell instanceof VirtualWellCellInventory wellInv) {
            lines.add(Tooltips.bytesUsed(wellInv.getUsedBytes(), wellInv.getTotalBytes()));
            lines.add(Tooltips.typesUsed(wellInv.getStoredFluidTypes(), wellInv.getTotalFluidTypes()));
        } else {
            lines.add(Tooltips.bytesUsed(0, tier.getTotalBytes()));
            lines.add(Tooltips.typesUsed(0, tier.getTotalTypes()));
        }

        int yieldMb = tier.getGenerationMilliBuckets();
        int intervalTicks = VirtualWellConfig.BASE_TICK_INTERVAL.get();
        double seconds = intervalTicks / 20.0;

        lines.add(Component.translatable("tooltip.ae2virtualwell.tier", tier.getTierName())
                .withStyle(ChatFormatting.AQUA));
        lines.add(Component.translatable("tooltip.ae2virtualwell.production", yieldMb, String.format(Locale.ROOT, "%.1f", seconds))
                .withStyle(ChatFormatting.GRAY));

        Fluid configured = getConfiguredFluid(stack);
        if (configured != null) {
            lines.add(Component.translatable("tooltip.ae2virtualwell.configured_target", getFluidDisplayName(configured))
                    .withStyle(ChatFormatting.DARK_AQUA));
        } else {
            lines.add(Component.translatable("tooltip.ae2virtualwell.not_configured")
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
            BlockState blockState = level.getBlockState(clickedPos);

            Fluid fluid = null;
            if (blockState.is(Blocks.WATER_CAULDRON)) {
                fluid = net.minecraft.world.level.material.Fluids.WATER;
            } else if (blockState.is(Blocks.LAVA_CAULDRON)) {
                fluid = net.minecraft.world.level.material.Fluids.LAVA;
            } else {
                FluidState state = level.getFluidState(clickedPos);
                if (state.isEmpty()) {
                    // Check adjacent block in clicked direction
                    BlockPos targetPos = clickedPos.relative(context.getClickedFace());
                    state = level.getFluidState(targetPos);
                }
                if (!state.isEmpty()) {
                    fluid = WellDropRegistry.normalizeFluid(state.getType());
                }
            }

            if (WellDropRegistry.isValidFluidTarget(fluid)) {
                if (!level.isClientSide()) {
                    ItemStack stack = context.getItemInHand();
                    stack.set(AEComponents.STORAGE_CELL_CONFIG_INV, List.of(new GenericStack(AEFluidKey.of(fluid), 1)));
                    player.displayClientMessage(Component.translatable("message.ae2virtualwell.sampled_configured",
                            getFluidDisplayName(fluid)).withStyle(ChatFormatting.AQUA), true);
                }
                return InteractionResult.SUCCESS;
            }
        }
        return super.useOn(context);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        InteractionHand otherHand = hand == InteractionHand.MAIN_HAND ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
        ItemStack otherStack = player.getItemInHand(otherHand);

        if (player.isShiftKeyDown()) {
            // 1. Quick-train using fluid container in off-hand
            if (!otherStack.isEmpty()) {
                Fluid fluid = WellDropRegistry.extractFluidFromItem(otherStack);
                if (WellDropRegistry.isValidFluidTarget(fluid)) {
                    if (!level.isClientSide()) {
                        stack.set(AEComponents.STORAGE_CELL_CONFIG_INV, List.of(new GenericStack(AEFluidKey.of(fluid), 1)));
                        player.displayClientMessage(Component.translatable("message.ae2virtualwell.configured",
                                getFluidDisplayName(fluid)).withStyle(ChatFormatting.AQUA), true);
                    }
                    return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
                }
            }

            // 2. Sample in-world fluid via raycast (both source and flowing liquids)
            BlockHitResult hitResult = getPlayerPOVHitResult(level, player, ClipContext.Fluid.ANY);
            if (hitResult.getType() == HitResult.Type.BLOCK) {
                BlockPos hitPos = hitResult.getBlockPos();
                FluidState state = level.getFluidState(hitPos);
                if (!state.isEmpty()) {
                    Fluid fluid = WellDropRegistry.normalizeFluid(state.getType());
                    if (WellDropRegistry.isValidFluidTarget(fluid)) {
                        if (!level.isClientSide()) {
                            stack.set(AEComponents.STORAGE_CELL_CONFIG_INV, List.of(new GenericStack(AEFluidKey.of(fluid), 1)));
                            player.displayClientMessage(Component.translatable("message.ae2virtualwell.sampled_configured",
                                    getFluidDisplayName(fluid)).withStyle(ChatFormatting.AQUA), true);
                        }
                        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
                    }
                }
            }

            // 3. Clear configuration only when clicking air with empty off-hand away from fluids
            if (otherStack.isEmpty()) {
                if (!level.isClientSide()) {
                    stack.remove(AEComponents.STORAGE_CELL_CONFIG_INV);
                    player.displayClientMessage(Component.translatable("message.ae2virtualwell.cleared")
                            .withStyle(ChatFormatting.RED), true);
                }
                return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
            }
        }

        return super.use(level, player, hand);
    }
}
