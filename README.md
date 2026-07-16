<div align="center">

# Earthling

![Earthling](https://github.com/Mooshlol304/Earthling/blob/1.21.11/src/main/resources/assets/earthling/icon.png?raw=true)

*A Fabric client mod for EarthMC.*

[![Modrinth](https://img.shields.io/badge/Modrinth-Download-brightgreen)](https://modrinth.com/project/earthling#download)
![Minecraft](https://img.shields.io/badge/Minecraft-1.21.11-blue)
![Loader](https://img.shields.io/badge/Loader-Fabric-white)
![License](https://img.shields.io/badge/License-LGPL--3.0-orange)

</div>

---

Earthling is a client-side Fabric mod that adds tools and quality-of-life features for EarthMC. It focuses on making everyday tasks quicker, keeping useful information on screen, and reducing the amount of repetitive commands you have to run while playing.

Whether you're exploring, managing a town, recruiting players, or just chatting, Earthling aims to make the experience a little smoother.

---

## Getting Started

### Requirements

| | |
|---|---|
| Minecraft | `1.21.11` |
| Mod Loader | Fabric |
| Fabric API | Required |
| ModMenu | `>= 17.0.0` |
| MooshLib | Included with Earthling |

### Installation

1. Download the latest release from [Modrinth](https://modrinth.com/project/earthling#download).
2. Place the `.jar` file into your `.minecraft/mods` folder.
3. Install Fabric, Fabric API, and ModMenu if you haven't already.
4. Launch Minecraft and enjoy.

---

## Features

### `/ert` Commands

All Earthling commands are available through the `/ert` command.

| Command | Description |
|---|---|
| `calculate` | Looks up information for a set of coordinates. |
| `discord` | Displays a player's linked Discord account. |
| `gold` | Converts between gold ingots, blocks, and stacks. |
| `goto` | Finds the closest spawn for a town. |
| `lastseen` | Shows when a player was last online. |
| `locate` | Searches for towns and returns their coordinates. |
| `miningreset` | Resets the Mining Widget session. |
| `nearby` | Lists nearby players within a chosen radius. |
| `player` | Displays information about a player, including their map position. |
| `staffonline` | Shows which EarthMC staff members are online. |
| `townless` | Lists players without a town, with quick recruit buttons. |
| `wilderness` | Lists players currently visible in the wilderness. |

---

### Widgets

Widgets are movable HUD elements that stay visible while you play.

- **Nearby Players** – Shows nearby players in real time.
- **Player Info** – Displays useful information about your player
- **Mining Widget** – Tracks ores mined during your current session.

---

### Other Features

- **Experience Overlay** – Displays your progress through the current XP level.
- **Chat Preview** – Shows which EarthMC chat channel your next message will be sent to.
- **Player Affiliations** – Renders player town and nation tags above their heads.
- **Translation** – Translate chat messages with a single click. Also add `--languagecode` to the end of a chat message to speak in that language!
- **Update Reminder** – Lets you know when a newer version of Earthling is available.

---

## Screenshots

![Wilderness](https://cdn.modrinth.com/data/cached_images/8e4e02c84ed69de09348afec2a7a25d02ea20862.png)

*Lists all players in the wilderness without newbie protection.*

---

![StaffOnline](https://cdn.modrinth.com/data/cached_images/e0436ead252162dfeb29751412e36bd686586b0c.png)

*Shows every staff member currently online.*

---

![PlayerInfoWidget](https://cdn.modrinth.com/data/cached_images/cdba5c6ae42eb8ec83b3329aa00a096b37f2c749_0.webp)

*Displays basic information about a player in a compact widget.*

---

![CalculateCommand](https://cdn.modrinth.com/data/cached_images/5deff517ab857f592e09a898c6ff77baa1044b23.png)

*Looks up what's located at specified coordinates.*

---

![NearbyWidget](https://cdn.modrinth.com/data/cached_images/96ce3674e422d58b2bcd8893af5632f8bb0b37b3.png)

*Shows players currently within your rendered chunks.*

---

![Translate1](https://cdn.modrinth.com/data/cached_images/0ce2704060ab13d6d77603a3b055ce374891c423.png)
![Translate2](https://cdn.modrinth.com/data/cached_images/949e8b09fc6dc9acdbc7940ab63fe311256ad18e.png)

*Automatically translates chat messages into your chosen language.*

---

![Townless1](https://cdn.modrinth.com/data/cached_images/20e13745ed1877121bee73a348ab04e5add216fe.png)
![Townless2](https://cdn.modrinth.com/data/cached_images/402ec5a28c3acf898036d35f8aa64dcbce62e460.png)

*Finds townless players online, making recruitment much easier.*

---

## License

Licensed under [LGPL-3.0](./LICENSE.txt) — use it, modify it, distribute it.

## MooshLib

If you'd like to recreate Earthling in a developer environment, MooshLib, a required dependency of Earthling can be accessed by
creating a base mod with the modID `moosh-lib` and place it in `/libs`.

_FYI: MooshLib is a library that just counts how many players are using the mod!_

## Credits

Created and maintained by **Moosh**.