# Villager Trade Rotation

Fresh villager trades every day, without breaking a single job site block.

Villager Trade Rotation makes villager trades rotate on their own. Once a day by default, each villager rolls a new set of trades for every level it has unlocked, from Novice up to its current level, the same way vanilla picks trades. Its level and experience stay exactly as they are. Today's trades, new ones tomorrow.

The mod runs on the server only. Players join with a plain vanilla client and see the new trades in the normal trading screen.

Available for NeoForge, Forge and Fabric.

## Features

- **Daily rotation.** Every villager gets new trades once a day by default. The interval can be set to any number of days.
- **All unlocked levels.** A Master librarian gets new Novice, Apprentice, Journeyman, Expert and Master trades, two per level, rolled from that level's vanilla trade pool with the vanilla chances.
- **Level and experience stay.** Only the trades change. The villager keeps its level, its experience and its progress to the next level.
- **Every villager with a job.** Villagers nobody has traded with yet rotate too, and so do villagers whose trades are all still fresh.
- **Explorer maps without the lag.** Cartographer map trades rotate like every other trade. Each cartographer remembers the map it offered for each kind of explorer map, so when the same kind comes back it offers the same map again instead of searching the world for a structure.
- **Works with Villager Trade Peek.** When Villager Trade Peek is installed, its preview of the trades a villager unlocks later is renewed together with the trades.
- **Prices stay vanilla.** Reputation, cure discounts and Hero of the Village apply to the new trades exactly as in vanilla.
- **Server config.** Turn the rotation off, change the interval or exclude professions in one small TOML file.

## How it works

- **When trades rotate.** Every villager records the day of its last rotation. A day starts at sunrise, so sleeping through the night also starts a new day. Once the interval has passed (1 day by default), the villager rotates its trades the next time it works at its job site, at the moment vanilla checks whether it should restock. That is usually the morning after.
- **A job site is needed.** A villager rotates only while working at its own job site block. A villager that has lost its job site, or cannot reach it, keeps its trades until it works there again.
- **Never while you are trading.** If a player has the villager's trading screen open when it gets to work, the rotation waits for the next time the villager works at its job site.
- **What changes.** All trades are replaced. Each unlocked level gets two new trades from the vanilla pool of that level, like vanilla does when a villager reaches a level. The new trades are fresh: uses, demand and special prices start over. With the Villager Trade Rebalance experiment enabled, its trade pools are used, as in vanilla.
- **Explorer maps.** Each villager keeps one map per kind of explorer map, for example one ocean explorer map and one woodland explorer map. When a rotation picks a kind the villager already has a map for, the trade offers a copy of that map, pointing to the same structure, with the vanilla price and uses. Only a kind the villager has no map for yet makes the game search for a structure, once; the map found is then kept. If no structure is found, vanilla picks another trade of that level instead and nothing is kept. Maps that are already in a villager's trades before a rotation, including maps from before the mod was installed, are kept too.
- **Bought maps come back on purpose.** A player who already bought an explorer map from a cartographer and sees the same kind offered again gets a map to the same structure. That is intended: the map belongs to that villager.
- **Days without daylight.** The day count follows the world's time of day. With `doDaylightCycle` off, the day never changes and villagers do not rotate. If the time is set back with `/time set`, a villager starts counting again from the new day, so its next rotation comes after the interval counted from there.
- **Existing worlds.** Villagers that already exist start counting the first time they work at their job site after the mod is installed. Their first rotation comes after the interval from that day.
- **Unemployed villagers, nitwits and wandering traders.** They are not affected. Unemployed villagers and nitwits have no trades, and wandering traders are left as they are.
- **Zombie villagers.** The day of the last rotation and the remembered maps move to the zombie villager and back to the villager when it is cured, like the villager's own trades do.

## Requirements

