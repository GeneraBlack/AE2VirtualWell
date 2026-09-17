package de.project.ae2virtualwell.cell;

import appeng.api.config.Actionable;
import appeng.api.ids.AEComponents;
import appeng.api.networking.security.IActionSource;
import appeng.api.stacks.AEFluidKey;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.GenericStack;
import appeng.api.stacks.KeyCounter;
import appeng.api.storage.cells.CellState;
import appeng.api.storage.cells.ISaveProvider;
import de.project.ae2virtualwell.recipe.WellDropRegistry;
import it.unimi.dsi.fastutil.objects.Object2LongMap;
import it.unimi.dsi.fastutil.objects.Object2LongMaps;
import it.unimi.dsi.fastutil.objects.Object2LongOpenHashMap;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class VirtualWellCellInventory implements IVirtualWellCell {

    private static final long AMOUNT_PER_BYTE = 8L * AEFluidKey.AMOUNT_BUCKET; // 8,000 mB per byte

    private final ItemStack stack;
    private final @Nullable ISaveProvider host;
    private final WellCellTier tier;
    private final Object2LongMap<AEKey> storedAmounts = new Object2LongOpenHashMap<>();
    private long storedFluidAmount = 0;
    private int storedTypes = 0;
    private boolean isPersisted = true;

    public VirtualWellCellInventory(ItemStack stack, @Nullable ISaveProvider host, WellCellTier tier) {
        this.stack = stack;
        this.host = host;
        this.tier = tier;
        this.loadCellFluids();
    }

    private void loadCellFluids() {
        List<GenericStack> stacks = stack.get(AEComponents.STORAGE_CELL_INV);
        if (stacks != null) {
            for (GenericStack entry : stacks) {
                if (entry != null && entry.amount() > 0 && entry.what() instanceof AEFluidKey) {
                    this.storedAmounts.mergeLong(entry.what(), entry.amount(), Long::sum);
                }
            }
        }
        this.storedFluidAmount = 0;
        for (long amt : this.storedAmounts.values()) {
            this.storedFluidAmount += amt;
        }
        this.storedTypes = this.storedAmounts.size();
    }

    @Override
    public ItemStack getItemStack() {
        return stack;
    }

    @Nullable
    @Override
    public ISaveProvider getSaveProvider() {
        return host;
    }

    @Override
    public WellCellTier getTier() {
        return tier;
    }

    @Nullable
    @Override
    public Fluid getConfiguredTarget() {
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

    public long getTotalBytes() {
        return tier.getTotalBytes();
    }

    public int getBytesPerType() {
        return tier.getBytesPerType();
    }

    public int getTotalFluidTypes() {
        return tier.getTotalTypes();
    }

    public long getStoredFluidAmount() {
        return storedFluidAmount;
    }

    public int getStoredFluidTypes() {
        return storedTypes;
    }

    public int getUnusedFluidAmount() {
        int rem = (int) (this.storedFluidAmount % AMOUNT_PER_BYTE);
        return rem == 0 ? 0 : (int) (AMOUNT_PER_BYTE - rem);
    }

    public long getUsedBytes() {
        long bytesForFluid = (this.storedFluidAmount + AMOUNT_PER_BYTE - 1L) / AMOUNT_PER_BYTE;
        return (long) this.storedTypes * this.tier.getBytesPerType() + bytesForFluid;
    }

    public long getFreeBytes() {
        return Math.max(0L, this.tier.getTotalBytes() - this.getUsedBytes());
    }

    public long getRemainingFluidTypes() {
        long basedOnStorage = this.getFreeBytes() / this.tier.getBytesPerType();
        long basedOnTotal = this.tier.getTotalTypes() - this.storedTypes;
        return Math.max(0L, Math.min(basedOnStorage, basedOnTotal));
    }

    public boolean canHoldNewType() {
        long freeBytes = this.getFreeBytes();
        return (freeBytes > this.tier.getBytesPerType()
                || (freeBytes == this.tier.getBytesPerType() && this.getUnusedFluidAmount() > 0))
                && this.getRemainingFluidTypes() > 0;
    }

    public long getRemainingFluidCapacity() {
        long remaining = this.getFreeBytes() * AMOUNT_PER_BYTE + (long) this.getUnusedFluidAmount();
        return Math.max(0L, remaining);
    }

    @Override
    public boolean isFull() {
        return getStatus() == CellState.FULL || getRemainingFluidCapacity() <= 0;
    }

    @Override
    public CellState getStatus() {
        if (this.storedTypes == 0) {
            return CellState.EMPTY;
        }
        if (this.canHoldNewType()) {
            return CellState.NOT_EMPTY;
        }
        if (this.getRemainingFluidCapacity() > 0) {
            return CellState.TYPES_FULL;
        }
        return CellState.FULL;
    }

    @Override
    public long injectGeneratedFluid(AEFluidKey key, long amount, Actionable mode) {
        if (amount <= 0 || isFull()) {
            return 0;
        }

        long currentAmount = this.storedAmounts.getLong(key);
        long remainingCapacity = this.getRemainingFluidCapacity();

        // If this key is not yet stored, verify and allocate type space
        if (currentAmount <= 0) {
            if (!canHoldNewType()) {
                return 0;
            }
            remainingCapacity -= (long) this.tier.getBytesPerType() * AMOUNT_PER_BYTE;
            if (remainingCapacity <= 0) {
                return 0;
            }
        }

        long toInsert = Math.min(amount, remainingCapacity);
        if (toInsert <= 0) {
            return 0;
        }

        if (mode == Actionable.MODULATE) {
            this.storedAmounts.put(key, currentAmount + toInsert);
            this.saveChanges();
        }

        return toInsert;
    }

    @Override
    public double getIdleDrain() {
        return tier.getIdleDrain();
    }

    @Override
    public void persist() {
        if (this.isPersisted) {
            return;
        }

        long totalFluid = 0L;
        List<GenericStack> stacks = new ArrayList<>(storedAmounts.size());

        for (var entry : this.storedAmounts.object2LongEntrySet()) {
            long amount = entry.getLongValue();
            if (amount > 0) {
                totalFluid += amount;
                stacks.add(new GenericStack(entry.getKey(), amount));
            }
        }

        if (stacks.isEmpty()) {
            stack.remove(AEComponents.STORAGE_CELL_INV);
        } else {
            stack.set(AEComponents.STORAGE_CELL_INV, stacks);
        }

        this.storedTypes = this.storedAmounts.size();
        this.storedFluidAmount = totalFluid;
        this.isPersisted = true;
    }

    protected void saveChanges() {
        this.storedTypes = this.storedAmounts.size();
        this.storedFluidAmount = 0;
        for (long storedAmount : this.storedAmounts.values()) {
            this.storedFluidAmount += storedAmount;
        }

        this.isPersisted = false;
        if (this.host != null) {
            this.host.saveChanges();
        } else {
            this.persist();
        }
    }

    @Override
    public long insert(AEKey what, long amount, Actionable mode, IActionSource source) {
        if (amount <= 0 || !(what instanceof AEFluidKey fluidKey)) {
            return 0;
        }

        Fluid configuredTarget = getConfiguredTarget();
        if (configuredTarget == null) {
            return 0;
        }

        Fluid fluid = WellDropRegistry.normalizeFluid(fluidKey.getFluid());
        boolean allowed = false;
        if (fluid.equals(configuredTarget)) {
            allowed = true;
        } else {
            List<de.project.ae2virtualwell.recipe.WellDropEntry> drops = WellDropRegistry.getDropEntries(configuredTarget, null);
            for (var entry : drops) {
                if (WellDropRegistry.normalizeFluid(entry.fluid()).equals(fluid)) {
                    allowed = true;
                    break;
                }
            }
        }

        if (!allowed) {
            return 0;
        }

        return injectGeneratedFluid(fluidKey, amount, mode);
    }

    @Override
    public long extract(AEKey what, long amount, Actionable mode, IActionSource source) {
        if (amount <= 0 || !(what instanceof AEFluidKey)) {
            return 0;
        }
        long currentAmount = this.storedAmounts.getLong(what);
        if (currentAmount <= 0) {
            return 0;
        }
        long toExtract = Math.min(amount, currentAmount);
        if (mode == Actionable.MODULATE) {
            if (currentAmount == toExtract) {
                this.storedAmounts.removeLong(what);
            } else {
                this.storedAmounts.put(what, currentAmount - toExtract);
            }
            this.saveChanges();
        }
        return toExtract;
    }

    @Override
    public void getAvailableStacks(KeyCounter out) {
        for (var entry : Object2LongMaps.fastIterable(this.storedAmounts)) {
            out.add(entry.getKey(), entry.getLongValue());
        }
    }

    @Override
    public Component getDescription() {
        return stack.getHoverName();
    }
}
