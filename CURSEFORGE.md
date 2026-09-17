# 💧 AE2 Virtual Well

**Bring virtual fluid extraction, automated infinite wells, and digital liquid generation directly into your Applied Energistics 2 ME Network!**

Requires **Applied Energistics 2** and **NeoForge (Minecraft 26.1.2)**.

---

### 🌟 What is AE2 Virtual Well?

**AE2 Virtual Well** introduces generative **Virtual Well Storage Cells** to Applied Energistics 2. Instead of building massive, server-lagging in-world fluid pump setups, ticking chunk-loaded water pools, or extensive Nether lava draining stations, you can now virtualize liquid extraction straight inside your ME Drives or ME Chests!

Simply train a Well Cell to any liquid—such as water, lava, milk, or modded fluids—insert it into a powered ME Drive, and the cell will passively generate clean liquid directly into your digital storage on a regular schedule.

---

### ✨ Key Features

* 📦 **5 Cell Tiers (1k to 256k):** Fluid yields scale with cell tier (every 3 seconds / 60 ticks):
  * **1k Well Cell:** 1,000 mB (1 Bucket) per cycle (0.5 AE/t idle drain)
  * **4k Well Cell:** 4,000 mB (4 Buckets) per cycle (1.0 AE/t idle drain)
  * **16k Well Cell:** 16,000 mB (16 Buckets) per cycle (2.0 AE/t idle drain)
  * **64k Well Cell:** 64,000 mB (64 Buckets) per cycle (4.0 AE/t idle drain)
  * **256k Well Cell:** 256,000 mB (256 Buckets) per cycle (8.0 AE/t idle drain)
* 🛑 **Zero Network Flooding (Smart Auto-Stop):**
  * Generated liquids are stored **strictly** inside the Well Cell itself.
  * Once the cell reaches maximum byte or type capacity (`CellState.FULL`), generation **automatically halts**.
  * Liquids will **never** overflow into other drives or fluid storage cells in your ME network!
* ⚡ **Zero Energy Waste on Overflow:** When a cell is full, it consumes **zero AE power** for unproduced fluid until space becomes available.
* 🌊 **Universal Fluid Support (Out of the Box):**
  * **Water:** Compact, infinite digital water generation without water source blocks.
  * **Lava:** High-throughput geothermal lava generation for generators, obsidian crafters, and processing lines.
  * **Milk:** Clean fluid milk generation without needing entities or milking machines.
  * **Any Modded Fluid:** Automatically discovers and supports any registered fluid in the game (*Create*, *Mekanism*, *Thermal*, *Ender IO*, and more) without requiring manual configuration!
* 🛠️ **Three Intuitive Training Methods:**
  * **In-Hand Fast Config:** Hold the Well Cell in your main hand and a fluid container (Water Bucket, Lava Bucket, fluid tank) in your off-hand, then **Sneak + Right-Click** to train immediately!
  * **World Liquid Sampling:** Hold the Well Cell and **Sneak + Right-Click directly on any liquid block in the world** (water source, lava pool, modded liquid) to sample and train the cell on the spot!
  * **AE2 Cell Workbench:** Partition the cell using standard AE2 Cell Workbench filter slots with either fluid containers or ME fluid keys.
  * **Quick Reset:** Sneak + Right-Click in the air with an empty off-hand to instantly clear the cell's training.
* 📊 **Authentic AE2 Tooltip & Terminal Integration:**
  * Real-time byte & type usage display ("*X of Y Bytes used*") with dynamic color feedback (Green → Orange → Red).
  * Hover preview showing installed upgrade cards and live stored fluids with amounts.
  * Fully accessible by ME Fluid Terminals, Fluid Storage Monitors, Fluid Export/Import Buses, and P2P Fluid Tunnels.
* 📋 **Datapack Extensible:** Add custom liquid generation recipes, bonus secondary drops, or custom yields via standard JSON recipes (`ae2virtualwell:well_drop`).

---

### 🔨 Crafting Recipes

All items integrate naturally into AE2's tier progression:

#### 1. Well Cell Housing
```
[ Quartz Glass ] [ Redstone    ] [ Quartz Glass ]
[ Redstone     ] [ Bucket      ] [ Redstone     ]
[ Iron Ingot   ] [ Iron Ingot  ] [ Iron Ingot   ]
```

#### 2. 1k Well Storage Component (Shapeless)
* Combine **1x 1k ME Storage Component** + **1x Bucket** + **1x Copper Ingot**.

#### 3. Higher Tier Components (4k, 16k, 64k, 256k)
* Crafted following AE2's tier upgrade progression:
  * Combine **3x previous tier Well Cell Components** + **1x AE2 Calculation Processor** + **Quartz Glass** + **Redstone**.

#### 4. Complete Storage Cells
* Shapeless recipe: Combine a **Well Cell Housing** with any **Well Cell Component**.

---

### ⚙️ Configuration Options

All gameplay values are fully customizable in `config/ae2virtualwell-common.toml`:
* **Generation Interval:** Adjust how often generation cycles run (default: `60` ticks / 3.0 seconds).
* **AE Power Requirement:** Toggle whether liquid generation requires network power (default: `true`).
* **Energy Cost:** Customize the AE energy consumed per 1,000 mB generated (default: `10.0` AE).
* **Tier Yields:** Customize the exact millibucket output per cycle for each tier (1k to 256k).

---

### ❓ Frequently Asked Questions (FAQ)

**Q: Can I use this mod in my modpack?**
> **Yes, absolutely!** You are free to include AE2 Virtual Well in any public or private modpack on CurseForge, Modrinth, or custom launchers.

**Q: Does it work with ME Chests as well as ME Drives?**
> Yes! Virtual Well Cells work seamlessly in both standard ME Drives and ME Chests.

**Q: What happens when the cell gets full?**
> Production automatically pauses immediately. No AE power is consumed for unproduced fluid, and zero fluid spills into other cells in your ME network. As soon as you withdraw liquid, generation resumes automatically.

**Q: How do I clear or change the trained liquid?**
> You can clear it either in an AE2 Cell Workbench, or by holding the cell in your main hand with an empty off-hand and pressing **Sneak + Right-Click**. You can retrain it anytime by sneak-right clicking a new liquid container or a liquid block in the world.

**Q: Can I extract the fluids into pipes or tanks?**
> Yes! Because the fluids are stored in native AE2 fluid format (`AEFluidKey`), you can pump them out using standard ME Fluid Export Buses, ME Fluid Interfaces, or fill buckets/tanks directly through the ME Fluid Terminal.

---

### 📦 Dependencies

* **Minecraft 26.1.2**
* **NeoForge 26.1.2.108+**
* **Applied Energistics 2 (AE2) 26.1.11-beta+**
