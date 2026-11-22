<div align="center">

# 🎭 NickHider (Fabric 1.8.9)

**A modern, lightweight, and powerful client-side nickhider mod for Minecraft 1.8.9.**

Hide your real username, spoof any skin on-the-fly, conceal server-side `/nick` aliases, and protect your privacy while streaming or recording. Fully integrated with **OneClient** and **OneConfig**.

[![Minecraft](https://img.shields.io/badge/Minecraft-1.8.9-brightgreen.svg?style=for-the-badge&logo=minecraft)](https://www.minecraft.net/)
[![Fabric](https://img.shields.io/badge/Modloader-Fabric%20%2F%20Ornithe-blue.svg?style=for-the-badge)](https://fabricmc.net/)
[![OneConfig](https://img.shields.io/badge/Compatibility-OneConfig%20%2F%20OneClient-purple.svg?style=for-the-badge)](https://polyfrost.org/)
[![License](https://img.shields.io/badge/License-MIT-yellow.svg?style=for-the-badge)](LICENSE)
[![Author](https://img.shields.io/badge/Author-EdTrick-red.svg?style=for-the-badge)](https://github.com/EdTrick)

<br/>

<img src="./images/logo.png" alt="NickHider Logo" width="180" />

</div>

---

## 📖 Table of Contents

- [Overview](#-overview)
- [✨ Key Features](#-key-features)
  - [1. Advanced Nick Hiding](#1-advanced-nick-hiding)
  - [2. Instant Client-Side Skin Changer](#2-instant-client-side-skin-changer)
  - [3. Player Skull & Inventory Integration](#3-player-skull--inventory-integration)
  - [4. OneConfig & OneClient UI Support](#4-oneconfig--oneclient-ui-support)
  - [5. Interactive In-Game Commands & Autocompletion](#5-interactive-in-game-commands--autocompletion)
  - [6. Hypixel Bedwars Stars & Level Spoofing](#6-hypixel-bedwars-stars--level-spoofing)
  - [7. Server-Side /nick & Hypixel Replay Support](#7-server-side-nick--hypixel-replay-support)
  - [8. Hypixel Network Level Spoofing](#8-hypixel-network-level-spoofing)
- [📸 Screenshots & Showcase](#-screenshots--showcase)
- [⌨️ Commands Reference](#️-commands-reference)
- [⚙️ Configuration](#️-configuration)
- [📦 Installation & Requirements](#-installation--requirements)
- [🛠️ Building from Source](#️-building-from-source)
- [🤝 Credits & Author](#-credits--author)
- [📄 License](#-license)

---

## 🔍 Overview

**NickHider** was engineered specifically for Minecraft 1.8.9 on Fabric / Ornithe / OneClient to give content creators, streamers, and competitive players complete control over their client-side identity.

Unlike legacy nick hiders that blindly replace entire lines of text (breaking scoreboard colors, team prefixes, health counters, or server-side `/nick` commands), **NickHider** uses surgical bytecode mixins that only replace your username where it matters, leaving all formatting, surrounding text, and server gameplay mechanics 100% intact.

---

## ✨ Key Features

### 1. Advanced Nick Hiding
- **Comprehensive Coverage:** Your real in-game name (IGN) is disguised seamlessly across:
  - 💬 **Chat & Private Messages** (incoming and outgoing)
  - 📋 **Tablist (Player List)**
  - 🏷️ **Nametags & Above-Head Displays**
  - 📊 **Scoreboards & Sidebars** (preserving team prefixes, health values, and styles)
  - 📜 **Holograms & Leaderboards**
  - 🎬 **Titles, Subtitles & Actionbars** (e.g. victory and win banners)
  - 🗂️ **Item Tooltips & Inventories**
  - ⚔️ **Hotbar Experience (XP) Bar** (dynamically spoofed inside Bedwars matches)
  - 👤 **Lobby Profile NPCs** (e.g. "Your Bed Wars Profile", "Your SkyWars Profile")
  - 📼 **Hypixel Replay Mode** (full disguise for your replay entity)
- **Full Color & Style Code Support:** Custom nicknames support standard Minecraft color formatting (`&0` - `&f`) and styles (`&l` bold, `&o` italic, `&n` underline, etc.). Example: `&6&lEdTrick` or `&eE&bd&aTrick`. Color and formatting codes are preserved even when server announcements or game modes add their own styles.
- **Dynamic Server Nick Detection:** If you play on servers with their own `/nick` system (such as Hypixel, Minemen Club, etc.), NickHider automatically detects your randomized server nick and replaces it with your chosen fake nick everywhere.
- **Smart Preservation:** Does not break team prefixes, health icons, scoreboard points, or server ranks.

<img src="./images/1.png" alt="Hidden Nick & Skin" width="720" />

---

### 2. Instant Client-Side Skin Changer
- **Zero World Reconnects:** Switch skins dynamically in real time without having to rejoin the server or change dimensions.
- **High-Reliability Multi-Source API:** Fetches skins using an intelligent fallback pipeline (`PlayerDB` ➔ `Crafthead` ➔ `Mojang Session API`) to ensure 99.9% uptime and avoid rate-limiting.
- **Server Skin Override:** Overrides server-enforced skins (e.g. randomized skins given by server-side `/nick`) when a fake skin is set, and seamlessly restores your original account skin when disabled.
- **Supports Both Skin Models:** Works natively with both **Classic** (4-pixel Steve) and **Slim** (3-pixel Alex) skins.

<img src="./images/2.png" alt="Hidden Skin" width="250" />

---

### 3. Player Skull & Inventory Integration
- **3D Skull Head Rendering:** Any player skull matching your player (in your inventory, hotbar, open chests, held items, armor/helmet slot, or placed in the world) automatically renders the 3D head of your fake skin.
- **Display Name Localization:** Player skull items dynamically display your fake nickname (e.g. `EdTrick's Head` instead of your real IGN).

<img src="./images/3.png" alt="Hidden Skin Skull" width="720" />

---

### 4. OneConfig & OneClient UI Support
- **Native OneConfig Menu:** Configurable directly via the OneConfig graphical menu (`Right Shift`). Found under **Visuals ➔ NickHider**.
- **Intuitive Toggles:** Independent `Enable Nick Hider` and `Enable Skin Hider` switches with dedicated text inputs for instant adjustments.
- **OneClient Sidebar Account Protection:** Replaces your real account name in the bottom-left corner of the OneClient mod menu (next to your skin avatar) without interfering with Mojang authentication or PolyPlus session tokens.

<img src="./images/4.png" alt="OneConfig Support" width="720" />

---

### 5. Interactive In-Game Commands & Autocompletion
- **Full Tab-Completion:** Press `Tab` on `/nick<TAB>` or `/nickhider<TAB>` to complete all subcommands and parameters.
- **Clickable Chat Menu:** Running `/nickhider help` prints an elegant, numbered list of commands with hover descriptions and click-to-prepare suggestions.

<img src="./images/5.png" alt="Interactive Help Menu" width="720" />

---

### 6. Hypixel Bedwars Stars & Level Spoofing
- **All 101 Hypixel Prestiges (Levels 0 to 10,000):** Accurate support for every prestige tier from Stone `§7[0✫]` all the way to Prestigious `§9[§b1§f0000§c✭§4]`, including custom digit colors, prestige brackets, and star icons (`✫`, `✪`, `⚝`, `✥`, `✭`).
- **Mix & Match Customization:** Combine any star level with any prestige color palette and any star icon (e.g. `/nickhider bwlvl stars 1402`, `/nickhider bwlvl color 1200`, `/nickhider bwlvl icon 2000` -> `§7[§e1402§6⚝§7]`).
- **Universal Multi-Display Spoofing:**
  - 💬 **Chat Messages** (incoming and outgoing messages featuring your player)
  - 📋 **Tablist & Player Names** (overrides tablist stars and external star indicator mods)
  - 📊 **Sidebar Scoreboard** (including `Stars: X` lines and player team rows)
  - ⚔️ **Hotbar Experience (XP) Bar** (accurately replaces Bedwars level inside active matches without touching Hypixel Network level in lobbies)
  - 🎬 **Actionbar & Middle-Screen Display** (text above your hotbar/inventory)
  - 🏷️ **Nametags, Titles, Subtitles & Holograms**
- **Interactive Chat Selection Menus:**
  - `/nickhider bwlvl color`: Displays an interactive chat palette showing all 101 prestiges formatted **5 per line**; clicking any prestige immediately applies its colors.
  - `/nickhider bwlvl icon`: Displays an interactive chat palette of star icons formatted **5 per line** to click and select.
- **OneConfig Support:** Configurable via **Visuals ➔ NickHider ➔ Bedwars Level** in the OneConfig menu.

<img src="./images/12.png" alt="Bedwars Level Spoofing" width="720" />

---

### 7. Server-Side /nick & Hypixel Replay Support
- **Automatic Server-Side Nick Disguise:**
  - When playing on Hypixel or servers with `/nick`, NickHider automatically detects your server-assigned randomized nick when chatting or from Hypixel's `/nick` messages.
  - Replaces the randomized nick with your chosen fake nick everywhere, keeping all formatting, rank colors, and game mode announcements (e.g. Murder Mystery announcements, Bedwars shout, etc.) intact.
  - Manual override available at any time via `/nickhider servernick [NICK]` (alias: `/nickhider snick`), or clear with `/nickhider servernick reset`.
- **Lobby Profile NPC Disguise:**
  - Cloned profile NPCs in lobbies (such as the Bedwars "Your Bed Wars Profile" NPC) that replicate your real account skin are automatically recognized and disguised with your custom fake skin and spoofed Bedwars stats.
- **Hypixel Replay Mode (`/nickhider replay`):**
  - In Hypixel replays, your character is loaded as a separate player entity (`EntityOtherPlayerMP`) with a randomized nick and skin.
  - Running `/nickhider replay` prints a clickable chat list of all players in the replay; clicking your replay identity instantly applies your custom fake nick and skin to your replay character.
  - Can also be set directly with `/nickhider replay [NICK]` (alias: `/nickhider replaynick`).

### 8. Hypixel Network Level Spoofing
- **Comprehensive Disguise:** Disguises your general Hypixel Network Level everywhere it appears across the server:
  - 🏷️ **Nametags & LevelHead:** Spoofs `Level: 95` above your character's head to your fake level.
  - 📋 **Player Profile GUI:** In the Character Information skull tooltip hover menu, your level is seamlessly replaced.
  - 📊 **Main Lobby & Game Scoreboards:** Disguises `Hypixel Level: 95` / `Network Level: 95` in the sidebar.
  - 🧪 **Lobby Experience Bar:** Overwrites the XP level counter between your hearts and hunger bar in lobbies.
  - 💬 **Chat & Level Up Messages:** Intercepts level announcements and `/level` outputs.
- **Full Control:** Easily configure any level with `/nickhider level <lvl>` (or `/nlvl <lvl>`), toggle ON/OFF with `/nlvl toggle`, or restore your real level with `/nlvl reset`.

---

## 📸 Screenshots & Showcase

| Feature | Demonstration |
| :--- | :--- |
| **In-Game Tablist & Chat** | <img src="./images/6.png" alt="TabList with Hidden Nick & Skin" /> |
| **Scoreboard & Nametags** | <img src="./images/7.png" alt="Scoreboard with Hidden Nick" /> |
| **Titles & Subtitles** | <img src="./images/8.png" alt="Title with Hidden Nick" /> |
| **Holograms & Leaderboards** | <img src="./images/9.png" alt="Hologram with Hidden Nick" /> |
| **Inventory & Player Skulls** | <img src="./images/10.png" alt="Inventory Spoof" /> |
| **OneConfig Menu Interface** | <img src="./images/4.png" alt="OneConfig Support" /> |
| **OneClient Sidebar** | <img src="./images/11.png" alt="OneClient Profile Spoof" /> |
| **Bedwars Level & In-Game XP** | <img src="./images/12.png" alt="Bedwars Level Spoofing" /> |
| **Lobby Profile NPC Disguise** | <img src="./images/13.png" alt="Lobby Profile NPC Disguise" /> |
| **Hypixel Replay Mode** | <img src="./images/14.png" alt="Hypixel Replay Disguise" /> |

---

## ⌨️ Commands Reference

You can use `/nickhider`:

| Command | Description | Example |
| :--- | :--- | :--- |
| `/nickhider nick <name>` | Sets a fake client-side nickname (supports `&` color codes) | `/nickhider nick &6&lEdTrick` |
| `/nickhider unnick` | Resets nickname back to your real account name | `/nickhider unnick` |
| `/nickhider skin <player>` | Sets your client-side skin to any Minecraft player's skin | `/nickhider skin Kolijn` |
| `/nickhider realskin` | Resets skin back to your real account skin | `/nickhider realskin` |
| `/nickhider servernick [nick]` | Shows, sets, or clears (`reset`) your server-side `/nick` | `/nickhider servernick SpookyAndCute` |
| `/nickhider replay [nick]` | Interactive player selector for Hypixel replays or sets replay nick | `/nickhider replay` |
| `/nickhider bwlvl` | Displays Bedwars level spoofing control panel & status | `/nickhider bwlvl` |
| `/nickhider bwlvl stars <lvl>` | Sets fake Bedwars stars level (0 - 10000) | `/nickhider bwlvl stars 1402` |
| `/nickhider bwlvl color [preset]`| Opens prestige color menu or sets a color scheme | `/nickhider bwlvl color 1200` |
| `/nickhider bwlvl icon [preset]` | Opens star icon menu or sets a custom icon | `/nickhider bwlvl icon 2000` |
| `/nickhider bwlvl toggle` | Toggles Bedwars level spoofing ON or OFF | `/nickhider bwlvl toggle` |
| `/nickhider bwlvl reset` | Resets Bedwars spoofing back to default settings | `/nickhider bwlvl reset` |
| `/nickhider level <lvl>` | Sets fake Hypixel Network Level (0 - 10000) | `/nickhider level 250` |
| `/nickhider level reset` | Disables network level spoofing and restores real level | `/nickhider level reset` |
| `/nickhider level toggle`| Toggles network level spoofing ON or OFF | `/nickhider level toggle` |
| `/nickhider level` | Displays interactive network level control panel & status | `/nickhider level` |
| `/nickhider help` | Displays the interactive command help menu | `/nickhider help` |

> [!NOTE]
> **Command Aliases:**
> - `/nickhider servernick` ➔ `/nickhider snick`
> - `/nickhider replay` ➔ `/nickhider replaynick`
> - `/nickhider bwlvl` ➔ `/nickhider bedwars`
> - `/nickhider level` ➔ `/nickhider nlvl`, `/nlvl`, `/networklevel`

> [!TIP]
> You can click on any command in the `/nickhider help` chat message to auto-fill it directly into your chat bar!

---

## ⚙️ Configuration

NickHider stores its settings in two locations depending on whether you use OneConfig:

1. **Standard Configuration File:**
   - Located at: `.minecraft/config/nickhider/nickhider.properties`
   - Properties:
     ```properties
     # NickHider configuration by EdTrick
     fakeNick=&6&lEdTrick
     fakeSkin=Kolijn
     ```
2. **OneConfig GUI:**
   - Press **Right Shift** (or your OneConfig menu keybind).
   - Navigate to **Visuals ➔ NickHider**.
   - Toggle switches or enter nicknames/skins in real-time. Changes save automatically!

---

## 📦 Installation & Requirements

### Requirements:
- **Minecraft:** `1.8.9`
- **Mod Loader:** [Fabric Loader (0.15+)](https://fabricmc.net/) or [Ornithe](https://ornithemc.net/) / [Polyfrost OneClient](https://polyfrost.org/)
- **Java:** `Java 8` or higher
- *(Optional)* [OneConfig](https://polyfrost.org/) for the graphical in-game settings menu.

### Installation Steps:
1. Download the latest `nickhider-x.x.x.jar` from the [Releases](https://github.com/EdTrick/NickHider-OneClient/releases/latest) tab.
2. Place the downloaded `.jar` file into your `.minecraft/mods` folder (or your OneClient cluster `mods` folder).
3. Launch Minecraft 1.8.9 with Fabric / OneClient.

---

## 🛠️ Building from Source

To compile the mod from source code:

1. Clone the repository:
   ```bash
   git clone https://github.com/EdTrick/nickhider-fabric-1.8.9.git
   cd nickhider-fabric-1.8.9
   ```

2. Build using the Gradle wrapper:
   - **Windows:**
     ```cmd
     .\gradlew.bat build
     ```
   - **Linux / macOS:**
     ```bash
     ./gradlew build
     ```

3. The finished `.jar` file will be located in `build/libs/nickhider-1.2.1.jar`.

---

## 🤝 Credits & Author

- **Author & Developer:** **EdTrick**
  - GitHub: [@EdTrick](https://github.com/EdTrick)
- Built with **Fabric Loom**, **Ploceus** (Ornithe MCP mappings for 1.8.9), and **OneConfig API**.

---

## 📄 License

This project is licensed under the **MIT License**. See the [LICENSE](LICENSE) file for more details.

<div align="center">
  <sub>Crafted by <b>EdTrick</b> for Minecraft 1.8.9</sub>
</div>
