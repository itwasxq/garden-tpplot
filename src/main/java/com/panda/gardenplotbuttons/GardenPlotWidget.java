package com.panda.gardenplotbuttons;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.network.chat.Component;
import net.minecraft.util.CommonColors;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.List;

public final class GardenPlotWidget extends AbstractWidget {
    public static final int WIDTH = 104;
    public static final int HEIGHT = 132;

    private static final int HEADER_HEIGHT = 14;
    private static final int GRID_X = 7;
    private static final int GRID_Y = 18;
    private static final int SLOT_SIZE = 18;

    private int hoveredSlot = -1;
    private boolean dragging;
    private double dragOffsetX;
    private double dragOffsetY;

    public GardenPlotWidget(int x, int y) {
        super(x, y, WIDTH, HEIGHT, Component.literal("Garden Plots"));
    }

    @Override
    protected void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        Font font = Minecraft.getInstance().font;

        graphics.fill(getX(), getY(), getRight(), getBottom(), 0xCC101010);
        graphics.fill(getX(), getY(), getRight(), getY() + HEADER_HEIGHT, 0xDD2A2A2A);
        graphics.fill(getX(), getBottom() - 25, getRight(), getBottom(), 0xAA1C1C1C);

        String title = dragging ? "Drag UI" : "Garden Plots";
        graphics.text(font, title, getX() + 7, getY() + 3, CommonColors.WHITE, false);

        hoveredSlot = -1;
        renderPlots(graphics, font, mouseX, mouseY);
        renderActionButtons(graphics, font, mouseX, mouseY);

