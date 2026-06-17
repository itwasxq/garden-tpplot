package com.panda.gardenplotbuttons;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

public final class GardenPlotConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_PATH = FabricLoader.getInstance().getConfigDir().resolve("gardenplotbuttons.json");

    private static Data data = new Data();

    private GardenPlotConfig() {}

    public static void load() {
        if (!Files.exists(CONFIG_PATH)) {
            data = new Data();
            save();
            return;
        }

        try (Reader reader = Files.newBufferedReader(CONFIG_PATH)) {
            Data loaded = GSON.fromJson(reader, Data.class);
            data = loaded == null ? new Data() : loaded;
            normalize();
        } catch (Exception e) {
            GardenPlotButtonsClient.LOGGER.warn("Failed to load Garden Plot Buttons config; using defaults.", e);
            data = new Data();
        }
    }

    public static void save() {
        normalize();
        try {
            Files.createDirectories(CONFIG_PATH.getParent());
            try (Writer writer = Files.newBufferedWriter(CONFIG_PATH)) {
                GSON.toJson(data, writer);
            }
        } catch (IOException e) {
            GardenPlotButtonsClient.LOGGER.warn("Failed to save Garden Plot Buttons config.", e);
        }
    }

    private static void normalize() {
        if (data.plotNames == null || data.plotNames.length != 25) {
            String[] fixed = new String[25];
            if (data.plotNames != null) System.arraycopy(data.plotNames, 0, fixed, 0, Math.min(data.plotNames.length, fixed.length));
            data.plotNames = fixed;
        }
        if (data.plotIconIds == null || data.plotIconIds.length != 25) {
            String[] fixed = new String[25];
            if (data.plotIconIds != null) System.arraycopy(data.plotIconIds, 0, fixed, 0, Math.min(data.plotIconIds.length, fixed.length));
            data.plotIconIds = fixed;
        }
    }

    public static int getX(int screenWidth) {
        if (data.x < 0) return defaultX(screenWidth);
        return clamp(data.x, 0, Math.max(0, screenWidth - GardenPlotWidget.WIDTH));
    }

    public static int getY(int screenHeight) {
        if (data.y < 0) return defaultY(screenHeight);
        return clamp(data.y, 0, Math.max(0, screenHeight - GardenPlotWidget.HEIGHT));
    }

    public static void setPosition(int x, int y) {
        data.x = x;
        data.y = y;
        save();
    }

    public static void resetPosition(int screenWidth, int screenHeight) {
        data.x = defaultX(screenWidth);
        data.y = defaultY(screenHeight);
        save();
    }

    public static boolean closeInventoryOnPlotClick() {
        return data.closeInventoryOnPlotClick;
    }

    public static boolean onlyShowInGarden() {
        return data.onlyShowInGarden;
    }

    public static String getPlotName(int index) {
        normalize();
        if (index < 0 || index >= data.plotNames.length) return "";
        String name = data.plotNames[index];
        return name == null ? "" : name.trim();
    }

    public static void setPlotName(int index, String name) {
        normalize();
        if (index < 0 || index >= data.plotNames.length) return;
        data.plotNames[index] = name == null ? "" : name.trim();
    }

    public static String getPlotIconId(int index) {
        normalize();
        if (index < 0 || index >= data.plotIconIds.length) return "";
        String iconId = data.plotIconIds[index];
        return iconId == null ? "" : iconId.trim();
    }

    public static void setPlotIconId(int index, String iconId) {
        normalize();
        if (index < 0 || index >= data.plotIconIds.length) return;
        data.plotIconIds[index] = iconId == null ? "" : iconId.trim();
    }

    public static boolean hasLearnedPlots() {
        normalize();
        return Arrays.stream(data.plotNames).anyMatch(s -> s != null && !s.isBlank());
    }

    private static int defaultX(int screenWidth) {
        return Math.max(4, screenWidth / 2 + 92);
    }

    private static int defaultY(int screenHeight) {
        return Math.max(4, (screenHeight - GardenPlotWidget.HEIGHT) / 2);
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static final class Data {
        int x = -1;
        int y = -1;
        boolean closeInventoryOnPlotClick = true;
        boolean onlyShowInGarden = true;
        String[] plotNames = new String[25];
        String[] plotIconIds = new String[25];
    }
}
