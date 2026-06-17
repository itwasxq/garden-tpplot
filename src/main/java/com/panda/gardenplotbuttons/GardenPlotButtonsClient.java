package com.panda.gardenplotbuttons;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.ContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.scores.DisplaySlot;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.ScoreHolder;
import net.minecraft.world.scores.Scoreboard;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Locale;

public final class GardenPlotButtonsClient implements ClientModInitializer {
    public static final String MOD_ID = "gardenplotbuttons";
    public static final Logger LOGGER = LoggerFactory.getLogger("GardenPlotButtons");

    @Override
    public void onInitializeClient() {
        GardenPlotConfig.load();

        ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
            if (isConfigurePlotsScreen(screen) && screen instanceof ContainerScreen containerScreen) {
                ScreenEvents.remove(screen).register(removedScreen -> parseConfigurePlots(containerScreen));
                return;
            }

            if (screen instanceof InventoryScreen inventoryScreen) {
                if (GardenPlotConfig.onlyShowInGarden() && !isInGarden()) return;

                GardenPlotWidget widget = new GardenPlotWidget(GardenPlotConfig.getX(scaledWidth), GardenPlotConfig.getY(scaledHeight));
                Screens.getWidgets(inventoryScreen).add(widget);
            }
        });

        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> GardenPlotConfig.save());
        LOGGER.info("Garden Plot Buttons loaded successfully!");
    }

    private static boolean isConfigurePlotsScreen(Screen screen) {
        String title = ChatFormatting.stripFormatting(screen.getTitle().getString());
        return title != null && title.trim().equalsIgnoreCase("Configure Plots");
    }

    private static void parseConfigurePlots(ContainerScreen screen) {
        if (!(screen.getMenu() instanceof ChestMenu menu)) return;
        boolean changed = false;

        for (int row = 0; row < 5; row++) {
            for (int slotId = row * 9 + 2; slotId < row * 9 + 7; slotId++) {
                if (slotId == 22) continue; // Barn icon slot.
                if (slotId < 0 || slotId >= menu.slots.size()) continue;

                Slot slot = menu.slots.get(slotId);
                ItemStack stack = slot.getItem();
                if (stack == null || stack.isEmpty()) continue;
                if (stack.is(Items.RED_STAINED_GLASS_PANE) || stack.is(Items.OAK_BUTTON) || stack.is(Items.BLACK_STAINED_GLASS_PANE)) continue;

                String rawName = ChatFormatting.stripFormatting(stack.getHoverName().getString());
                if (rawName == null || rawName.isBlank()) continue;

                String plotName = extractPlotName(rawName);
                if (plotName.isBlank()) continue;

                int plotIndex = row * 5 + (slotId % 9 - 2);
                GardenPlotConfig.setPlotName(plotIndex, plotName);
                GardenPlotConfig.setPlotIconId(plotIndex, BuiltInRegistries.ITEM.getKey(stack.getItem()).toString());
                changed = true;
            }
        }

        if (changed) {
            GardenPlotConfig.save();
            LOGGER.info("Learned Garden plot names from Configure Plots screen.");
        }
    }

    private static String extractPlotName(String rawName) {
        String clean = rawName.trim();
        String[] parts = clean.split("-", 2);
        if (parts.length >= 2) return parts[1].trim();
        return clean;
    }

    /**
     * Lightweight Garden detection copied from the same idea Skyblocker uses: read the Hypixel sidebar.
     * The Garden sidebar normally contains a location line like "⏣ Garden".
     */
    static boolean isInGarden() {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || client.level == null) return false;

        try {
            ClientLevel world = client.level;
            Scoreboard scoreboard = world.getScoreboard();
            Objective objective = scoreboard.getDisplayObjective(DisplaySlot.BY_ID.apply(1));
            if (objective == null) return false;

            String title = clean(objective.getDisplayName());
            if (looksLikeGardenLine(title)) return true;

            for (ScoreHolder scoreHolder : scoreboard.getTrackedPlayers()) {
                if (!scoreboard.listPlayerScores(scoreHolder).containsKey(objective)) continue;

                PlayerTeam team = scoreboard.getPlayersTeam(scoreHolder.getScoreboardName());
                if (team == null) continue;

                String line = clean(Component.empty().append(team.getPlayerPrefix().copy()).append(team.getPlayerSuffix().copy()));
                if (looksLikeGardenLine(line)) return true;
            }
        } catch (Throwable t) {
            // Location detection should never break inventory rendering.
            LOGGER.debug("Failed to detect Garden sidebar location.", t);
        }

        return false;
    }

    private static String clean(Component component) {
        String text = component == null ? "" : component.getString();
        String stripped = ChatFormatting.stripFormatting(text);
        return stripped == null ? "" : stripped.trim();
    }

    private static boolean looksLikeGardenLine(String line) {
        if (line == null || line.isBlank()) return false;
        String lower = line.toLowerCase(Locale.ROOT);
        // Hypixel location line is usually "⏣ Garden". Keep the fallback broader for unicode/icon changes.
        return lower.contains("⏣ garden")
                || lower.contains("⏣ plot")
                || lower.startsWith("plot ")
                || lower.startsWith("plot:")
                || lower.contains(" plot -")
                || lower.contains(" plot:")
                || lower.equals("garden")
                || lower.contains(" garden");
    }

    static void sendCommand(String command) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null) return;

        String normalized = command.trim();
        if (normalized.startsWith("/")) normalized = normalized.substring(1);
        client.player.connection.sendCommand(normalized);
    }

    static void clickSound() {
        Minecraft client = Minecraft.getInstance();
        if (client.getSoundManager() == null) return;
        client.getSoundManager().play(net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK.value(), 1.0F));
    }
}
