package de.project.ae2virtualwell.client.gui;

import appeng.api.upgrades.UpgradeInventories;
import appeng.core.definitions.AEItems;
import de.project.ae2virtualwell.cell.VirtualWellCellItem;
import de.project.ae2virtualwell.cell.partition.WellCellPartition;
import de.project.ae2virtualwell.cell.partition.WellCellPartitionList;
import de.project.ae2virtualwell.menu.VirtualPartitionerMenu;
import de.project.ae2virtualwell.network.SetPartitionsPayload;
import de.project.ae2virtualwell.recipe.WellDropRegistry;
import de.project.ae2virtualwell.registry.ModDataComponents;
import de.project.ae2virtualwell.registry.ModItems;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;

public class VirtualPartitionerScreen extends AbstractContainerScreen<VirtualPartitionerMenu> {

    private static final int[] PALETTE = {
            0xFF2E86AB, // Blue
            0xFF2BA84A, // Green
            0xFFE08D3C, // Orange
            0xFF8338EC, // Purple
            0xFFE63946, // Red
            0xFF00B4D8  // Cyan
    };

    public static class PartitionDraft {
        public Fluid target;
        public int percent;
        public boolean voidSecondary;

        public PartitionDraft(Fluid target, int percent, boolean voidSecondary) {
            this.target = target;
            this.percent = percent;
            this.voidSecondary = voidSecondary;
        }
    }

    private final List<PartitionDraft> workingList = new ArrayList<>();
    private ItemStack lastCellStack = ItemStack.EMPTY;
    private boolean dirty = false;
    private int selectedRowForPicker = -1;
    private int scrollOffset = 0;

    public VirtualPartitionerScreen(VirtualPartitionerMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 220;
        this.imageHeight = 262;
        this.inventoryLabelY = 169;
        this.inventoryLabelX = 30;
    }

    @Override
    protected void init() {
        super.init();
        syncFromCell(true);
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        ItemStack currentCell = menu.getSlot(0).getItem();
        if (!ItemStack.matches(currentCell, lastCellStack)) {
            syncFromCell(false);
        }
    }

    private void syncFromCell(boolean force) {
        ItemStack currentCell = menu.getSlot(0).getItem();
        if (force || !ItemStack.isSameItemSameComponents(currentCell, lastCellStack)) {
            lastCellStack = currentCell.copy();
            workingList.clear();
            selectedRowForPicker = -1;
            scrollOffset = 0;
            dirty = false;

            if (!currentCell.isEmpty() && currentCell.getItem() instanceof VirtualWellCellItem) {
                WellCellPartitionList list = currentCell.get(ModDataComponents.PARTITIONS.get());
                if (list != null && !list.isEmpty()) {
                    for (WellCellPartition p : list.partitions()) {
                        workingList.add(new PartitionDraft(p.target(), p.percent(), p.voidSecondary()));
                    }
                }
            }
        }
    }

    private int getTotalPercent() {
        int sum = 0;
        for (PartitionDraft d : workingList) {
            sum += d.percent;
        }
        return sum;
    }

    private int getUnallocatedPercent() {
        return Math.max(0, 100 - getTotalPercent());
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics, mouseX, mouseY, partialTick);
        renderCustomBackground(graphics, mouseX, mouseY);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
        renderCustomTooltips(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        // Handled in renderCustomBackground
    }

