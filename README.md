# Phat's Progression Framework

[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![Minecraft: 1.21.11](https://img.shields.io/badge/Minecraft-1.21.11-green.svg)](https://www.minecraft.net/)

A modpack-configurable mining tier progression framework for Minecraft. Define which tools can mine which blocks entirely through JSON configs. Extend the vanilla tier system past netherite. Reassign existing tools and blocks to fit any progression scheme.

## What it does

Out of the box, this mod replicates vanilla mining tiers. The interesting part is what modpack developers can do with the config:

- **Reassign vanilla tools to new tiers** — make iron the new diamond, or insert copper between stone and iron.
- **Add tiers above netherite** — gate Mythic Metals' runite or Adamantite behind tools you specify.
- **Keep mod tools coherent** — tier progression respects every mod that adds tools, not just vanilla.
- **Override individual blocks and tools** — gate iron ore behind copper, gate diamond ore behind something else, your choice.

Mining a block with the wrong-tier tool is now a real consequence: blocks don't drop, mining speed drops to bare-hands speed, and the tool type still matters (iron axe still won't mine iron ore — only pickaxes do).

## Quick start

1. Install [Architectury API](https://modrinth.com/mod/architectury-api) for your loader.
2. Drop the appropriate jar (Fabric or NeoForge) into your `mods` folder.
3. Launch the game. Default config files are auto-generated at `config/phats_progression/`.

By default, the mod ships with vanilla-faithful progression — installing it alone changes nothing about your gameplay. The power comes from configuring it.

## Configuration

Two config files live at `config/phats_progression/`:

- `block_tiers.json` — assigns blocks to tiers
- `tool_tiers.json` — assigns tools to tiers

### Schema

```json
{
  "ignore_datapacks": false,
  "tiers": {
    "3": [
      "minecraft:iron_pickaxe",
      "*:copper_pickaxe",
      "#c:tools/pickaxes",
      {
        "keyword": "bronze",
        "tag": "c:tools/pickaxes"
      }
    ],
    "6": [
      "mythicmetals:runite_ore"
    ]
  }
}
```

Each tier bucket accepts entries in three styles:

- **Plain ID strings** — exact match on the item/block ID. Supports glob wildcards: `*` (any sequence) and `?` (one character). Example: `*:copper_pickaxe` matches every mod's copper pickaxe.
- **Tag strings** — prefix with `#`. Example: `#c:tools/pickaxes` matches anything in that tag.
- **Object form** — combines `id`, `tag`, and `keyword` fields with AND semantics. The `keyword` field matches a substring of the item path. Useful for matching across mods: `{"keyword": "bronze", "tag": "c:tools/pickaxes"}` catches every modded bronze pickaxe regardless of mod-specific naming.

### Tiers

Tiers are integers 0-10:

- 0 = hand (constant, never reassign)
- 1-5 = vanilla progression (wood, stone, iron, diamond, netherite)
- 6-10 = modpack-extensible

Tier 6+ has no vanilla content gated to it — it's reserved for modpack devs to define.

### Resolution rules

- When multiple entries match an item or block, the highest tier wins.
- If a block has no configured tier, vanilla behavior applies.
- If a tool has no configured tier, vanilla mining behavior is inferred (1.21.1 only — see "Limitations" below).

### `ignore_datapacks` flag

Set this to `true` in either config to make that file ignore datapack-shipped tier definitions and use only the config file's contents. This gives modpack devs full override control over mod-shipped defaults.

## For modpack developers

The config files are your primary surface. They're hot-reloadable: edit them, then run `/reload` in-game (or `/progression reload` for config-only).

Helpful in-game commands:

- `/progression info` — show the tier of the item you're holding
- `/progression info <item_id>` — show the tier of any item by ID
- `/progression block` — show the tier of the block you're looking at
- `/progression block <block_id>` — show the tier of any block by ID
- `/progression list tools` — dump all tool tier assignments to a file
- `/progression list blocks` — dump all block tier assignments to a file
- `/progression reload` — reload config files

## For mod developers

Mods can ship tier definitions via datapack at:data/<your_mod_id>/phats_progression/block_tiers/<file>.json

- data/<your_mod_id>/phats_progression/tool_tiers/<file>.json
- data/<your_mod_id>/phats_progression/tool_tiers/<file>.json

The schema is identical to the config files. Modpack devs can use `ignore_datapacks: true` in their configs to override your defaults entirely, or accept them by leaving it `false`.

## Compatibility

- **Jade**: works without integration code — block tooltips show effective mining requirements correctly.
- **JEI/REI**: tier info is not currently shown in recipe viewers. Planned for a future release.

## Limitations

- **No vanilla tool fallback**: tools without explicit config entries return null (no opinion). Minecraft 1.21.5+ removed the `Tier`/`Tiers` API in favor of data components, so we can't infer tier from ToolMaterial anymore. Configure modded tools explicitly via IDs, tags, or keywords. Default config covers vanilla tools and common metal patterns; modpack devs add the rest.
- **Server-required**: requires the mod on both client and server. Vanilla clients cannot connect to servers running this mod. A server-only mode is planned for a future release.
- **Mining speed only at 1.0**: when a tool is wrong-tier, mining speed drops to bare-hands speed (1.0). The mod doesn't currently scale speed gradually based on tier difference.

## Building

Requires JDK 21.

```bash
./gradlew build
```

Built jars appear in `fabric/build/libs/` and `neoforge/build/libs/`.

To run the dev client:

```bash
./gradlew fabric:runClient
./gradlew neoforge:runClient
```

## Acknowledgments

- [Almost Unified](https://modrinth.com/mod/almost-unified) for the inspiration on config-driven mod compatibility.
- [Architectury API](https://github.com/architectury) for the multiloader framework that makes this mod possible across Fabric and NeoForge.

## License

MIT. See [LICENSE](LICENSE).

## Contributing

Issues and pull requests welcome on [GitHub](https://github.com/Phatdog731/phats_progression).