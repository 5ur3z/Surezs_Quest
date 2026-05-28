# Surez's Quest

Minecraft 1.21.1 NeoForge 任务模组——支持单人/全服任务、NPC 对话、物品提交、击杀/合成/位置检测、任务链自动解锁。

> [English Version](README_EN.md)

## 功能

- **5 种任务目标**：上交物品、找到物品、击杀实体、合成物品、到达位置
- **全服协作任务**：多人共同提交物品，进度共享，贡献者可领取奖励
- **NPC 对话系统**：接受/拒绝/完成/领取奖励时弹出 NPC 对话
- **任务链自动解锁**：完成前置任务后自动解锁后续任务，支持分支选择
- **GUI 任务面板**（默认 K 键）：左侧 NPC 列表，右侧任务卡片，展开查看进度/描述/奖励
- **隐藏任务**：`hidden=true` 的任务在前置完成后才显示
- **多语言支持**：简体中文 / English，切换语言后 GUI 和命令反馈自动跟随

## 快速开始

1. 安装 NeoForge 1.21.1，将 jar 放入 `mods/` 文件夹
2. 启动游戏，首次运行自动在 `config/surezs_quest/` 生成默认配置和 5 个示例任务
3. 按 **K** 打开任务面板，点击 NPC "aleksei" 查看任务

## 配置

所有配置文件在 `config/surezs_quest/`：

```
config/surezs_quest/
├── quests/          ← 任务 JSON 定义
├── npcs/            ← NPC JSON 定义
├── config.json      ← 模组设置
├── avatars/         ← NPC 头像 PNG
├── player_data/     ← 玩家任务进度
└── server_data.json ← 全服任务进度
```

修改 JSON 后执行 `/quest reload` 即刻生效。

### config.json

| 配置项 | 默认值 | 说明 |
|--------|--------|------|
| `server_quests_enabled` | true | 是否启��全服任务 |
| `server_quests_gui_visible` | true | 全服任务是否在 GUI 显示 |
| `auto_save_interval_seconds` | 30 | 自动保存间隔 |
| `location_check_interval_ticks` | 20 | 位置检测间隔 |

## 任务 JSON 格式

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
    "give": "任务描述（支持\\n换行）",
    "accept": "接受时NPC说的话",
    "decline": "拒绝时NPC说的话",
    "in_progress": "进行中时NPC的话",
    "complete": "完成时NPC说的话"
  }
}
```

### Objective 类型

| type | 额外字段 | 说明 |
|------|---------|------|
| `submit_items` | `item`, `count` | 玩家手动提交物品（消耗） |
| `find_items` | `item`, `count` | 自动检测背包中有该物品 |
| `kill_entity` | `entity_type`, `count` | 击杀指定实体 |
| `craft_item` | `item`, `count` | 合成指定物品 |
| `reach_location` | `dimension`, `x`, `y`, `z`, `radius` | 到达指定坐标范围 |

### Scope

| 值 | 说明 |
|----|------|
| `PLAYER` | 单人任务（默认） |
| `SERVER` | 全服共享任务，仅支持 `submit_items` |

### 任务链

通过 `prerequisites` + `auto_accept` 控制：

- `prerequisites`: 前置任务 ID 列表
- `prerequisite_mode`: `ALL`（全部完成）/ `ANY`（任一完成）
- `auto_accept`: `true`（前置完成后自动接取）/ `false`（发 NPC 消息让玩家手动选）

无前置 + `auto_accept: true` 的任务在玩家登录时自动派发。

## 命令

所有命令需 op 权限（等级 2）。

| 命令 | 说明 |
|------|------|
| `/quest give <player> <quest_id>` | 派发任务给玩家 |
| `/quest complete <player> <quest_id>` | 强制完成任务 |
| `/quest reset <player>` | 提示重置确认 |
| `/quest reset <player> confirm` | 清空该玩家全部任务进度 |
| `/quest reset <player> <quest_id>` | 重置单个任务（级联重置后续任务） |
| `/quest list <player>` | 查看玩家任务状态 |
| `/quest reload` | 热重载所有任务配置 |
| `/quest server reset <quest_id>` | 重置全服任务进度 |

## 默认示例任务

| 任务 | 类型 | 说明 |
|------|------|------|
| collect_iron | submit_items | 上交 3 个铁锭，奖励绿宝石+经验 |
| kill_zombies | kill_entity | 击杀 10 只僵尸，奖励铁剑+经验，不可拒绝 |
| craft_furnace | craft_item | 合成 1 个熔炉，奖励煤炭+经验 |
| secret_weapon | find_items | 找到 1 把铁剑，hidden=true，前置 collect_iron |
| server_collect_iron | submit_items (SERVER) | 全服上交 1000 个铁锭 |

## 许可证

GNU GPL 3.0