    private void renderCustomBackground(GuiGraphics graphics, int mouseX, int mouseY) {
        int x = this.leftPos;
        int y = this.topPos;

        // Window Background (GParted dark workstation theme)
        graphics.fill(x, y, x + imageWidth, y + imageHeight, 0xFF1E1E1E);
        // Bevel borders
        graphics.fill(x, y, x + imageWidth, y + 1, 0xFF4A4A4A);
        graphics.fill(x, y + 1, x + 1, y + imageHeight, 0xFF4A4A4A);
        graphics.fill(x, y + imageHeight - 1, x + imageWidth, y + imageHeight, 0xFF0D0D0D);
        graphics.fill(x + imageWidth - 1, y, x + imageWidth, y + imageHeight, 0xFF0D0D0D);

        // Header separator
        graphics.fill(x + 8, y + 16, x + imageWidth - 8, y + 17, 0xFF333333);

        // Cell slot background (x=16, y=20)
        drawSlotBox(graphics, x + 15, y + 19);

        // Drive Info Header
        ItemStack cell = menu.getSlot(0).getItem();
        if (!cell.isEmpty() && cell.getItem() instanceof VirtualWellCellItem virtualCell) {
            String tierName = virtualCell.getTier().getTierName() + " Virtual Drive";
            graphics.drawString(font, tierName, x + 40, y + 21, 0xFF55FF55, false);

            long totalBytes = virtualCell.getTier().getTotalBytes();
            String stats = String.format("Capacity: %,d B | Allocated: %d%%", totalBytes, getTotalPercent());
            graphics.drawString(font, stats, x + 40, y + 30, 0xFFAAAAAA, false);
        } else {
            graphics.drawString(font, "No Drive Connected", x + 40, y + 21, 0xFFFF5555, false);
            graphics.drawString(font, "Insert a Virtual Cell below", x + 40, y + 30, 0xFF777777, false);
        }

        // GParted Disk Visual Bar (x=12, y=42, w=196, h=14)
        int barX = x + 12;
        int barY = y + 42;
        int barW = 196;
        int barH = 14;

        // Bar border
        graphics.fill(barX - 1, barY - 1, barX + barW + 1, barY + barH + 1, 0xFF0A0A0A);

        if (cell.isEmpty() || !(cell.getItem() instanceof VirtualWellCellItem)) {
            graphics.fill(barX, barY, barX + barW, barY + barH, 0xFF2A2A2A);
            graphics.drawCenteredString(font, "NO DISK DETECTED", barX + barW / 2, barY + 3, 0xFF555555);
        } else {
            int currentX = barX;
            for (int i = 0; i < workingList.size(); i++) {
                PartitionDraft p = workingList.get(i);
                int sliceW = (int) Math.round((p.percent / 100.0) * barW);
                if (sliceW > 0) {
                    int color = PALETTE[i % PALETTE.length];
                    graphics.fill(currentX, barY, Math.min(barX + barW, currentX + sliceW), barY + barH, color);
                    graphics.fill(currentX + sliceW - 1, barY, currentX + sliceW, barY + barH, 0xFF111111);
                    currentX += sliceW;
                }
            }
            if (currentX < barX + barW) {
                graphics.fill(currentX, barY, barX + barW, barY + barH, 0xFF353535);
                if (barX + barW - currentX > 30) {
                    graphics.drawCenteredString(font, getUnallocatedPercent() + "% Free", (currentX + barX + barW) / 2, barY + 3, 0xFF888888);
                }
            }
        }

        // Partition Table Area (y=58 to y=134)
        int tableX = x + 12;
        int tableY = y + 58;
        int tableW = 196;
        int tableH = 76;
        graphics.fill(tableX, tableY, tableX + tableW, tableY + tableH, 0xFF141414);
        graphics.fill(tableX, tableY, tableX + tableW, tableY + 1, 0xFF282828);

        // Table header labels
        graphics.drawString(font, "Target", tableX + 22, tableY + 3, 0xFF888888, false);
        graphics.drawString(font, "Alloc", tableX + 104, tableY + 3, 0xFF888888, false);
        graphics.drawString(font, "Void", tableX + 144, tableY + 3, 0xFF888888, false);

        // Partition Rows (up to 3 visible rows, height = 20)
        int maxVisible = 3;
        if (workingList.isEmpty()) {
            if (!cell.isEmpty()) {
                graphics.drawCenteredString(font, "No partitions. Click '+ Add' below.", tableX + tableW / 2, tableY + 32, 0xFF666666);
            }
        } else {
            for (int i = 0; i < maxVisible; i++) {
                int index = scrollOffset + i;
                if (index >= workingList.size()) break;

                PartitionDraft p = workingList.get(index);
                int rowY = tableY + 14 + i * 20;

                if (index % 2 == 1) {
                    graphics.fill(tableX + 1, rowY - 1, tableX + tableW - 1, rowY + 19, 0xFF1A1A1A);
                }

                int color = PALETTE[index % PALETTE.length];
                graphics.fill(tableX + 4, rowY + 1, tableX + 7, rowY + 17, color);

                int boxX = tableX + 10;
                int boxY = rowY;
                int boxColor = (selectedRowForPicker == index) ? 0xFFFFFF00 : 0xFF3A3A3A;
                graphics.fill(boxX - 1, boxY - 1, boxX + 17, boxY + 17, boxColor);
                graphics.fill(boxX, boxY, boxX + 16, boxY + 16, 0xFF222222);

                if (p.target != null) {
                    ItemStack bucket = new ItemStack(p.target.getBucket() != Items.AIR ? p.target.getBucket() : Items.BUCKET);
                    graphics.renderItem(bucket, boxX, boxY);
                }

                String name = (p.target != null) ? VirtualWellCellItem.getFluidDisplayName(p.target).getString() : "[Select]";
                if (font.width(name) > 52) {
                    name = font.plainSubstrByWidth(name, 48) + "..";
                }
                graphics.drawString(font, name, tableX + 30, rowY + 4, 0xFFDDDDDD, false);

                drawButton(graphics, tableX + 88, rowY + 3, 11, 11, "-", 0xFFE0E0E0);

                String pctStr = p.percent + "%";
                graphics.drawCenteredString(font, pctStr, tableX + 111, rowY + 4, 0xFFFFFFFF);

                drawButton(graphics, tableX + 122, rowY + 3, 11, 11, "+", 0xFFE0E0E0);

                int voidBg = p.voidSecondary ? 0xFF5A189A : 0xFF2C2C2C;
                int voidText = p.voidSecondary ? 0xFFFFFFFF : 0xFF888888;
                drawButtonWithCustomBg(graphics, tableX + 138, rowY + 3, 26, 11, "Void", voidText, voidBg);

                drawButton(graphics, tableX + 170, rowY + 3, 11, 11, "x", 0xFFFF5555);
            }
        }

        // Scrollbar if needed
        if (workingList.size() > maxVisible) {
            int scrollTrackX = tableX + 185;
            int scrollTrackY = tableY + 14;
            int scrollTrackH = maxVisible * 20;
            graphics.fill(scrollTrackX, scrollTrackY, scrollTrackX + 9, scrollTrackY + scrollTrackH, 0xFF1F1F1F);
            drawButton(graphics, scrollTrackX, scrollTrackY, 9, 9, "▲", 0xFFCCCCCC);
            drawButton(graphics, scrollTrackX, scrollTrackY + scrollTrackH - 9, 9, 9, "▼", 0xFFCCCCCC);
        }

        // Action Buttons Row (y=137)
        int btnY = y + 137;
        boolean hasCell = !cell.isEmpty() && cell.getItem() instanceof VirtualWellCellItem;

        int addColor = (hasCell && workingList.size() < 6 && getUnallocatedPercent() > 0) ? 0xFFFFFFFF : 0xFF666666;
        drawButton(graphics, x + 12, btnY, 36, 14, "+ Add", addColor);

        int eqColor = (hasCell && !workingList.isEmpty()) ? 0xFFFFFFFF : 0xFF666666;
        drawButton(graphics, x + 51, btnY, 44, 14, "Equalize", eqColor);

        int clearColor = (hasCell && !workingList.isEmpty()) ? 0xFFFF7777 : 0xFF666666;
        drawButton(graphics, x + 98, btnY, 36, 14, "Clear", clearColor);

        int applyBg = dirty ? 0xFF1B4332 : 0xFF2C2C2C;
        int applyText = dirty ? 0xFF55FF55 : (hasCell ? 0xFFCCCCCC : 0xFF666666);
        drawButtonWithCustomBg(graphics, x + 138, btnY, 70, 14, dirty ? "Apply *" : "Apply", applyText, applyBg);

        // Upgrade Slots Row (y=155)
        graphics.drawString(font, "Upgrades:", x + 10, y + 158, 0xFF888888, false);
        for (int i = 0; i < 4; i++) {
            int slotX = x + 51 + i * 18;
            int slotY = y + 154;
            if (hasCell) {
                drawSlotBox(graphics, slotX, slotY);
            } else {
                graphics.fill(slotX, slotY, slotX + 18, slotY + 18, 0xFF1A1A1A);
                graphics.fill(slotX + 1, slotY + 1, slotX + 17, slotY + 17, 0xFF111111);
            }
        }
        int voidSlotX = x + 137;
        int voidSlotY = y + 154;
        if (hasCell) {
            drawVoidSlotBox(graphics, voidSlotX, voidSlotY);
        } else {
            graphics.fill(voidSlotX, voidSlotY, voidSlotX + 18, voidSlotY + 18, 0xFF1A1A1A);
            graphics.fill(voidSlotX + 1, voidSlotY + 1, voidSlotX + 17, voidSlotY + 17, 0xFF111111);
        }

        // Player Inventory slots
        int invStartX = x + 30;
        int invStartY = y + 180;
        for (int row = 0; row < 3; ++row) {
            for (int col = 0; col < 9; ++col) {
                drawSlotBox(graphics, invStartX + col * 18 - 1, invStartY + row * 18 - 1);
            }
        }

        // Hotbar slots
        int hotbarStartY = y + 238;
        for (int col = 0; col < 9; ++col) {
            drawSlotBox(graphics, invStartX + col * 18 - 1, hotbarStartY - 1);
        }
    }

