# AE2 Virtual Well

<div align="center">
  <img src="logo.png" alt="AE2 Virtual Well Logo" width="200" height="200" />

  **Virtual Fluid & Liquid Generation inside your ME Network for Minecraft 1.21.1 (NeoForge)**

  [![Minecraft](https://img.shields.io/badge/Minecraft-1.21.1-brightgreen.svg)](https://minecraft.net/)
  [![NeoForge](https://img.shields.io/badge/NeoForge-21.1.172-orange.svg)](https://neoforged.net/)
  [![Applied Energistics 2](https://img.shields.io/badge/Applied%20Energistics%202-19.2.10-blue.svg)](https://appliedenergistics.org/)
  [![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)
</div>

---

## 💧 About

**AE2 Virtual Well** is an official-style companion addon for **Applied Energistics 2** on **Minecraft 1.21.1 (NeoForge)**. It bridges digital ME network storage and virtual liquid extraction by introducing generative **Virtual Well Storage Cells**.

Insert a trained Well Cell into any standard **ME Drive** or **ME Chest**, supply AE power, and watch it generate water, lava, milk, or modded liquids directly into the cell every 3 seconds (60 ticks)!

---

## ✨ Features

- 📦 **5 Tiers of Virtual Well Cells:** 1k, 4k, 16k, 64k, and 256k storage cells.
- ⚡ **Scaled Production Rates:** Liquid yield per cycle scales with cell tier (1,000 mB up to 256,000 mB every 3 seconds).
- 🛑 **Zero Network Flooding & Smart Auto-Stop:**
  - Liquids are generated **strictly** into the well cell itself.
  - When the cell reaches full capacity (`CellState.FULL` or byte/type limits), generation completely halts.
  - No liquids ever spill over into other cells in your ME network!
- 🔋 **Zero Power Waste on Overflow:** When a cell is full, zero AE power is drained for unproduced fluid.
- 🌊 **Universal Fluid Coverage Out of the Box:**
  - **Water:** Infinite water generation directly inside digital storage.
  - **Lava:** High-throughput geothermal lava generation for obsidian generation, power, or thermal processing.
  - **Milk:** Clean milk generation without entity lag.
  - **Any Modded Fluid:** Automatically detects and generates any registered fluid (e.g. Mekanism, Thermal, Create, EnderIO, etc.) without requiring configuration.
- 📋 **Datapack Extensible:** Create or customize fluid generation tables via standard JSON datapacks using recipe type `ae2virtualwell:well_drop`.
- 🛠️ **Three Training & Configuration Methods:**
  - **In-Hand Quick Config:** Hold the Well Cell in your main hand and a fluid container (Water Bucket, Lava Bucket, fluid tank) in your offhand, then **Sneak + Right-Click** to train instantly!
  - **World Liquid Sampling:** Hold the Well Cell and **Sneak + Right-Click directly on a liquid block in the world** (water source, lava pool, modded liquid) to sample and train the cell!
  - **Cell Workbench (AE2 Native):** Put the cell in a Cell Workbench and configure the partition filter slot with your desired fluid or fluid container.
  - **Resetting:** Sneak + Right-Click in the air with an empty offhand to reset/clear configuration.
- 📊 **Native AE2 Tooltips:** Displays byte and type usage with color coding, upgrade cards, generation yield, and trained liquid names.

---

## 📊 Cell Tiers & Rates

By default, generation cycles occur every **60 ticks (3.0 seconds)**:

| Tier | Capacity | Liquid Yield | Generation Rate | Idle Power Drain |
| :--- | :--- | :--- | :--- | :--- |
| **1k Virtual Well Cell** | 1,024 Bytes | **1,000 mB (1 Bucket)** | 1 Bucket / 3.0s | 0.5 AE/t |
| **4k Virtual Well Cell** | 4,096 Bytes | **4,000 mB (4 Buckets)** | 4 Buckets / 3.0s | 1.0 AE/t |
| **16k Virtual Well Cell** | 16,384 Bytes | **16,000 mB (16 Buckets)** | 16 Buckets / 3.0s | 2.0 AE/t |
| **64k Virtual Well Cell** | 65,536 Bytes | **64,000 mB (64 Buckets)** | 64 Buckets / 3.0s | 4.0 AE/t |
| **256k Virtual Well Cell** | 262,144 Bytes | **256,000 mB (256 Buckets)** | 256 Buckets / 3.0s | 8.0 AE/t |

*All generation amounts, tick intervals, and AE energy costs (default: 10.0 AE per 1,000 mB) are fully customizable in `config/ae2virtualwell-common.toml`.*

---

## 🔨 Crafting Recipes

### 1. Well Cell Housing
```
[ Quartz Glass ] [ Redstone    ] [ Quartz Glass ]
[ Redstone     ] [ Bucket      ] [ Redstone     ]
[ Iron Ingot   ] [ Iron Ingot  ] [ Iron Ingot   ]
```

### 2. 1k Well Cell Component (Shapeless)
- Combine **1x 1k ME Storage Component** + **1x Bucket** + **1x Copper Ingot**.

### 3. Higher Tier Components (4k, 16k, 64k, 256k)
Crafted following AE2's tier upgrade progression:
- Combine 3x previous tier Well Cell Components + 1x AE2 Calculation Processor + Quartz Glass + Redstone.

### 4. Complete Storage Cells
Shapeless recipe: Combine a **Well Cell Housing** with any **Well Cell Component**.

---

## ⚙️ Configuration

The configuration file is located at `config/ae2virtualwell-common.toml`:

```toml
[general]
  # Base interval in ticks between liquid generation cycles (20 ticks = 1 second)
  # Range: 1 ~ 72000 (Default: 60)
  baseTickInterval = 60

  # Whether liquid generation requires AE energy from the network
  # Default: true
  requireAeEnergy = true

  # AE energy consumed per 1,000 mB (1 Bucket) generated
  # Range: 0.0 ~ 100000.0 (Default: 10.0)
  energyPerBucket = 10.0

[tiers]
  # Yield in millibuckets per cycle
  tier1kMilliBuckets = 1000
  tier4kMilliBuckets = 4000
  tier16kMilliBuckets = 16000
  tier64kMilliBuckets = 64000
  tier256kMilliBuckets = 256000
```

---

## 📄 License

This mod is licensed under the [MIT License](LICENSE).