| | NeoForge | Forge | Fabric |
|---|---|---|---|
| Minecraft | 26.2 | 26.2 | 26.2 |
| Loader | NeoForge 26.2.0.0-beta or newer | Forge 65.0.0 or newer | Fabric Loader 0.18.4 or newer |
| Other | | | Fabric API 0.152.1+26.2 or newer for Minecraft 26.2 |
| Jar | `villagertraderotation-neoforge-26.2-<version>.jar` | `villagertraderotation-forge-26.2-<version>.jar` | `villagertraderotation-fabric-26.2-<version>.jar` |

On Minecraft 26.2, NeoForge and Forge are separate loaders that do not run each other's mods, so each has its own jar. Pick the jar that matches your server's loader.

The Fabric jar bundles [NightConfig](https://github.com/TheElectronWill/night-config) (core and toml, LGPL-3.0) to read the TOML config. NeoForge and Forge already ship it.

### Client and server

Install the mod on the server only, on every loader. It adds no items, blocks, entities, network messages or registries, so the server has nothing a client needs to know about:

- **NeoForge:** vanilla clients and NeoForge clients without the mod can join.
- **Forge:** vanilla clients and Forge clients without the mod can join. The mod is marked as server-only, so a Forge client without it still shows the server as compatible in the server list.
- **Fabric:** vanilla clients and Fabric clients without the mod can join.

Installing it on a client as well does no harm.

In singleplayer, install it in your own game; the built-in server runs it.

## Configuration

### Server: `config/villagertraderotation-server.toml`

The file is created with comments on the first server start, or in singleplayer the first time a world is opened. It is read every time a server or world starts; restart the server or reopen the world after changing it. The file is never sent to clients. The options and their defaults:

```toml
# Rotate villager trades. False stops all rotation; the data saved on villagers is kept.
enabled = true

# Days between two rotations of the same villager, a whole number from 1 to 100.
# A new day starts at sunrise; sleeping through the night also starts a new day.
# 1 means today's trades, new ones tomorrow.
rerollIntervalDays = 1

# Profession IDs whose villagers never rotate, for example
# ["minecraft:librarian", "minecraft:cartographer"]. Professions from other mods work too.
excludedProfessions = []
```

- `enabled`: `true` (default) rotates trades. `false` stops every rotation; villagers keep their current trades and the data the mod saved on them is left in place.
- `rerollIntervalDays`: how many days pass between two rotations of the same villager, a whole number from 1 to 100. With `3`, a villager that rotated on day 10 rotates again on day 13.
- `excludedProfessions`: profession IDs, such as `"minecraft:librarian"` or a profession from another mod. Villagers with one of these professions never rotate.

Your own values and comments are never rewritten. An invalid value falls back to the default of that option with a warning in the log. A profession ID that is not known on the server is kept and reported in the log. A file that cannot be read as TOML is left untouched; the defaults are used and the log says what is wrong.

## Removing the mod

The world opens normally. The mod's data is stored on each villager under its own key, `villagertraderotation:rotation`, and vanilla ignores it. Villagers keep the trades they have at that moment and from then on behave like vanilla again. Explorer maps already offered or bought remain normal maps.

## Compatibility

- **Villager Trade Peek:** optional, no extra setup. When trades rotate, the future trades Villager Trade Peek stored for the villager are cleared, and Villager Trade Peek previews new ones the next time the trading screen opens. Without Villager Trade Peek, this part simply does nothing.
- **Villager Trade Lock:** works as is. Villager Trade Lock only remembers trades at the moment a villager loses its profession, so it remembers the trades of the latest rotation and gives those back when the villager takes the same profession again. They rotate again when the next interval comes.
- **Villager Trade Swap:** works as is. Swapped trades are normal trades, so they are replaced with the rest when the interval comes.
- **Mods and data packs that change the trades:** trades that other mods or data packs add to the villager trade sets are part of the pools, so they show up in rotations too. Every level gets as many trades as its trade set gives, as in vanilla. A profession without trade sets, for example one whose mod creates trades in its own way, is not rotated and keeps its trades.

## Building from source

```
./gradlew build
```

The jars are written to `neoforge/build/libs/`, `forge/build/libs/` and `fabric/build/libs/`.

## License

MIT. See [LICENSE](LICENSE). The Fabric jar includes NightConfig, which is licensed under the LGPL-3.0.
