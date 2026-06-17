# Garden Plot Buttons

A small standalone Fabric client mod for Minecraft 26.1.2.

It extracts only the Garden plot teleport widget idea from Skyblocker, without requiring the full Skyblocker mod.

## Features

- Adds a 5x5 Garden plot panel when the player opens the inventory.
- Left-click a learned plot to send `/plottp <plot name>`.
- The Barn sends `/plottp barn`.
- Bottom buttons send `/desk`, `/warp garden`, and `/setspawn`.
- Drag the top bar to move the UI.
- Right-click the top bar to reset the UI position.
- Position is saved in `.minecraft/config/gardenplotbuttons.json`.

## How to learn plot names

The mod does not know your plot names until it sees Hypixel's Configure Plots menu.

1. Join SkyBlock Garden.
2. Run `/desk`.
3. Open Configure Plots.
4. Close that screen.
5. Reopen inventory.

The mod will save the plot names to `gardenplotbuttons.json`.

## Build

Use the included GitHub Actions workflow or run:

```bash
gradle build
```

Use Java 25.

## Credit

Inspired by Skyblocker's Garden Plots Widget. Skyblocker is LGPL-3.0 licensed.


## v1.0.1

- Added Garden-only display detection. The inventory widget is only added when the Hypixel sidebar location looks like Garden.
- Added config option `onlyShowInGarden`, default `true`. Set it to `false` in `config/gardenplotbuttons.json` if you want the panel everywhere.

## v1.0.2 notes

- Learns both plot names and plot marker icons from `/desk` -> `Configure Plots`.
- The inventory overlay now renders the learned marker item for each plot, for example white stained glass for Plot 1 if that is what the desk GUI shows.
- Garden-only detection now accepts sidebar lines containing `Garden` or `Plot`, so it should show across the whole Garden, not only the Barn.
