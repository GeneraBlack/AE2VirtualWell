package de.project.ae2virtualwell.client.gui;

import de.project.ae2virtualwell.menu.VirtualPartitionerMenu;
import de.project.ae2virtualwell.network.SetPartitionsPayload;
import de.project.ae2virtualwell.util.VirtualCellAdapter;
import de.project.ae2virtualwell.util.VirtualCellAdapter.UniversalPartition;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.TextAlignment;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

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
        public Identifier targetId;
        public boolean isFluid;
        public int percent;
        public boolean voidSecondary;

        public PartitionDraft(Identifier targetId, boolean isFluid, int percent, boolean voidSecondary) {
            this.targetId = targetId;
            this.isFluid = isFluid;
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
        super(menu, playerInventory, title, 220, 262);
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        ItemStack currentCell = menu.getSlot(0).getItem();
        boolean changed = !ItemStack.isSameItem(currentCell, lastCellStack) || 
            !java.util.Objects.equals(
                currentCell.get(de.project.ae2virtualwell.registry.ModDataComponents.PARTITIONS.get()), 
                lastCellStack.get(de.project.ae2virtualwell.registry.ModDataComponents.PARTITIONS.get())
            );
        if (changed) {
            lastCellStack = currentCell.copy();
            loadWorkingListFromCell(currentCell);
            dirty = false;
            selectedRowForPicker = -1;
            scrollOffset = 0;
        }
    }

    private void loadWorkingListFromCell(ItemStack currentCell) {
        workingList.clear();
        if (!currentCell.isEmpty() && VirtualCellAdapter.isVirtualStorageCell(currentCell)) {
            List<UniversalPartition> list = VirtualCellAdapter.readPartitions(currentCell);
            for (UniversalPartition p : list) {
                workingList.add(new PartitionDraft(p.targetId(), p.isFluid(), p.percent(), p.voidSecondary()));
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
    public void extractRenderState(GuiGraphicsExtractor extractor, int mouseX, int mouseY, float partialTick) {
        this.extractBackground(extractor, mouseX, mouseY, partialTick);
        renderCustomBackground(extractor, mouseX, mouseY);
        super.extractRenderState(extractor, mouseX, mouseY, partialTick);
        renderCustomTooltips(extractor, mouseX, mouseY);
    }

    private void renderCustomBackground(GuiGraphicsExtractor extractor, int mouseX, int mouseY) {
        int x = this.leftPos;
        int y = this.topPos;

        // Window background
        extractor.fill(x, y, x + imageWidth, y + imageHeight, 0xFF1E1E1E);
        extractor.fill(x, y, x + imageWidth, y + 1, 0xFF4A4A4A);
        extractor.fill(x, y, x + 1, y + imageHeight, 0xFF4A4A4A);
        extractor.fill(x, y + imageHeight - 1, x + imageWidth, y + imageHeight, 0xFF0D0D0D);
        extractor.fill(x + imageWidth - 1, y, x + imageWidth, y + imageHeight, 0xFF0D0D0D);

        // Header separator
        extractor.fill(x + 8, y + 16, x + imageWidth - 8, y + 17, 0xFF333333);

        // Cell slot background (x=16, y=20)
        drawSlotBox(extractor, x + 15, y + 19);

        // Drive Info Header
        ItemStack cell = menu.getSlot(0).getItem();
        boolean hasCell = !cell.isEmpty() && VirtualCellAdapter.isVirtualStorageCell(cell);
        if (hasCell) {
            String tierName = VirtualCellAdapter.getCellTierName(cell) + " Virtual Drive";
            extractor.textRenderer().accept(x + 40, y + 21, Component.literal(tierName).withColor(0xFF55FF55));

            long totalBytes = VirtualCellAdapter.getCellTotalBytes(cell);
            String stats = String.format("Capacity: %,d B | Allocated: %d%%", totalBytes, getTotalPercent());
            extractor.textRenderer().accept(x + 40, y + 30, Component.literal(stats).withColor(0xFFAAAAAA));
        } else {
            extractor.textRenderer().accept(x + 40, y + 21, Component.literal("No Drive Connected").withColor(0xFFFF5555));
            extractor.textRenderer().accept(x + 40, y + 30, Component.literal("Insert a Virtual Storage Cell").withColor(0xFF777777));
        }

        // GParted Disk Visual Bar (x=12, y=42, w=196, h=14)
        int barX = x + 12;
        int barY = y + 42;
        int barW = 196;
        int barH = 14;

        extractor.fill(barX - 1, barY - 1, barX + barW + 1, barY + barH + 1, 0xFF0A0A0A);

        if (!hasCell) {
            extractor.fill(barX, barY, barX + barW, barY + barH, 0xFF2A2A2A);
            extractor.textRenderer().accept(TextAlignment.CENTER, barX + barW / 2, barY + 3,
                    Component.literal("NO DISK DETECTED").withColor(0xFF555555));
        } else {
            int currentX = barX;
            for (int i = 0; i < workingList.size(); i++) {
                PartitionDraft p = workingList.get(i);
                int sliceW = (int) Math.round((p.percent / 100.0) * barW);
                if (sliceW > 0) {
                    int color = PALETTE[i % PALETTE.length];
                    extractor.fill(currentX, barY, Math.min(barX + barW, currentX + sliceW), barY + barH, color);
                    extractor.fill(currentX + sliceW - 1, barY, currentX + sliceW, barY + barH, 0xFF111111);
                    currentX += sliceW;
                }
            }
            if (currentX < barX + barW) {
                extractor.fill(currentX, barY, barX + barW, barY + barH, 0xFF353535);
                if (barX + barW - currentX > 30) {
                    extractor.textRenderer().accept(TextAlignment.CENTER, (currentX + barX + barW) / 2, barY + 3,
                            Component.literal(getUnallocatedPercent() + "% Free").withColor(0xFF888888));
                }
            }
        }

        // Partition Table Area (y=58 to y=134)
        int tableX = x + 12;
        int tableY = y + 58;
        int tableW = 196;
        int tableH = 76;
        extractor.fill(tableX, tableY, tableX + tableW, tableY + tableH, 0xFF141414);
        extractor.fill(tableX, tableY, tableX + tableW, tableY + 1, 0xFF282828);

        extractor.textRenderer().accept(tableX + 22, tableY + 3, Component.literal("Target").withColor(0xFF888888));
        extractor.textRenderer().accept(tableX + 104, tableY + 3, Component.literal("Alloc").withColor(0xFF888888));
        extractor.textRenderer().accept(tableX + 144, tableY + 3, Component.literal("Void").withColor(0xFF888888));

        int maxVisible = 3;
        if (workingList.isEmpty()) {
            if (hasCell) {
                extractor.textRenderer().accept(TextAlignment.CENTER, tableX + tableW / 2, tableY + 32,
                        Component.literal("No partitions. Click '+ Add' below.").withColor(0xFF666666));
            }
        } else {
            for (int i = 0; i < maxVisible; i++) {
                int index = scrollOffset + i;
                if (index >= workingList.size()) break;

                PartitionDraft p = workingList.get(index);
                int rowY = tableY + 14 + i * 20;

                if (index % 2 == 1) {
                    extractor.fill(tableX + 1, rowY - 1, tableX + tableW - 1, rowY + 19, 0xFF1A1A1A);
                }

                int color = PALETTE[index % PALETTE.length];
                extractor.fill(tableX + 4, rowY + 1, tableX + 7, rowY + 17, color);

                int boxX = tableX + 10;
                int boxY = rowY;
                int boxColor = (selectedRowForPicker == index) ? 0xFFFFFF00 : 0xFF3A3A3A;
                extractor.fill(boxX - 1, boxY - 1, boxX + 17, boxY + 17, boxColor);
                extractor.fill(boxX, boxY, boxX + 16, boxY + 16, 0xFF222222);

                ItemStack renderStack = VirtualCellAdapter.getTargetRenderStack(p.targetId, p.isFluid);
                if (!renderStack.isEmpty()) {
                    extractor.item(renderStack, boxX, boxY);
                }

                String name = VirtualCellAdapter.getTargetDisplayName(p.targetId, p.isFluid).getString();
                if (font.width(name) > 52) {
                    name = font.plainSubstrByWidth(name, 48) + "..";
                }
                extractor.textRenderer().accept(tableX + 30, rowY + 4, Component.literal(name).withColor(0xFFDDDDDD));

                drawButton(extractor, tableX + 88, rowY + 3, 11, 11, "-", 0xFFE0E0E0);

                String pctStr = p.percent + "%";
                extractor.textRenderer().accept(TextAlignment.CENTER, tableX + 111, rowY + 4,
                        Component.literal(pctStr).withColor(0xFFFFFFFF));

                drawButton(extractor, tableX + 122, rowY + 3, 11, 11, "+", 0xFFE0E0E0);

                boolean canVoid = hasVoidCard(cell);
                int voidBg = canVoid ? (p.voidSecondary ? 0xFF5A189A : 0xFF2C2C2C) : 0xFF1A1A1A;
                int voidText = canVoid ? (p.voidSecondary ? 0xFFFFFFFF : 0xFF888888) : 0xFF444444;
                drawButtonWithCustomBg(extractor, tableX + 138, rowY + 3, 26, 11, "Void", voidText, voidBg);

                drawButton(extractor, tableX + 170, rowY + 3, 11, 11, "x", 0xFFFF5555);
            }
        }

        if (workingList.size() > maxVisible) {
            drawButton(extractor, tableX + tableW - 11, tableY + 14, 9, 9, "▲", scrollOffset > 0 ? 0xFFFFFFFF : 0xFF555555);
            drawButton(extractor, tableX + tableW - 11, tableY + tableH - 12, 9, 9, "▼", (scrollOffset + maxVisible < workingList.size()) ? 0xFFFFFFFF : 0xFF555555);
        }

        // Action Buttons Row (y=138)
        int btnY = y + 137;
        int addColor = (hasCell && workingList.size() < 6 && getUnallocatedPercent() > 0) ? 0xFFFFFFFF : 0xFF666666;
        drawButton(extractor, x + 12, btnY, 36, 14, "+ Add", addColor);

        int eqColor = (hasCell && !workingList.isEmpty()) ? 0xFFFFFFFF : 0xFF666666;
        drawButton(extractor, x + 51, btnY, 44, 14, "Equalize", eqColor);

        int clearColor = (hasCell && !workingList.isEmpty()) ? 0xFFFF7777 : 0xFF666666;
        drawButton(extractor, x + 98, btnY, 36, 14, "Clear", clearColor);

        int applyBg = dirty ? 0xFF1B4332 : 0xFF2C2C2C;
        int applyText = dirty ? 0xFF55FF55 : (hasCell ? 0xFFCCCCCC : 0xFF666666);
        drawButtonWithCustomBg(extractor, x + 138, btnY, 70, 14, dirty ? "Apply *" : "Apply", applyText, applyBg);

        // Upgrade Slots Row (y=155)
        extractor.textRenderer().accept(TextAlignment.LEFT, x + 10, y + 158,
                Component.literal("Upgrades:").withColor(0xFF888888));
        // 4 Acceleration card slots (x = 52 + i * 18)
        for (int i = 0; i < 4; i++) {
            int slotX = x + 51 + i * 18;
            int slotY = y + 154;
            if (hasCell) {
                drawSlotBox(extractor, slotX, slotY);
            } else {
                extractor.fill(slotX, slotY, slotX + 18, slotY + 18, 0xFF1A1A1A);
                extractor.fill(slotX + 1, slotY + 1, slotX + 17, slotY + 17, 0xFF111111);
            }
        }
        // 1 Void Secondary card slot (x = 138)
        int voidSlotX = x + 137;
        int voidSlotY = y + 154;
        if (hasCell) {
            drawVoidSlotBox(extractor, voidSlotX, voidSlotY);
        } else {
            extractor.fill(voidSlotX, voidSlotY, voidSlotX + 18, voidSlotY + 18, 0xFF1A1A1A);
            extractor.fill(voidSlotX + 1, voidSlotY + 1, voidSlotX + 17, voidSlotY + 17, 0xFF111111);
        }

        // Player Inventory slots (y = 181)
        int invStartX = x + 30;
        int invStartY = y + 181;
        for (int row = 0; row < 3; ++row) {
            for (int col = 0; col < 9; ++col) {
                drawSlotBox(extractor, invStartX + col * 18 - 1, invStartY + row * 18 - 1);
            }
        }

        // Hotbar slots (y = 239)
        int hotbarStartY = y + 239;
        for (int col = 0; col < 9; ++col) {
            drawSlotBox(extractor, invStartX + col * 18 - 1, hotbarStartY - 1);
        }
    }

    private void drawSlotBox(GuiGraphicsExtractor extractor, int sx, int sy) {
        extractor.fill(sx, sy, sx + 18, sy + 18, 0xFF373737);
        extractor.fill(sx + 1, sy + 1, sx + 17, sy + 17, 0xFF8B8B8B);
        extractor.fill(sx + 1, sy + 1, sx + 16, sy + 16, 0xFF373737);
        extractor.fill(sx + 1, sy + 1, sx + 17, sy + 2, 0xFF373737);
        extractor.fill(sx + 1, sy + 1, sx + 2, sy + 17, 0xFF373737);
        extractor.fill(sx + 1, sy + 1, sx + 17, sy + 17, 0xFF1A1A1A);
    }

    private void drawVoidSlotBox(GuiGraphicsExtractor extractor, int sx, int sy) {
        extractor.fill(sx, sy, sx + 18, sy + 18, 0xFF4A148C);
        extractor.fill(sx + 1, sy + 1, sx + 17, sy + 17, 0xFF7B1FA2);
        extractor.fill(sx + 1, sy + 1, sx + 16, sy + 16, 0xFF4A148C);
        extractor.fill(sx + 1, sy + 1, sx + 17, sy + 2, 0xFF4A148C);
        extractor.fill(sx + 1, sy + 1, sx + 2, sy + 17, 0xFF4A148C);
        extractor.fill(sx + 1, sy + 1, sx + 17, sy + 17, 0xFF1A0A2A);
    }

    private void drawButton(GuiGraphicsExtractor extractor, int bx, int by, int bw, int bh, String text, int textColor) {
        extractor.fill(bx, by, bx + bw, by + bh, 0xFF2C2C2C);
        extractor.fill(bx, by, bx + bw, by + 1, 0xFF3D3D3D);
        extractor.fill(bx, by, bx + 1, by + bh, 0xFF3D3D3D);
        extractor.fill(bx, by + bh - 1, bx + bw, by + bh, 0xFF1A1A1A);
        extractor.fill(bx + bw - 1, by, bx + bw, by + bh, 0xFF1A1A1A);
        extractor.textRenderer().accept(TextAlignment.CENTER, bx + bw / 2, by + (bh - 8) / 2,
                Component.literal(text).withColor(textColor));
    }

    private void drawButtonWithCustomBg(GuiGraphicsExtractor extractor, int bx, int by, int bw, int bh, String text, int textColor, int bgColor) {
        extractor.fill(bx, by, bx + bw, by + bh, bgColor);
        extractor.fill(bx, by, bx + bw, by + 1, 0xFF4A4A4A);
        extractor.fill(bx, by, bx + 1, by + bh, 0xFF4A4A4A);
        extractor.fill(bx, by + bh - 1, bx + bw, by + bh, 0xFF0D0D0D);
        extractor.fill(bx + bw - 1, by, bx + bw, by + bh, 0xFF0D0D0D);
        extractor.textRenderer().accept(TextAlignment.CENTER, bx + bw / 2, by + (bh - 8) / 2,
                Component.literal(text).withColor(textColor));
    }

    private void renderCustomTooltips(GuiGraphicsExtractor extractor, int mouseX, int mouseY) {
        int x = this.leftPos;
        int y = this.topPos;

        ItemStack cell = menu.getSlot(0).getItem();
        boolean hasCell = !cell.isEmpty() && VirtualCellAdapter.isVirtualStorageCell(cell);
        boolean isFluid = hasCell && VirtualCellAdapter.isFluidCell(cell);

        // Bar tooltips
        int barX = x + 12;
        int barY = y + 42;
        int barW = 196;
        int barH = 14;

        if (mouseX >= barX && mouseX <= barX + barW && mouseY >= barY && mouseY <= barY + barH && hasCell) {
            long totalBytes = VirtualCellAdapter.getCellTotalBytes(cell);
            int relX = mouseX - barX;
            int currentX = 0;
            boolean hovered = false;

            for (int i = 0; i < workingList.size(); i++) {
                PartitionDraft p = workingList.get(i);
                int sliceW = (int) Math.round((p.percent / 100.0) * barW);
                if (relX >= currentX && relX < currentX + sliceW) {
                    Component name = VirtualCellAdapter.getTargetDisplayName(p.targetId, p.isFluid);
                    long allocated = (totalBytes * p.percent) / 100L;
                    List<Component> tooltip = List.of(
                            name.copy().withStyle(ChatFormatting.AQUA),
                            Component.literal("Share: " + p.percent + "% (" + String.format("%,d", allocated) + " Bytes)").withStyle(ChatFormatting.GRAY),
                            Component.literal(p.voidSecondary ? "Void Byproducts: Enabled" : "Void Byproducts: Disabled")
                                    .withStyle(p.voidSecondary ? ChatFormatting.DARK_PURPLE : ChatFormatting.DARK_GRAY)
                    );
                    extractor.setComponentTooltipForNextFrame(font, tooltip, mouseX, mouseY);
                    hovered = true;
                    break;
                }
                currentX += sliceW;
            }

            if (!hovered && relX >= currentX) {
                long freeBytes = (totalBytes * getUnallocatedPercent()) / 100L;
                List<Component> tooltip = List.of(
                        Component.literal("Unallocated Space").withStyle(ChatFormatting.YELLOW),
                        Component.literal("Free: " + getUnallocatedPercent() + "% (" + String.format("%,d", freeBytes) + " Bytes)").withStyle(ChatFormatting.GRAY)
                );
                extractor.setComponentTooltipForNextFrame(font, tooltip, mouseX, mouseY);
            }
        }

        // Action button tooltips
        int btnY = y + 137;
        if (mouseY >= btnY && mouseY <= btnY + 14) {
            if (mouseX >= x + 12 && mouseX <= x + 48) {
                extractor.setTooltipForNextFrame(font, Component.literal("Add a new partition to this cell"), mouseX, mouseY);
            } else if (mouseX >= x + 51 && mouseX <= x + 95) {
                extractor.setTooltipForNextFrame(font, Component.literal("Evenly distribute space across all active partitions"), mouseX, mouseY);
            } else if (mouseX >= x + 98 && mouseX <= x + 134) {
                extractor.setTooltipForNextFrame(font, Component.literal("Clear all partitions"), mouseX, mouseY);
            } else if (mouseX >= x + 138 && mouseX <= x + 208) {
                extractor.setTooltipForNextFrame(font, Component.literal("Apply partition layout to the storage cell"), mouseX, mouseY);
            }
        }

        // Table row tooltips
        int tableX = x + 12;
        int tableY = y + 58;
        int maxVisible = 3;
        for (int i = 0; i < maxVisible; i++) {
            int index = scrollOffset + i;
            if (index >= workingList.size()) break;

            PartitionDraft p = workingList.get(index);
            int rowY = tableY + 14 + i * 20;

            if (mouseX >= tableX + 10 && mouseX <= tableX + 27 && mouseY >= rowY && mouseY <= rowY + 17) {
                if (p.targetId != null) {
                    Component displayName = VirtualCellAdapter.getTargetDisplayName(p.targetId, p.isFluid);
                    List<Component> tooltip = new ArrayList<>();
                    tooltip.add(displayName.copy().withStyle(ChatFormatting.AQUA));
                    tooltip.add(Component.literal("Drop item/fluid here or click to pick").withStyle(ChatFormatting.YELLOW));
                    extractor.setComponentTooltipForNextFrame(font, tooltip, mouseX, mouseY);
                } else {
                    extractor.setTooltipForNextFrame(font, Component.literal(isFluid ? "Drop a fluid/bucket here or click to pick" : "Drop an item here or click to pick"), mouseX, mouseY);
                }
            }

            if (mouseX >= tableX + 138 && mouseX <= tableX + 164 && mouseY >= rowY + 3 && mouseY <= rowY + 14) {
                if (hasVoidCard(cell)) {
                    extractor.setTooltipForNextFrame(font, Component.literal("Toggle voiding byproduct outputs"), mouseX, mouseY);
                } else {
                    extractor.setTooltipForNextFrame(font, Component.literal("Requires Void Secondary Card").withStyle(ChatFormatting.RED), mouseX, mouseY);
                }
            }
        }

        // Empty Upgrade slot tooltips
        for (int i = 0; i < 4; i++) {
            int slotX = x + 51 + i * 18;
            int slotY = y + 154;
            if (mouseX >= slotX && mouseX <= slotX + 18 && mouseY >= slotY && mouseY <= slotY + 18) {
                if (!menu.getSlot(1 + i).hasItem()) {
                    extractor.setTooltipForNextFrame(font, Component.literal("Acceleration Card (" + (i + 1) + "/4)").withStyle(ChatFormatting.GRAY), mouseX, mouseY);
                }
            }
        }
        int voidSlotX = x + 137;
        int voidSlotY = y + 154;
        if (mouseX >= voidSlotX && mouseX <= voidSlotX + 18 && mouseY >= voidSlotY && mouseY <= voidSlotY + 18) {
            if (!menu.getSlot(5).hasItem()) {
                extractor.setTooltipForNextFrame(font, Component.literal("Void Secondary Card").withStyle(ChatFormatting.LIGHT_PURPLE), mouseX, mouseY);
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
    public boolean mouseClicked(MouseButtonEvent event, boolean wasHandled) {
        int x = this.leftPos;
        int y = this.topPos;
        double mouseX = event.x();
        double mouseY = event.y();

        ItemStack cell = menu.getSlot(0).getItem();
        boolean hasCell = !cell.isEmpty() && VirtualCellAdapter.isVirtualStorageCell(cell);
        boolean isFluid = hasCell && VirtualCellAdapter.isFluidCell(cell);

        // Action Buttons Row
        int btnY = y + 137;
        if (mouseY >= btnY && mouseY <= btnY + 14) {
            // [+ Add]
            if (mouseX >= x + 12 && mouseX <= x + 48 && hasCell && workingList.size() < 6 && getUnallocatedPercent() > 0) {
                playClickSound();
                Identifier targetId = findFirstUnusedInventoryTarget(cell, isFluid);
                int pct = Math.min(20, Math.max(5, getUnallocatedPercent()));
                workingList.add(new PartitionDraft(targetId, isFluid, pct, false));
                dirty = true;
                return true;
            }
            // [Equalize]
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
            // [Clear]
            if (mouseX >= x + 98 && mouseX <= x + 134 && hasCell && !workingList.isEmpty()) {
                playClickSound();
                workingList.clear();
                selectedRowForPicker = -1;
                dirty = true;
                return true;
            }
            // [Apply & Format]
            if (mouseX >= x + 138 && mouseX <= x + 208 && hasCell) {
                playClickSound();
                applyPartitionsToServer();
                return true;
            }
        }

        // Table Rows interaction
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
                    Identifier pickedId = null;
                    if (isFluid) {
                        Fluid extracted = VirtualCellAdapter.extractFluidFromItem(carried);
                        if (extracted != null) {
                            Identifier fluidId = BuiltInRegistries.FLUID.getKey(extracted);
                            if (VirtualCellAdapter.isValidTarget(cell, fluidId, minecraft != null ? minecraft.level : null)) {
                                pickedId = fluidId;
                            }
                        }
                    } else {
                        Item item = carried.getItem();
                        Identifier itemId = BuiltInRegistries.ITEM.getKey(item);
                        if (VirtualCellAdapter.isValidTarget(cell, itemId, minecraft != null ? minecraft.level : null)) {
                            pickedId = itemId;
                        }
                    }
                    if (pickedId != null) {
                        playClickSound();
                        p.targetId = pickedId;
                        p.isFluid = isFluid;
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
                } else if (p.percent > 1) {
                    p.percent -= 1;
                }
                dirty = true;
                return true;
            }

            if (mouseX >= tableX + 122 && mouseX <= tableX + 133 && mouseY >= rowY + 3 && mouseY <= rowY + 14) {
                playClickSound();
                int unalloc = getUnallocatedPercent();
                if (unalloc >= 5) {
                    p.percent += 5;
                } else if (unalloc > 0) {
                    p.percent += unalloc;
                }
                dirty = true;
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

        Slot slot = getHoveredSlot();
        if (slot != null && slot.hasItem() && selectedRowForPicker >= 0 && selectedRowForPicker < workingList.size()) {
            Identifier pickedId = null;
            if (isFluid) {
                Fluid extracted = VirtualCellAdapter.extractFluidFromItem(slot.getItem());
                if (extracted != null) {
                    Identifier fluidId = BuiltInRegistries.FLUID.getKey(extracted);
                    if (VirtualCellAdapter.isValidTarget(cell, fluidId, minecraft != null ? minecraft.level : null)) {
                        pickedId = fluidId;
                    }
                }
            } else {
                Item item = slot.getItem().getItem();
                Identifier itemId = BuiltInRegistries.ITEM.getKey(item);
                if (VirtualCellAdapter.isValidTarget(cell, itemId, minecraft != null ? minecraft.level : null)) {
                    pickedId = itemId;
                }
            }
            if (pickedId != null) {
                playClickSound();
                workingList.get(selectedRowForPicker).targetId = pickedId;
                workingList.get(selectedRowForPicker).isFluid = isFluid;
                selectedRowForPicker = -1;
                dirty = true;
                return true;
            }
        }

        return super.mouseClicked(event, wasHandled);
    }

    private boolean hasVoidCard(ItemStack cell) {
        return VirtualCellAdapter.hasVoidCardInstalled(menu.upgradeContainer, cell);
    }

    private Identifier findFirstUnusedInventoryTarget(ItemStack cell, boolean isFluid) {
        if (minecraft != null && minecraft.player != null) {
            Inventory inv = minecraft.player.getInventory();
            for (int i = 0; i < inv.getContainerSize(); i++) {
                ItemStack stack = inv.getItem(i);
                if (!stack.isEmpty()) {
                    if (isFluid) {
                        Fluid extracted = VirtualCellAdapter.extractFluidFromItem(stack);
                        if (extracted != null) {
                            Identifier fluidId = BuiltInRegistries.FLUID.getKey(extracted);
                            if (VirtualCellAdapter.isValidTarget(cell, fluidId, minecraft.level)) {
                                boolean used = false;
                                for (PartitionDraft p : workingList) {
                                    if (fluidId.equals(p.targetId)) {
                                        used = true;
                                        break;
                                    }
                                }
                                if (!used) return fluidId;
                            }
                        }
                    } else {
                        Item item = stack.getItem();
                        Identifier itemId = BuiltInRegistries.ITEM.getKey(item);
                        if (VirtualCellAdapter.isValidTarget(cell, itemId, minecraft.level)) {
                            boolean used = false;
                            for (PartitionDraft p : workingList) {
                                if (itemId.equals(p.targetId)) {
                                    used = true;
                                    break;
                                }
                            }
                            if (!used) return itemId;
                        }
                    }
                }
            }
        }
        return null;
    }

    private void applyPartitionsToServer() {
        List<UniversalPartition> partitions = new ArrayList<>();
        for (PartitionDraft draft : workingList) {
            if (draft.targetId != null && draft.percent > 0) {
                partitions.add(new UniversalPartition(draft.targetId, draft.isFluid, draft.percent, draft.voidSecondary));
            }
        }
        ClientPacketDistributor.sendToServer(new SetPartitionsPayload(partitions));
        dirty = false;
    }

    private void playClickSound() {
        if (minecraft != null) {
            minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
        }
    }
}