    private void drawSlotBox(GuiGraphics graphics, int sx, int sy) {
        graphics.fill(sx, sy, sx + 18, sy + 18, 0xFF373737);
        graphics.fill(sx + 1, sy + 1, sx + 17, sy + 17, 0xFF8B8B8B);
        graphics.fill(sx + 1, sy + 1, sx + 16, sy + 16, 0xFF373737);
        graphics.fill(sx + 1, sy + 1, sx + 17, sy + 2, 0xFF373737);
        graphics.fill(sx + 1, sy + 1, sx + 2, sy + 17, 0xFF373737);
        graphics.fill(sx + 1, sy + 1, sx + 17, sy + 17, 0xFF1A1A1A);
    }

    private void drawVoidSlotBox(GuiGraphics graphics, int sx, int sy) {
        graphics.fill(sx, sy, sx + 18, sy + 18, 0xFF5A189A);
        graphics.fill(sx + 1, sy + 1, sx + 17, sy + 17, 0xFF8B8B8B);
        graphics.fill(sx + 1, sy + 1, sx + 16, sy + 16, 0xFF373737);
        graphics.fill(sx + 1, sy + 1, sx + 17, sy + 2, 0xFF373737);
        graphics.fill(sx + 1, sy + 1, sx + 2, sy + 17, 0xFF373737);
        graphics.fill(sx + 1, sy + 1, sx + 17, sy + 17, 0xFF1A1A1A);
    }

