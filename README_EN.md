# Surez's Quest

A Minecraft 1.21.1 NeoForge quest mod — player & server-wide quests, NPC dialogue, item submission, kill/craft/location detection, quest chain auto-unlock, and full i18n support.

> [中文版 (Chinese Version)](README.md)

## Features

- **5 Quest Objective Types**: Submit Items, Find Items, Kill Entity, Craft Item, Reach Location
- **Server-wide Cooperative Quests**: Multiple players submit items together, shared progress, contributors claim rewards
- **NPC Dialogue System**: Accept / Decline / Complete / Claim reward popups with NPC interaction
- **Quest Chain Auto-unlock**: Complete prerequisites to automatically unlock follow-up quests, with branch selection support
- **GUI Quest Panel** (default K key): NPC list (left), quest cards (right), expand for progress/description/rewards
- **Hidden Quests**: `hidden=true` quests only appear after prerequisites are met
- **Multi-language**: Simplified Chinese / English — GUI, command feedback, reward labels follow client language setting

## Quick Start

1. Install NeoForge 1.21.1, place the jar in `mods/`
2. Launch the game — default config and 5 sample quests are auto-generated in `config/surezs_quest/` on first run
3. Press **K** to open the quest panel, click NPC "aleksei" to view quests

## Configuration

All config files are in `config/surezs_quest/`:

```
config/surezs_quest/
├── quests/          ← Quest JSON definitions
├── npcs/            ← NPC JSON definitions
├── config.json      ← Mod settings
├── avatars/         ← NPC avatar PNGs
├── player_data/     ← Player quest progress
└── server_data.json ← Server-wide quest progress
```

After editing JSON, run `/quest reload` to apply changes instantly.

### config.json

| Setting | Default | Description |
|---------|---------|-------------|
| `server_quests_enabled` | true | Enable server-wide quests |
| `server_quests_gui_visible` | true | Show server quests in GUI |
| `auto_save_interval_seconds` | 30 | Auto-save interval |
| `location_check_interval_ticks` | 20 | Location detection interval |

## Quest JSON Format

```json
{
  "id": "modid:quest_id",
  "npc_id": "modid:npc_id",
  "scope": "PLAYER",
  "objectives": [
    {
      "type": "submit_items",
      "item": "minecraft:iron_ingot",
      "count": 3
    }
  ],
  "prerequisites": [],
  "prerequisite_mode": "ALL",
  "rewards": [
    { "type": "ITEM", "item": { "id": "minecraft:emerald", "count": 2 } },
    { "type": "EXPERIENCE", "experience": 50, "icon": "minecraft:experience_bottle" },
    { "type": "COMMAND", "command": "say hello" },
    { "type": "FUNCTION", "function": "mymod:reward" }
  ],
  "can_reject": true,
  "hidden": false,
  "repeatable": false,
  "auto_accept": false,
  "dialogue": {
    "give": "Quest description (supports \\n for line breaks)",
    "accept": "NPC response on accept",
    "decline": "NPC response on decline",
    "in_progress": "NPC response during progress",
    "complete": "NPC response on completion"
  }
}
```

### Objective Types

| type | Extra Fields | Description |
|------|-------------|-------------|
| `submit_items` | `item`, `count` | Player manually submits items (consumed) |
| `find_items` | `item`, `count` | Auto-detects items in inventory (not consumed) |
| `kill_entity` | `entity_type`, `count` | Kill specified entity |
| `craft_item` | `item`, `count` | Craft specified item (polled every 20 ticks) |
| `reach_location` | `dimension`, `x`, `y`, `z`, `radius` | Reach a coordinate range |

### Scope

| Value | Description |
|-------|-------------|
| `PLAYER` | Individual quest (default) |
| `SERVER` | Server-wide quest, only supports `submit_items` |

### Quest Chain

Controlled via `prerequisites` + `auto_accept`:

- `prerequisites`: List of prerequisite quest IDs
- `prerequisite_mode`: `ALL` (all must be done) / `ANY` (at least one)
- `auto_accept`: `true` (auto-accept when prerequisites met) / `false` (NPC message, player chooses)

Quests with no prerequisites and `auto_accept: true` are automatically given on player login.

## Commands

All commands require op permission (level 2).

| Command | Description |
|---------|-------------|
| `/quest give <player> <quest_id>` | Give a quest to a player |
| `/quest complete <player> <quest_id>` | Force-complete a quest |
| `/quest reset <player>` | Prompt reset confirmation |
| `/quest reset <player> confirm` | Clear all quest progress for player |
| `/quest reset <player> <quest_id>` | Reset a single quest (cascade resets dependents) |
| `/quest list <player>` | View player quest status |
| `/quest reload` | Hot-reload all quest configs |
| `/quest server reset <quest_id>` | Reset server-wide quest progress |

## Default Sample Quests

| Quest | Type | Description |
|-------|------|-------------|
| collect_iron | submit_items | Submit 3 iron ingots, reward emerald + EXP |
| kill_zombies | kill_entity | Kill 10 zombies, reward iron sword + EXP, cannot reject |
| craft_furnace | craft_item | Craft 1 furnace, reward coal + EXP |
| secret_weapon | find_items | Find 1 iron sword, hidden=true, prerequisite: collect_iron |
| server_collect_iron | submit_items (SERVER) | Server-wide: submit 1000 iron ingots total |

## License

GNU GPL 3.0