        if (!GardenPlotConfig.hasLearnedPlots()) {
            graphics.fill(getX() + 5, getY() + 60, getRight() - 5, getY() + 92, 0xDD000000);
            graphics.text(font, "Open /desk", getX() + 12, getY() + 64, 0xFFFFDD55, false);
            graphics.text(font, "then close Configure Plots", getX() + 12, getY() + 76, 0xFFFFDD55, false);
        }
    }

    private void renderPlots(GuiGraphicsExtractor graphics, Font font, int mouseX, int mouseY) {
        for (int index = 0; index < 25; index++) {
            int slotX = getX() + GRID_X + (index % 5) * SLOT_SIZE;
            int slotY = getY() + GRID_Y + (index / 5) * SLOT_SIZE;
            boolean hovered = inArea(mouseX, mouseY, slotX, slotY, SLOT_SIZE, SLOT_SIZE);

            graphics.fill(slotX, slotY, slotX + 17, slotY + 17, hovered ? 0x80FFFFFF : 0x50303030);
            graphics.fill(slotX + 1, slotY + 1, slotX + 16, slotY + 16, 0x803A3A3A);

            ItemStack stack = makePlotIcon(index);
            graphics.item(stack, slotX + 1, slotY + 1);

            if (hovered) {
                hoveredSlot = index;
                String name = plotNameForDisplay(index);
                List<Component> tooltip = List.of(
                        Component.literal(index == 12 ? "The Barn" : "Plot: " + name).withStyle(ChatFormatting.GREEN),
                        Component.literal("Left click: /plottp " + commandName(index)).withStyle(ChatFormatting.YELLOW),
                        Component.literal("Drag top bar to move UI").withStyle(ChatFormatting.GRAY)
                );
                graphics.setComponentTooltipForNextFrame(font, tooltip, mouseX, mouseY);
            }
        }
    }

    private void renderActionButtons(GuiGraphicsExtractor graphics, Font font, int mouseX, int mouseY) {
        drawActionButton(graphics, font, mouseX, mouseY, 7, "Desk", new ItemStack(Items.BOOK), "/desk");
        drawActionButton(graphics, font, mouseX, mouseY, 42, "Garden", new ItemStack(Items.ENDER_EYE), "/warp garden");
        drawActionButton(graphics, font, mouseX, mouseY, 77, "Set", new ItemStack(Items.RED_BED), "/setspawn");
    }

    private void drawActionButton(GuiGraphicsExtractor graphics, Font font, int mouseX, int mouseY, int localX, String label, ItemStack icon, String command) {
        int x = getX() + localX;
        int y = getBottom() - 22;
        boolean hovered = inArea(mouseX, mouseY, x, y, 20, 20);
        graphics.fill(x, y, x + 20, y + 20, hovered ? 0x80FFFFFF : 0x50404040);
        graphics.fill(x + 1, y + 1, x + 19, y + 19, 0x80202020);
        graphics.item(icon, x + 2, y + 2);
        if (hovered) {
            graphics.setComponentTooltipForNextFrame(font, List.of(
                    Component.literal(label).withStyle(ChatFormatting.GREEN),
                    Component.literal("Left click: " + command).withStyle(ChatFormatting.YELLOW)
            ), mouseX, mouseY);
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent click, boolean doubled) {
        if (!visible || !active) return false;
        double mouseX = click.x();
        double mouseY = click.y();

        if (click.button() == 0 && inHeader(mouseX, mouseY)) {
            dragging = true;
            dragOffsetX = mouseX - getX();
            dragOffsetY = mouseY - getY();
            return true;
        }

        if (click.button() == 1 && inHeader(mouseX, mouseY)) {
            if (Minecraft.getInstance().screen != null) {
                GardenPlotConfig.resetPosition(Minecraft.getInstance().screen.width, Minecraft.getInstance().screen.height);
                setPosition(GardenPlotConfig.getX(Minecraft.getInstance().screen.width), GardenPlotConfig.getY(Minecraft.getInstance().screen.height));
            }
            GardenPlotButtonsClient.clickSound();
            return true;
        }

        if (click.button() == 0 && isMouseOver(mouseX, mouseY)) {
            String action = actionButtonCommand(mouseX, mouseY);
            if (action != null) {
                runCommand(action);
                return true;
            }

            int slot = plotSlotAt(mouseX, mouseY);
            if (slot >= 0) {
                if (slot != 12 && GardenPlotConfig.getPlotName(slot).isBlank()) {
                    GardenPlotButtonsClient.LOGGER.info("Plot {} has no learned name yet. Open /desk > Configure Plots once, then close it.", slot);
                    return true;
                }
                runCommand("/plottp " + commandName(slot));
                return true;
            }
        }

        return super.mouseClicked(click, doubled);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent click, double offsetX, double offsetY) {
        if (dragging && click.button() == 0) {
            int newX = (int) Math.round(click.x() - dragOffsetX);
            int newY = (int) Math.round(click.y() - dragOffsetY);
            if (Minecraft.getInstance().screen != null) {
                newX = clamp(newX, 0, Math.max(0, Minecraft.getInstance().screen.width - getWidth()));
                newY = clamp(newY, 0, Math.max(0, Minecraft.getInstance().screen.height - getHeight()));
            }
            setPosition(newX, newY);
            return true;
        }
        return super.mouseDragged(click, offsetX, offsetY);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent click) {
        if (dragging) {
            dragging = false;
            GardenPlotConfig.setPosition(getX(), getY());
            return true;
        }
        return super.mouseReleased(click);
    }

    private void runCommand(String command) {
        if (GardenPlotConfig.closeInventoryOnPlotClick() && Minecraft.getInstance().screen != null) {
            Minecraft.getInstance().screen.onClose();
        }
        GardenPlotButtonsClient.sendCommand(command);
        GardenPlotButtonsClient.clickSound();
    }

    private int plotSlotAt(double mouseX, double mouseY) {
        int localX = (int) mouseX - getX() - GRID_X;
        int localY = (int) mouseY - getY() - GRID_Y;
        if (localX < 0 || localY < 0) return -1;
        int col = localX / SLOT_SIZE;
        int row = localY / SLOT_SIZE;
        if (col < 0 || col >= 5 || row < 0 || row >= 5) return -1;
        int inSlotX = localX % SLOT_SIZE;
        int inSlotY = localY % SLOT_SIZE;
        if (inSlotX >= 18 || inSlotY >= 18) return -1;
        return row * 5 + col;
    }

    private String actionButtonCommand(double mouseX, double mouseY) {
        int y = getBottom() - 22;
        if (inArea(mouseX, mouseY, getX() + 7, y, 20, 20)) return "/desk";
        if (inArea(mouseX, mouseY, getX() + 42, y, 20, 20)) return "/warp garden";
        if (inArea(mouseX, mouseY, getX() + 77, y, 20, 20)) return "/setspawn";
        return null;
    }

    private ItemStack makePlotIcon(int index) {
        if (index == 12) {
            ItemStack barn = new ItemStack(Items.LODESTONE);
            barn.set(DataComponents.ITEM_NAME, Component.literal("The Barn"));
            return barn;
        }

        ItemStack stack = stackFromStoredIcon(index);
        stack.set(DataComponents.ITEM_NAME, Component.literal(plotNameForDisplay(index)));
        return stack;
    }

    private ItemStack stackFromStoredIcon(int index) {
        String iconId = GardenPlotConfig.getPlotIconId(index);
        if (!iconId.isBlank()) {
            try {
                return new ItemStack(BuiltInRegistries.ITEM.getValue(Identifier.parse(iconId)));
            } catch (Throwable ignored) {
                GardenPlotButtonsClient.LOGGER.debug("Invalid Garden plot icon id: {}", iconId);
            }
        }
        return new ItemStack(Items.GRAY_STAINED_GLASS_PANE);
    }

    private String plotNameForDisplay(int index) {
        String learned = GardenPlotConfig.getPlotName(index);
        return learned.isBlank() ? "Unknown Plot " + (index + 1) : learned;
    }

    private String commandName(int index) {
        if (index == 12) return "barn";
        return GardenPlotConfig.getPlotName(index);
    }

    private boolean inHeader(double mouseX, double mouseY) {
        return inArea(mouseX, mouseY, getX(), getY(), getWidth(), HEADER_HEIGHT);
    }

    private static boolean inArea(double mouseX, double mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput builder) {
    }
}
