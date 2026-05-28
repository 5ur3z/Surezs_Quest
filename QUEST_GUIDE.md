# Surez's Quest — 默认任务流程指南

## 默认任务（共 5 个 + 1 全服任务）

点击阿列克谢后可见 3 个初始任务 + 1 个隐藏任务（完成前置后解锁）+ 1 个全服任务 = 最多 5 个任务卡片，足以测试 GUI 滚动。

| # | 任务 ID | 类型 | 可见条件 | 测试点 |
|---|---------|------|----------|--------|
| 1 | collect_iron | submit_items | 始终可见 | 手动提交、can_reject、ITEM+EXP 奖励 |
| 2 | kill_zombies | kill_entity | 始终可见 | can_reject=false、repeatable |
| 3 | craft_furnace | craft_item | 始终可见 | 合成检测、轻量奖励 |
| 4 | secret_weapon | find_items | hidden，完成 collect_iron 后解锁 | 前置任务、自动检测 |
| 5 | server_collect_iron | submit_items (SERVER) | 始终可见，手动接取 | 全服上交物品 |

---

### 1. collect_iron（收集铁锭）— SUBMIT_ITEMS

| 步骤 | 操作 | 预期结果 |
|------|------|----------|
| ① | 按 K 打开 GUI，点阿列克谢 | 右侧显示 collect_iron [可接取]、kill_zombies [可接取]、server_collect_iron [可接取] |
| ② | 点 collect_iron 卡片 → 展开 → 点 [接受] | 状态变为 [进行中]，显示"找到并上交 iron_ingot: 0/3" |
| ③ | 去挖矿凑够 3 个铁锭 | 背包里有铁锭，但进度不会自动变——SUBMIT 类型必须手动提交 |
| ④ | 展开任务卡 → 点 [提交物品] | 弹出 SubmitItemScreen，列出背包里的铁锭 |
| ⑤ | 点击铁锭行选中 → 点 [确认提交] | 物品扣除，进度变为 3/3，自动回到 QuestScreen |
| ⑥ | 再次展开任务卡 | 显示 [领取奖励]，点领取 → 获得 2 绿宝石 + 50 经验 |
| ⑦ | 任务线推进 | 阿列克谢的任务线中解锁分支点，可看到 secret_weapon 和 kill_zombies 分支 |

### 2. kill_zombies（杀僵尸）— KILL_ENTITY

| 步骤 | 操作 | 预期结果 |
|------|------|----------|
| ① | 在阿列克谢的列表里展开 kill_zombies → 点 [接受] | 注意：没有 [拒绝] 按钮（`can_reject: false`） |
| ② | 去杀 10 只僵尸 | 每杀一只，后台日志显示 `[Kill] Quest obj matched X/10` |
| ③ | 回到 GUI 展开 | 进度条实时更新，杀满后显示 [领取奖励] |
| ④ | 点领取 | 获得铁剑 + 100 经验。任务可重复接取 |

### 3. craft_furnace（合成熔炉）— CRAFT_ITEM

| 步骤 | 操作 | 预期结果 |
|------|------|----------|
| ① | 在阿列克谢列表展开 craft_furnace → [接受] | 状态变为 [进行中] |
| ② | 用 8 个圆石合成 1 个熔炉 | CraftHandler 检测背包变化，进度自动更新为 1/1 |
| ③ | 展开 → [领取奖励] | 获得 16 煤炭 + 30 经验 |

### 4. secret_weapon（秘密武器）— FIND_ITEMS + 前置任务

| 步骤 | 操作 | 预期结果 |
|------|------|----------|
| ① | 初始状态 | **不显示**在列表中（`hidden: true`，前置 collect_iron 未完成） |
| ② | 完成 collect_iron | 任务链解锁，阿列克谢发消息"既然你弄到了铁，给自己做把铁剑带来" |
| ③ | 再次打开 GUI | secret_weapon 现在可见 [待接受] |
| ④ | 点 [接受] | 进入进行中状态 |
| ⑤ | 合成一把铁剑放在背包 | 自动检测（FIND_ITEMS 类型），进度 1/1 |
| ⑥ | 展开 → 点 [领取奖励] | 获得 3 钻石 + 200 经验 |

### 5. server_collect_iron（全服上交）— SERVER scope

| 步骤 | 操作 | 预期结果 |
|------|------|----------|
| ① | 打开 GUI 展开 server_collect_iron → 点 [接受] | 状态变为 [进行中]（无拒绝按钮，`can_reject: false`） |
| ② | 挖矿获得铁锭 → 展开任务卡 → 点 [提交物品] | 弹出 SubmitItemScreen，列出背包里的铁锭 |
| ③ | 选中铁锭 → 点 [确认提交] | 物品被消耗，全服进度叠加，广播给所有在线玩家 |
| ④ | 全服累计上交 1000 个 | 任务达标，每人都可领 5 钻石 + 500 经验 |

---

## 快速测试命令

```
/quest give <你的ID> surezs_quest:collect_iron       # 直接接收集铁锭
/quest give <你的ID> surezs_quest:kill_zombies       # 直接接杀僵尸
/quest complete <你的ID> surezs_quest:collect_iron    # 跳过收集直接完成
/quest reset <你的ID>                                 # 全部清空重新来
/quest list <你的ID>                                  # 查看所有任务状态
/quest reload                                         # 热重载所有任务定义
```

---

## 任务 JSON 配置位置

```
config/surezs_quest/
├── quests/
│   ├── collect_iron.json
│   ├── kill_zombies.json
│   ├── secret_weapon.json
│   └── server_collect_iron.json
├── npcs/
│   └── aleksei.json
├── config.json
├── avatars/
└── player_data/
```

修改 JSON 后 `/quest reload` 即刻生效。

### 任务链机制

通过 `prerequisites` + `auto_accept` 两个字段控制任务间流转，无需额外的 quest_lines 配置：

- `prerequisites: ["surezs_quest:collect_iron"]` — 前置任务
- `auto_accept: true` — 前置完成后自动接受（强制派发）
- `auto_accept: false` — 前置完成后发 NPC 消息，玩家手动选择是否接受

例如 secret_weapon 配置了 `prerequisites: [collect_iron]` + `auto_accept: false`，完成 collect_iron 后阿列克谢会发消息通知玩家有新任务可接。