    private void drawButton(GuiGraphics graphics, int bx, int by, int bw, int bh, String label, int textColor) {
        drawButtonWithCustomBg(graphics, bx, by, bw, bh, label, textColor, 0xFF2C2C2C);
    }

    private void drawButtonWithCustomBg(GuiGraphics graphics, int bx, int by, int bw, int bh, String label, int textColor, int bgColor) {
        graphics.fill(bx, by, bx + bw, by + bh, 0xFF0D0D0D);
        graphics.fill(bx + 1, by + 1, bx + bw - 1, by + bh - 1, bgColor);
        graphics.fill(bx + 1, by + 1, bx + bw - 1, by + 2, 0xFF555555);
        graphics.fill(bx + 1, by + 1, bx + 2, by + bh - 1, 0xFF555555);

        int textW = font.width(label);
        graphics.drawString(font, label, bx + (bw - textW) / 2, by + (bh - 8) / 2, textColor, false);
    }

    private void renderCustomTooltips(GuiGraphics graphics, int mouseX, int mouseY) {
        int x = this.leftPos;
        int y = this.topPos;

        int tableX = x + 12;
        int tableY = y + 58;
        int maxVisible = 3;

        ItemStack cell = menu.getSlot(0).getItem();
        boolean voidInstalled = hasVoidCard(cell);

        for (int i = 0; i < maxVisible; i++) {
            int index = scrollOffset + i;
            if (index >= workingList.size()) break;
            PartitionDraft p = workingList.get(index);
            int rowY = tableY + 14 + i * 20;

            if (mouseX >= tableX + 10 && mouseX <= tableX + 27 && mouseY >= rowY && mouseY <= rowY + 17) {
                if (p.target != null) {
                    graphics.renderTooltip(font, VirtualWellCellItem.getFluidDisplayName(p.target), mouseX, mouseY);
                } else {
                    graphics.renderTooltip(font, Component.literal("Click with bucket/fluid container or click to select").withStyle(ChatFormatting.GRAY), mouseX, mouseY);
                }
            }

            if (mouseX >= tableX + 138 && mouseX <= tableX + 164 && mouseY >= rowY + 3 && mouseY <= rowY + 14) {
                if (voidInstalled) {
                    graphics.renderTooltip(font, Component.literal(p.voidSecondary ? "Void Secondary: ON" : "Void Secondary: OFF").withStyle(ChatFormatting.LIGHT_PURPLE), mouseX, mouseY);
                } else {
                    graphics.renderTooltip(font, Component.literal("Requires Void Secondary Card").withStyle(ChatFormatting.RED), mouseX, mouseY);
                }
            }
        }

        // Empty Upgrade slot tooltips
        for (int i = 0; i < 4; i++) {
            int slotX = x + 51 + i * 18;
            int slotY = y + 154;
            if (mouseX >= slotX && mouseX <= slotX + 18 && mouseY >= slotY && mouseY <= slotY + 18) {
                if (!menu.getSlot(1 + i).hasItem()) {
                    graphics.renderTooltip(font, Component.literal("Acceleration Card (" + (i + 1) + "/4)").withStyle(ChatFormatting.GRAY), mouseX, mouseY);
                }
            }
        }
        int voidSlotX = x + 137;
        int voidSlotY = y + 154;
        if (mouseX >= voidSlotX && mouseX <= voidSlotX + 18 && mouseY >= voidSlotY && mouseY <= voidSlotY + 18) {
            if (!menu.getSlot(5).hasItem()) {
                graphics.renderTooltip(font, Component.literal("Void Secondary Card").withStyle(ChatFormatting.LIGHT_PURPLE), mouseX, mouseY);
            }
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (workingList.size() > 3) {
            int tableX = this.leftPos + 12;
            int tableY = this.topPos + 58;
            int tableW = 196;
            int tableH = 76;
            if (mouseX >= tableX && mouseX <= tableX + tableW && mouseY >= tableY && mouseY <= tableY + tableH) {
                if (scrollY > 0 && scrollOffset > 0) {
                    scrollOffset--;
                    return true;
                } else if (scrollY < 0 && scrollOffset + 3 < workingList.size()) {
                    scrollOffset++;
                    return true;
                }
            }
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0) {
            return super.mouseClicked(mouseX, mouseY, button);
        }

        int x = this.leftPos;
        int y = this.topPos;

        ItemStack cell = menu.getSlot(0).getItem();
        boolean hasCell = !cell.isEmpty() && cell.getItem() instanceof VirtualWellCellItem;

        int btnY = y + 137;
        if (mouseY >= btnY && mouseY <= btnY + 14) {
            if (mouseX >= x + 12 && mouseX <= x + 48 && hasCell && workingList.size() < 6 && getUnallocatedPercent() > 0) {
                playClickSound();
                Fluid target = findFirstUnusedInventoryTarget();
                int pct = Math.min(20, Math.max(5, getUnallocatedPercent()));
                workingList.add(new PartitionDraft(target, pct, false));
                dirty = true;
                return true;
            }
            if (mouseX >= x + 51 && mouseX <= x + 95 && hasCell && !workingList.isEmpty()) {
                playClickSound();
                int count = workingList.size();
                int share = 100 / count;
                int rem = 100 % count;
                for (int i = 0; i < count; i++) {
                    workingList.get(i).percent = share + (i < rem ? 1 : 0);
                }
                dirty = true;
                return true;
            }
            if (mouseX >= x + 98 && mouseX <= x + 134 && hasCell && !workingList.isEmpty()) {
                playClickSound();
                workingList.clear();
                selectedRowForPicker = -1;
                dirty = true;
                return true;
            }
            if (mouseX >= x + 138 && mouseX <= x + 208 && hasCell) {
                playClickSound();
                applyPartitionsToServer();
                return true;
            }
        }

        int tableX = x + 12;
        int tableY = y + 58;
        int maxVisible = 3;
        for (int i = 0; i < maxVisible; i++) {
            int index = scrollOffset + i;
            if (index >= workingList.size()) break;
            PartitionDraft p = workingList.get(index);
            int rowY = tableY + 14 + i * 20;

            if (mouseX >= tableX + 10 && mouseX <= tableX + 27 && mouseY >= rowY && mouseY <= rowY + 17) {
                ItemStack carried = menu.getCarried();
                if (!carried.isEmpty()) {
                    Fluid carriedFluid = WellDropRegistry.extractFluidFromItem(carried);
                    if (carriedFluid != null && WellDropRegistry.isValidFluidTarget(carriedFluid, minecraft != null ? minecraft.level : null)) {
                        playClickSound();
                        p.target = WellDropRegistry.normalizeFluid(carriedFluid);
                        selectedRowForPicker = -1;
                        dirty = true;
                        return true;
                    }
                }
                playClickSound();
                if (selectedRowForPicker == index) {
                    selectedRowForPicker = -1;
                } else {
                    selectedRowForPicker = index;
                }
                return true;
            }

            if (mouseX >= tableX + 88 && mouseX <= tableX + 99 && mouseY >= rowY + 3 && mouseY <= rowY + 14) {
                playClickSound();
                if (p.percent > 5) {
                    p.percent -= 5;
                    dirty = true;
                }
                return true;
            }

            if (mouseX >= tableX + 122 && mouseX <= tableX + 133 && mouseY >= rowY + 3 && mouseY <= rowY + 14) {
                playClickSound();
                int unalloc = getUnallocatedPercent();
                if (unalloc > 0) {
                    int add = Math.min(5, unalloc);
                    p.percent += add;
                    dirty = true;
                }
                return true;
            }

            if (mouseX >= tableX + 138 && mouseX <= tableX + 164 && mouseY >= rowY + 3 && mouseY <= rowY + 14) {
                if (hasVoidCard(cell)) {
                    playClickSound();
                    p.voidSecondary = !p.voidSecondary;
                    dirty = true;
                }
                return true;
            }

            if (mouseX >= tableX + 170 && mouseX <= tableX + 181 && mouseY >= rowY + 3 && mouseY <= rowY + 14) {
                playClickSound();
                workingList.remove(index);
                if (selectedRowForPicker == index) selectedRowForPicker = -1;
                dirty = true;
                return true;
            }
        }

        if (workingList.size() > maxVisible) {
            if (mouseX >= tableX + 185 && mouseX <= tableX + 194) {
                if (mouseY >= tableY + 14 && mouseY <= tableY + 23 && scrollOffset > 0) {
                    playClickSound();
                    scrollOffset--;
                    return true;
                }
                if (mouseY >= tableY + 64 && mouseY <= tableY + 73 && scrollOffset + maxVisible < workingList.size()) {
                    playClickSound();
                    scrollOffset++;
                    return true;
                }
            }
        }

        Slot slot = getSlotUnderMouse();
        if (slot != null && slot.hasItem() && selectedRowForPicker >= 0 && selectedRowForPicker < workingList.size()) {
            Fluid fluid = WellDropRegistry.extractFluidFromItem(slot.getItem());
            if (fluid != null && WellDropRegistry.isValidFluidTarget(fluid, minecraft != null ? minecraft.level : null)) {
                playClickSound();
                workingList.get(selectedRowForPicker).target = WellDropRegistry.normalizeFluid(fluid);
                selectedRowForPicker = -1;
                dirty = true;
                return true;
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    private boolean hasVoidCard(ItemStack cell) {
        if (cell.isEmpty()) return false;
        if (menu.getSlot(5).hasItem()) return true;
        var upgrades = UpgradeInventories.forItem(cell, 5);
        return upgrades != null && (upgrades.isInstalled(ModItems.VOID_SECONDARY_CARD.get())
                || upgrades.isInstalled(AEItems.VOID_CARD.asItem()));
    }

    private Fluid findFirstUnusedInventoryTarget() {
        if (minecraft != null && minecraft.player != null) {
            Inventory inv = minecraft.player.getInventory();
            for (int i = 0; i < inv.getContainerSize(); i++) {
                ItemStack stack = inv.getItem(i);
                if (!stack.isEmpty()) {
                    Fluid fluid = WellDropRegistry.extractFluidFromItem(stack);
                    if (fluid != null && WellDropRegistry.isValidFluidTarget(fluid, minecraft.level)) {
                        Fluid normalized = WellDropRegistry.normalizeFluid(fluid);
                        boolean used = false;
                        for (PartitionDraft p : workingList) {
                            if (p.target == normalized) {
                                used = true;
                                break;
                            }
                        }
                        if (!used) return normalized;
                    }
                }
            }
        }
        return null;
    }

    private void applyPartitionsToServer() {
        List<WellCellPartition> partitions = new ArrayList<>();
        for (PartitionDraft draft : workingList) {
            if (draft.target != null && draft.percent > 0) {
                partitions.add(new WellCellPartition(draft.target, draft.percent, draft.voidSecondary));
            }
        }
        PacketDistributor.sendToServer(new SetPartitionsPayload(new WellCellPartitionList(partitions)));
        dirty = false;
    }

    private void playClickSound() {
        if (minecraft != null) {
            minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
        }
    }
}
