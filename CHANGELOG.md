# Changelog

格式基于 [Semantic Versioning](https://semver.org/lang/zh-CN/)。版本号规则：

- **主版本号 (MAJOR)**：不兼容的 API 修改、存档格式变更、大规模架构重写
- **次版本号 (MINOR)**：向下兼容的功能新增
- **修订号 (PATCH)**：向下兼容的问题修正、bug 修复

---

## [0.7.2] — 2026-05-29

### 修复
- **前置任务完成后后置任务不实时刷新**：`QuestChainHandler.prerequisitesMet()` 和 `NetworkHandler.refreshQuestScreen()` 的 `visibleHidden` 计算都用 `data.isCompleted()` 判断前置是否完成，但 `isCompleted` 仅在领取奖励时设为 true，而 `QuestCompletedEvent` 在目标达成时就触发——导致事件驱动的可见性更新永远拿不到正确结果。修复：两处改为遍历前置任务所有 objective 的 `data.getProgress()` 与 `max` 比对（新增 `isQuestCompleted` 辅助方法），目标进度全部达标即视为前置已完成
- **领奖后 GUI 不刷新**：`handleClaim` 领奖时调用 `grantRewards()` → `data.markCompleted()`，但无任何事件通知或客户端刷新 → 两个分支末尾各加 `refreshQuestScreen(player)` 全量推送最新状态

---

## [0.7.1] — 2026-05-29

### 修复
- **前置任务未完成后置任务仍显示**：`OpenQuestScreenPacket.QuestInfo` 未携带 `prerequisites` 字段，客户端重建 `Quest` 对象时 `prerequisites` 被硬编码为空列表 → `QuestListPanel.getVisibleQuests()` 前置条件检查永远不生效。修复：`QuestInfo` 新增 `prerequisites` 字段，`rewardText`+`description` 合并为 `TextData` 子记录（NeoForge StreamCodec 最多 6 字段限制）
- **重置前置任务后后置任务不隐藏**：`QuestCommand.resetQuest()` 的 `cascadeReset` 已递归重置后续任务，但客户端缓存未清理 → 新增 `NetworkHandler.refreshQuestScreen()` 全量刷新调用。`QuestVisibilityUpdatePacket` 新增 `removedIds` 字段，客户端收到后清除对应 `visibleHidden` 及 `acceptedQuests`/`declinedQuests`/`progress`

### 变更
- **`hidden` 字段移除，改为前置条件驱动可见性**：`Quest.hidden` 字段与 `prerequisites` 功能重叠 → 删除 `hidden` 字段。无前置条件的任务始终可见，有前置条件的任务在前置完成前自动隐藏。`QuestInfo.flags` 字节 bit 1 含义从 `hidden` 改为保留位。`QuestChainHandler` 可见性扫描移除 `!q.hidden()` 条件。所有 JSON 任务文件移除 `"hidden": true/false` 行
- **全服任务状态标签修正**：`handleOpenScreen` 中全服任务"可接取"不再错误加入 `accepted` 列表

### 移除
- **`NPCMessageDialog` 及 5 个调用点**：Phase 8 GUI 重构后客户端处理器为空操作，服务端仍在发送无意义的 NPC 消息包 → 删除整个 `NPCMessageDispatcher` 类及 `QuestDataManager` 中全部调用
- **`Config` 死字段**：`serverQuestsEnabled`、`serverQuestsGuiVisible` 从未被运行时代码读取 → 移除
- **`IPlayerDataStore`/`IServerDataStore`/`JsonPlayerDataStore`/`JsonServerDataStore` 空方法**：接口及实现中残留的 `getAllPlayerIds` 等无实现方法 → 移除
- **`LocationHandler` 硬编码 tick 间隔**：改用 `Config.INSTANCE.locationCheckIntervalTicks()`

### 优化
- **Handler 循环去重**：`InventoryHandler`、`KillEntityHandler`、`CraftHandler`、`LocationHandler` 各自遍历已接任务的循环逻辑（~100 行重复代码）提取为 `AcceptedObjectiveWalker` 共享遍历器。新增 `TickerGate` 限流工具类
- **Manager 泛型化**：`QuestManager` 和 `NPCManager` 各自的 `Map` + `get`/`getAll`/`exists`/`load` 重复实现合并为泛型 `DefinitionManager<T>`，两个原始类改为类型别名。新增 `JsonDefinitionLoader` 统一 JSON 加载逻辑
- **`QuestDataManager` 保存回调**：`IPlayerDataStore`/`IServerDataStore` 的 `getAllPlayerIds` 空方法清理；`QuestProgressManager` 中全服任务进度检查的 `serverData` 参数类型从 `Object` 强转为正式类型

---

## [0.6.2] — 2026-05-28

### 修复
- **对话描述 `\n` 换行冲突**：服务端和用户 JSON 共用 `\n` 作为分隔符，导致手动换行被误切。改为 `\0`（null char）做段落分隔符

### 移除
- **`consumeOnSubmit` 死字段**：`SubmitItems` 中声明但无运行时代码读取 → 移除
- **`FindItems.checkNbt` 死字段**：从未实现 → 移除
- **`ItemStackSpec.nbt` 死字段**：1.21.1 已改用 Data Components 替代 NBT → 移除

---

## [0.6.1] — 2026-05-28

### 修复
- **任务描述 `\n` 换行无效**：服务端用 `\n` 拼接 4 个 dialogue 段落，客户端用 `\n` 切分——用户 JSON 中的 `\n` 被误认为段落分隔符。改为 `\0` 做分隔符，`wrapLines` 增加 `\n` 切分支持手动换行

---

## [0.6.0] — 2026-05-28

### 重大变更
- **合并 completed 和 claimed 状态**：删除独立的 `completedQuests`（目标达标标记）。目标是否达标改为从 progress 实时计算，`rewardClaimed` 重命名为 `completedQuests` 成为唯一的"已完成"标志。消除两条源不一致的 bug。覆盖 PLAYER 和 SERVER scope 的全部代码路径

### 优化
- **任务描述支持 `\n` 手动换行**：`wrapLines` 增加 `\n` 切分，JSON 中写 `"第一行\n第二行"` 即可手动控制换行位置，同时保留自动换行
- **全服任务达标后仅参与者可见"可领取"**：未接受/未贡献的玩家不再看到领取按钮，避免点了领不了的 UX 问题
- **全服任务完成后保留玩家历史记录**：`acceptedPlayers`/`declinedPlayers`/`claimedPlayers` 不再在完成时清空，`server_data.json` 中可溯源谁参与过
- **server_data.json 增加 `is_completed` 调试字段**：每个全服任务条目增加布尔标记，方便直观查看是否达标过
### 修复
- **handleClaim PLAYER 分支领取后未移出 acceptedQuests**：领取时追加 `acceptedQuests.remove()`
- **updateServerProgress 达标后重复触发 completeServerQuest**：加 `wasAlreadyMet` 守卫
- **StatusLists 重复 completed 字段**：合并为目标达标的 completed 和已领取的 completed 为一个

---

## [0.5.2] — 2026-05-28

### 优化
- **多目标任务的进度框改为每 objective 一行**：有多个 objective 时（如 server_collect_iron 的铁锭 + 钻石），展开卡片内每个目标独立一行——描述文字 + `cur/max` + 微型进度条，不再串在同一行

### 修复
- **进度框高度计算漏了底部留白**：`calcExpandedHeight` 公式少了一层 PAD，导致卡片背景比实际内容短，按钮栏被裁剪

---

## [0.5.1] — 2026-05-28

### 新增
- **登录时自动接受无前置的 auto_accept 任务**：`auto_accept: true` + `prerequisites` 为空的任务在玩家登录时自动派发，不依赖 GUI 打开。PLAYER 和 SERVER scope 均支持，重复登录不会重复接受

### 修复
- **全服任务默认显示"进行中"**：`handleOpenScreen` 错误将"可接取"加入了 accepted 列表，导致 GUI 显示为"进行中"

### 移除
- **旧版 server_data.json 扁平格式兼容代码**：`ServerQuestData` 四个扁平 getter、旧 `addContribution` 签名、QuestDataCodec 旧格式 fallback 解析

---

## [0.5.0] — 2026-05-28

### 重大重构
- **全服任务状态统一存 ServerQuestData**：全服任务不再写入 `PlayerQuestData`。所有 per-player 状态（accepted/declined/claimed/contributors）集中在 `server_data.json` 的 per-quest 结构中。`ServerQuestData` 从 6 个平铺 Map 重构为 `Map<QuestId, ServerQuestEntry>`，JSON 和 Java 内存结构一致，无需转换层
- **hidden 可见性改为服务端状态驱动**：不再依赖 NPC 消息事件（`pendingQuestCards` 机制）。`handleOpenScreen` 服务端计算 visibleHidden 列表下发，QuestChainHandler 增量推送 `QuestVisibilityUpdatePacket`。退役整个 pendingQuestCards 系统

### 新增
- `/quest server reset <quest_id>` — 重置全服任务（清除进度、贡献、状态）
- 客户端初始加载全服进度 — `handleOpenScreen` 为 SERVER quest 发送 `ServerQuestProgressPacket`，新登录玩家正确显示全服进度

### 修复
- **hidden quest 完成后重开 GUI 不显示**：`getVisibleQuests` 过滤条件补齐 `isQuestCompleted` 豁免
- **hidden quest 拒绝后消失**：补齐 `isDeclined` 豁免
- **接 FIND_ITEMS 任务前已有物品不更新进度**：`handleAccept` 接任务后立即扫描背包设置初始进度
- **领取奖励后状态不变**：领取按钮渲染/点击条件加 `!claimed` 检查
- **`complete()` 未从 acceptedQuests 移除**：完成任务时先从 accepted 移除再加到 completed
- **全服任务打开 GUI 默认显示"进行中"**：`handleOpenScreen` 状态合并逻辑修正，"可接取"不加入 accepted 列表

### 移除
- **`pendingQuestCards` 机制**：退役整个事件驱动的 hidden quest 可见性系统
- **旧版 `server_data.json` 扁平格式支持**：模组未发布，不保留兼容代码
- **`ServerQuestData` 兼容 getter**：`serverProgress()`、`completedServerQuests()`、`claimedPlayers()`、`contributors()` 四个旧 getter 及旧 3-arg `addContribution`

---

## [0.4.10] — 2026-05-27

### 移除
- **Story triggers 死代码**：`triggers/` 目录、`TriggerDefinition`、`TriggerAction`、`TriggerManager`、`NPCMessageDispatcher.tryTrigger()` 是一套独立的剧情触发器系统，从未被接入游戏事件循环。连带移除仅为此服务的 `triggeredMessages` 字段及序列化代码
- **`QuestTrigger.ItemObtained` 变体**：仅 diamond_trigger 使用，无对应 Handler

### 修复
- **`/quest reset` 遗漏 declinedQuests 清除**：`clearQuestData` 清 4 缺 1 → 补齐 5 个状态

---

## [0.4.9] — 2026-05-27

### 修复
- **领取奖励后状态不变**：领取按钮渲染和点击条件只检查 `completed` 不检查 `!claimed` → 双处改为 `if (completed && !claimed)`
- **重开 GUI 后 claimed/declined 状态丢失**：`OpenQuestScreenPacket` 缺少 `claimedQuestIds` 和 `declinedQuestIds` 字段 → 新增 `StatusLists` 子 record 打包四个状态列表同步到客户端
- **hidden=true 任务拒绝后消失**：`getVisibleQuests()` 的 hidden 过滤未豁免 `declined` 状态 → 加 `!cache.isDeclined()`
- **complete() 未从 acceptedQuests 移除**：完成任务后同时存在于 accepted 和 completed 两个集合 → `complete()` 先 `remove` 再 `add`
- **`/quest reset` 遗漏 declinedQuests 清除**：`clearQuestData` 清 4 缺 1 → 补齐 5 个状态全部清除

---

## [0.4.8] — 2026-05-27

### 变更
- **激活 `auto_accept` + `prerequisites` 替代 quest_lines 图系统**：`auto_accept` 字段自 Quest 创建时就存在但从未被读取。现在完成一个任务后自动扫描所有以它为前置的任务，`auto_accept: true` 自动接受、`false` 发 NPC 消息让玩家选择。删除 `QuestLine.java`、`QuestLineManager.java`、`quest_lines/` 配置目录及图遍历逻辑（~150 行）
- **`auto_accept` 默认值 `true` → `false`**：之前所有 quest 隐式 auto_accept=true 但不生效；改为默认 false，显式开启才自动接受

---

## [0.4.7] — 2026-05-27

### 修复
- **全服任务领取奖励无效果**：`handleClaim` 对全服任务检查 `PlayerQuestData.isCompleted()` 永远 false → 新增 SERVER scope 分支，改查 `ServerQuestData.isCompleted()` + `hasPlayerClaimed()`，发奖后标记已领取
- **全服任务完成后 [领取奖励] 不显示**：`completeServerQuest` 未通知客户端 → 广播 `QuestCompletedPacket` 给所有在线玩家，修复客户端处理器空实现，同步写入各玩家 `completedQuests` 防止 GUI 重开时状态丢失

### 移除
- **`NPCDialogue` 类**：NPC 的 `dialogue_pool` 配置从未被运行时代码读取，对话全部来自 `Quest.dialogue()` → 删除 `NPCDialogue.java`、NPC 中的 `dialoguePool` 字段、JSON 中的 `dialogue_pool` 块
- **`NPC.questLines` 字段**：声明但无任何代码读取 → 移除
- **`QuestLineManager.get()` 方法**：实现但无调用方 → 移除

---

## [0.4.6] — 2026-05-27

### 修复
- **全服任务进度重启丢失**：全服数据仅在 `ServerStoppingEvent` 时保存，非正常关服（崩溃、kill）进度全部丢失 → `QuestDataManager` 新增 `saveServer()` 方法，`QuestProgressManager.updateServerProgress()` 每次进度变化后即时落盘到 `server_data.json`

---

## [0.4.5] — 2026-05-27

### 变更
- **全服任务只支持 SUBMIT_ITEMS**：SERVER scope + FIND_ITEMS 设计上不成立（玩家退出后物品消失但计数器不能扣），`server_collect_iron` 示例从 find_items 改为 submit_items，文案从"收集"改为"上交"
- **全服任务提交上限修复**：`handleConfirmSubmit` 对 SERVER scope 改从 `ServerQuestData` 读取进度计算提交上限，修复无限提交和进度显示 0/N 的问题

---

## [0.4.4] — 2026-05-27

### 移除
- **`chat_history` 遗留存储**：旧聊天面板（Phase 8 已移除）的聊天历史不再被 GUI 消费，但服务器端仍在每��� NPC 消息时写入并持久化到磁盘，造成无意义的内存和磁盘占用。移除 `PlayerQuestData` 中 `chatHistory` 字段/方法、`QuestDataCodec` 中序列化代码、`NPCMessageDispatcher` 中写入调用，删除 `ChatMessage` 类文件，移除 `Config` 中从未被实际使用的 `chat_history_max_per_npc` 配置项

---

## [0.4.3] — 2026-05-27

### 变更
- 合并"待接受"和"可接取"状态标签，统一显示"可接取"
- 全服任务改为手动接取（`auto_accept: false`，`can_reject: false`），不再登录时自动加入

### 修复
- **重置后重接任务进度残留**：`setAcceptedQuests` 对比新旧列表，不在新列表中的 quest 清 progress → 重置后再接从 0 开始
- **FIND_ITEMS 进度不封顶**：背包 6 把铁剑 → 显示 6/1 → 进度条溢出。`InventoryHandler` 上报前 `Math.min(held, max)` 裁剪 + 进度条 fill `Math.min(cur, max)`
- **进度条溢出卡片**：`barX` 从右边界往左计算，硬限制 `barX ≥ x + PAD + 6`
- **重启后已完成任务显示为"进行中"**：`OpenQuestScreenPacket` 新增 `completedQuestIds` 字段，客户端优先用服务端数据判断是否完成
- **已完成后重启进度条显示 0/N**：服务端已标记完成但本地 progress 缓存为空 → 进度条和文字统一显示 max/N

---

## [0.4.2] — 2026-05-27

### 修复
- **玩家数据仅在下线时保存**：接受/拒绝/完成/领取奖励后未写硬盘，崩溃或中途退出数据丢失 → 所有关键操作点添加 `savePlayer()` 即时持久化
- **物品提交后不扣除、进度不更新**：`handleConfirmSubmit` 缺 `savePlayer()` → 补充
- **经验奖励图标不渲染**：`rewardItems` 解析用 `split(":")` 切分，ResourceLocation 自带冒号导致解析失败（`"minecraft:diamond_sword:1"` 被切成 3 段而不是 2 段）→ 改用 `lastIndexOf(':')`
- **reset 后 GUI 仍显示旧状态**：客户端缓存 `claimedRewards`/`declinedQuests` 未清 → 新增 `clearStatusFlags()` 替代全量 `clear()`，保留 `progress` 防止提交后重开 GUI 进度丢失
- **展开后按钮点击区域偏移**：`mouseClicked` 和 `renderExpanded` 的按钮 Y 计算公式不统一 → 渲染时记下实际 Y 坐标，点击时直接读取
- **任务列表滚动不到底**：`getHeight()` 和 `renderExpanded()` 返回值不一致导致 `totalH` 偏小 → 统一为 `calcExpandedHeight + 2`，`maxScroll` 加 12px 底部留白
- **展开卡片底部浅蓝框被截断**：背景 `fill` 高度比实际内容少 2px → 背景改用 `calcExpandedHeight + 2` 全覆盖
- **"可接取"状态卡片底部余量不统一** → `calcExpandedHeight` 返回 `h + PAD`，和渲染高度一致

### 变更
- 合并"待接受"和"可接取"状态标签，统一显示"可接取"

---

## [0.4.1] — 2026-05-27

### 变更
- collect_iron 奖励改为 3 个物品（绿宝石 x2 + 钻石剑 x1 + 50 经验），用于测试多奖励图标格子排列

### 修复
- **奖励图标与"奖励"标签重叠**：格子起始 Y 从 `cy+10` 改为 `cy+24`（标签下方留 18px 空行），box 高度同步修正
- **描述框/进度框文字未垂直居中**：单行文字 Y 偏移 +2px，公式 `boxTop + (boxH - lineHeight) / 2`
- **展开后按钮点击偏移**：`mouseClicked` 和 `updateHover` 的按钮 Y 计算重写为与 `renderExpanded` 完全一致的 cy 递推公式
- **奖励格子数字压边框**：`cellWidth` 改为 `16 + 4 + font.width("xN") + 8`（左右各多 4px 留白）
- **单物品奖励不显示数量**：改为始终显示 xN（包括 x1）
- **弹窗"关闭"按钮无悬停动效**：`DialoguePopup.render()` 新增 mouseX/Y 参数，悬停时颜色变亮 `#445566 → #6688AA`
- **SubmitItemScreen 确认按钮无悬停动效**：`mouseMoved` 跟踪，悬停 `#226622 → #337733`

---

## [0.4.0] — 2026-05-27

### 新增
- **GUI 视觉重设计**：任务卡片分为独立视觉区域（描述框/进度框/奖励框/按钮栏），各区不同背景色 + 左侧彩色竖线标记
- **文字自动换行**：描述文字按 240px 宽度自动断行，单词边界优先，超出不溢出
- **按钮悬停反馈**：4 种按钮（接受/拒绝/提交/领取）均有悬停高亮变色（~30% 亮度提升），QuestScreen 新增 `mouseMoved` 事件跟踪
- **奖励物品图标渲染**：奖励框改为带边框的图标格子排列，使用 `renderFakeItem` 渲染真实 MC 物品图标 + 数量文字
- **QuestReward icon 字段**：每种奖励类型可独立指定 `"icon"` 渲染图标（ITEM 默认用物品自身 ID，EXPERIENCE→经验瓶，COMMAND→命令方块，FUNCTION→知识之书）
- **奖励格子换行**：图标格子横排，超出卡片宽度自动折行，卡片高度自适应行数
- 新增 `craft_furnace` 任务（CRAFT_ITEM 类型，合成熔炉，奖励煤炭+经验）

### 变更
- 折叠/展开标题颜色统一为金色 `0xFF_FFD700`
- 按钮文字改为"提交物品"、"领取奖励"（更长更清晰）
- 过长进度文字自动截断加 `…`
- 每个 objective 显示微型进度条（60px 内联）
- 奖励框移除纯文字渲染，改为纯图标格子
- 5 个默认任务 JSON 经验奖励全部加 `"icon": "minecraft:experience_bottle"`

### 修复
- **奖励物品图标不渲染**：`gfx.fill()` 和 `gfx.renderFakeItem()` 走不同顶点缓冲，填充后提交覆盖图标 → 物品渲染移到卡片最末尾 + 移除无效 `gfx.flush()`
- **客户端物品数据解析失败**：服务端非物品奖励（经验/命令/函数）未写入 rewardItems → 所有奖励类型统一写入 `iconId:count` 格式，客户端 fallback 解析文本
- **硬编码钻石调试图标残留** → 移除
- **卡片 getHeight 与 renderExpanded 高度不一致**（漏了描述和进度行高）→ 抽取 `calcExpandedHeight()` 统一
- **弹窗 Z 层文字透叠** → `gfx.pose().translate(0, 0, 400)` 推高弹窗
- **鼠标点击区域与展开卡片视觉位置错位** → 同步 btnY 计算

---

## [0.3.2] — 2026-05-26

### 新增
- NPC 对话弹窗文字支持 JSON 自定义（`dialogue.accept`/`decline`/`complete` 通过 `\n` 分隔符发送到客户端）
- 弹窗内长文本自动换行支持
- 折叠视图始终显示极简进度（未接取任务也显示 0/N）

### 修复
- **任务线虚拟节点不跳转**：`QuestChainHandler` 遇到无任务的虚拟节点（如 branch_point）直接返回，后续任务永远触发不到 → 递归遍历虚拟节点的 outgoing edges
- **展开后点击错位**：`getHeight()` 与 `renderExpanded()` 高度计算不一致（漏了描述文字和进度摘要高度）→ 抽取 `calcExpandedHeight()` 统一计算
- **弹窗文字透叠**：`fill()` 与 `drawString()` 的 Z 层级不同，文字盖在弹窗背景之上 → `gfx.pose().translate(0, 0, 400)` 推高弹窗 Z 层
- 展开后按钮 Y 坐标未计入 description/progressLine 高度 → mouseClicked 同步计算
- 展开后卡片 `totalH` 未计入描述和摘要行高 → 统一在 `calcExpandedHeight()` 处理
- 弹窗关闭按钮点击区域与渲染位置不一致

### 变更
- 弹窗设计方案重写：轻量 `0x88` 遮罩 + 纯色背景 + 阴影 + 蓝色装饰条

---

## [0.3.1] — 2026-05-26

### 修复
- 折叠视图不显示极简进度（`progressLine` 去掉了 `isAccepted` 检查，未接任务也显示 0/N）
- 展开后按钮点击无效（`mouseClicked` 的 `btnY` 加上描述文字和进度摘要高度以匹配实际位置）
- `totalH` 计算未包含描述和进度摘要行高，导致按钮区域超出卡片背景
- `QuestReward.ItemStackSpec` Codec 中 `optionalFieldOf("nbt", null)` 导致 `NullPointerException` → 改为 `new CompoundTag()`
- 服务端崩溃：`Surezs_quest` 类的静态字段 `KeyMapping OPEN_QUEST` 导致 DEDICATED_SERVER 加载客户端类 → 移入 `ClientSetup`
- `NetworkHandler` 导入客户端类导致服务端加载 `Screen` → 拆分 `ClientNetworkHandler`
- 卡片展开/收起时"进行中"标识样式不一致 → 统一靠右对齐、同色

---

## [0.3.0] — 2026-05-26

### 新增
- NPC 对话弹窗：接受/拒绝/领取奖励时弹出窗口，显示 NPC 头像+名称+台词
- 展开后显示任务描述文字（`dialogue.give`），再显示极简进度 + 可视化进度条
- `/quest reset <player>` 不加任务 ID 时提示确认，`/quest reset <player> confirm` 清空全部进度
- 左侧 NPC 红点：有待接任务或进行中任务的 NPC 显示未读标记
- `hidden` 任务过滤：`hidden=true` 且未接取的任务不显示，直到被触发或接受

### 变更
- 任务定义从 datapack 移至 `config/surezs_quest/`，`/quest reload` 独立热重载
- 首次运行自动从 JAR 复制默认 JSON 到 config 目录
- 折叠/展开状态标签统一：靠右对齐、颜色一致（进行中=蓝、可领取=金、已拒绝=红、已完成=灰）
- 点击卡片任意位置展开，再次点击非按钮区域折叠
- 卡片宽度自适应面板
- 任务描述文字：FIND_ITEMS 和 SUBMIT_ITEMS 统一为"找到并上交"

### 修复
- 客户端数据由服务端下发，不再依赖 ResourceManager 读取
- `acceptedQuests` 从不可变 Set 改为可变 HashSet（修复 QuestCompletedPacket 处理异常）
- 卡片渲染 Y 坐标重叠问题（多任务卡竖直排列）
- [提交] 和 [领取] 按钮重叠（达标后自动隐藏提交按钮）
- [提交] 按钮点击后自动收起（修复按钮 Y 坐标计算）
- 提交后回到 QuestScreen 而非关闭 GUI
- 领取/拒绝按钮点击后 UI 不更新（乐观更新缓存）
- 聊天记录重复、内存泄漏等旧面板问题（Phase 8 GUI 重构彻底解决）

### 默认任务
- `collect_iron`: SUBMIT_ITEMS, can_reject=true, 奖励绿宝石+经验
- `kill_zombies`: KILL_ENTITY, can_reject=false, 可重复
- `secret_weapon`: FIND_ITEMS, hidden=true, 前置 collect_iron
- `server_collect_iron`: SERVER scope, 全服采集 1000 铁锭

---

## [0.2.0] — 2026-05-26

### 新增
- 右侧面板重构为 QuestListPanel：按 NPC 筛选，可滚动
- 任务卡片折叠/展开交互
- 展开后显示进度条、奖励预览、操作按钮
- 默认显示 NPC 名下全部可见任务（可接取/进行中/可领取/已拒绝/已完成）
- SubmitItemScreen 物品提交界面

### 移除
- ChatAreaPanel / ChatBubbleWidget（聊天面板）
- 右侧聊天历史展示

### 修复
- GUI 背景模糊问题（重写 `renderBackground` 为空方法）
- 服务端崩溃：KeyMapping 等客户端类移到 ClientSetup
- `NetworkHandler` 客户端引用隔离到 `ClientNetworkHandler`
- QuestReward.ItemStackSpec `optionalFieldOf("nbt", null)` → NPE 崩溃
- `ItemCraftedEvent` NeoForge 1.21.1 不存在，改为 PlayerTick 检测

---

## [0.1.0] — 2026-05-26

### 新增
- **数据模型**：QuestObjective（5 种）、QuestReward（4 种）、Quest、NPC、QuestLine 等 sealed interface + Codec
- **存储层**：IPlayerDataStore/IServerDataStore 接口 + JSON 实现，配置文件持久化
- **触发器系统**：InventoryHandler、LocationHandler、KillEntityHandler、CraftHandler
- **进度管理**：QuestProgressManager 统一协调进度更新+完成判定
- **奖励发放**：QuestRewardDispatcher（物品/经验/命令/函数）
- **任务链**：QuestChainHandler 任务线自动解锁
- **网络层**：11 种 C↔S 数据包（CustomPacketPayload + StreamCodec）
- **GUI**：QuestScreen 主界面、NPCSidebarWidget、ChatAreaPanel、QuestCardWidget
- **全服任务**：SERVER scope 共享进度，广播同步
- **管理指令**：`/quest give|complete|reset|list|reload`
- **配置系统**：Gson JSON 配置文件，含全服任务开关
