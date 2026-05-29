package org.surez.surezs_quest.screen;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;
import org.surez.surezs_quest.api.quest.Quest;
import org.surez.surezs_quest.api.quest.QuestObjective;
import org.surez.surezs_quest.api.quest.QuestObjectiveUtils;
import org.surez.surezs_quest.network.packet.*;

import java.util.List;
import java.util.Map;

public class QuestCardWidget {

    private static final int BTN_W = 56;
    private static final int BTN_H = 16;
    private static final int PAD = 6;

    // Colors
    private static final int BG_COLLAPSED   = 0xFF_252535;
    private static final int BG_EXPANDED    = 0xFF_2A2A3C;
    private static final int BG_DESC_BOX    = 0xFF_1E1E2E;
    private static final int BG_PROGRESS_BOX = 0xFF_1E1E2E;
    private static final int BG_REWARD_BOX  = 0xFF_222233;
    private static final int BG_BUTTON_BAR  = 0xFF_1E1E2A;
    private static final int BORDER_COLOR   = 0xFF_3A3A50;
    private static final int ACCENT_COLOR   = 0xFF_5577AA;

    private static final int BTN_ACCEPT     = 0xFF_2A6E2A;
    private static final int BTN_ACCEPT_HOV = 0xFF_3A8E3A;
    private static final int BTN_DECLINE    = 0xFF_6E2A2A;
    private static final int BTN_DECLINE_HOV = 0xFF_8E3A3A;
    private static final int BTN_SUBMIT     = 0xFF_3A3A6E;
    private static final int BTN_SUBMIT_HOV = 0xFF_4A4A8E;
    private static final int BTN_CLAIM      = 0xFF_6E5A2A;
    private static final int BTN_CLAIM_HOV  = 0xFF_8E7A3A;

    public static ResourceLocation expandedQuestId;
    /** Button hover state: questId -> which button is hovered (0=none, 1=accept, 2=decline, 3=submit, 4=claim) */
    public static ResourceLocation hoveredQuestId;
    public static int hoveredButton;
    /** Last rendered expanded card's button Y positions, keyed by questId */
    private static final Map<ResourceLocation, Integer> renderedBtnY = new java.util.HashMap<>();
    private static final Map<ResourceLocation, Integer> renderedCardX = new java.util.HashMap<>();
    private static final Map<ResourceLocation, Integer> renderedCardW = new java.util.HashMap<>();

    public static int objectiveMax(QuestObjective obj) {
        return QuestObjectiveUtils.maxProgress(obj);
    }

    public static int getHeight(ResourceLocation questId) {
        Quest quest = ClientQuestData.get(questId);
        if (quest == null) return 30;
        if (questId.equals(expandedQuestId)) return calcExpandedHeight(quest) + 2;
        return 30;
    }

    private static int calcExpandedHeight(Quest quest) {
        var cache = ClientQuestDataCache.INSTANCE;
        boolean accepted = cache.isAccepted(quest.id());
        boolean completed = cache.areObjectivesMet(quest.id());
        boolean rewardDone = cache.isCompleted(quest.id());
        boolean declined = cache.isDeclined(quest.id());

        String desc = ClientQuestData.getDescription(quest.id());
        int descLines = desc.isEmpty() ? 0 : wrapLines(desc, 240).size();

        int h = 32; // title bar
        if (descLines > 0) h += PAD + 14 * descLines + PAD * 2; // desc box
        h += PAD + 18 * quest.objectives().size() + PAD * 2; // progress box (per-objective rows)
        var items = ClientQuestData.getRewardItems(quest.id());
        if (!items.isEmpty()) {
            int rows = calcRewardRows(quest, 240);
            h += PAD + 10 + 8 + rows * 24 + PAD; // reward box (label + gap + cells)
        }
        if (!rewardDone && !declined) {
            if ((!accepted && !completed) || (accepted && !completed &&
                quest.objectives().stream().anyMatch(o -> o instanceof QuestObjective.SubmitItems)) || completed)
                h += BTN_H + PAD * 2; // button bar
        }
        return h + PAD;
    }

    public static int render(GuiGraphics gfx, Font font, int x, int y, int cardWidth, ResourceLocation questId) {
        Quest quest = ClientQuestData.get(questId);
        if (quest == null) return 0;
        if (questId.equals(expandedQuestId))
            return renderExpanded(gfx, font, x, y, cardWidth, quest);
        else
            return renderCollapsed(gfx, font, x, y, cardWidth, quest);
    }

    public static int render(GuiGraphics gfx, Font font, int x, int y, ResourceLocation questId) {
        return render(gfx, font, x, y, 260, questId);
    }

    // ── collapsed ──────────────────────────────────────────────────────────

    private static int renderCollapsed(GuiGraphics gfx, Font font, int x, int y, int w, Quest quest) {
        var cache = ClientQuestDataCache.INSTANCE;
        boolean accepted = cache.isAccepted(quest.id());
        boolean completed = cache.areObjectivesMet(quest.id());
        boolean rewardDone = cache.isCompleted(quest.id());
        boolean declined = cache.isDeclined(quest.id());

        int h = 30;
        gfx.fill(x, y, x + w, y + h, BG_COLLAPSED);
        gfx.fill(x, y, x + w, y + 1, ACCENT_COLOR);

        gfx.drawString(font, quest.id().getPath(), x + PAD, y + 6, 0xFF_DDDDDD);

        String status = rewardDone ? Component.translatable("surezs_quest.status.completed").getString() : declined ? Component.translatable("surezs_quest.status.declined").getString() : completed ? Component.translatable("surezs_quest.status.claimable").getString() : accepted ? Component.translatable("surezs_quest.status.in_progress").getString() : Component.translatable("surezs_quest.status.available").getString();
        if (!status.isEmpty()) {
            int sc = rewardDone ? 0xFF_888888 : declined ? 0xFF_CC4444 : completed ? 0xFF_FFD700 : accepted ? 0xFF_88AAFF : 0xFF_FF8844;
            gfx.drawString(font, status, x + w - font.width(status) - PAD - 4, y + 6, sc);
        }

        String prog = progressLine(quest);
        if (!prog.isEmpty())
            gfx.drawString(font, clipText(font, prog, w - PAD * 2), x + PAD, y + 18, 0xFF_999999);

        return h + 2;
    }

    // ── expanded ───────────────────────────────────────────────────────────

    private static int renderExpanded(GuiGraphics gfx, Font font, int x, int y, int w, Quest quest) {
        var cache = ClientQuestDataCache.INSTANCE;
        boolean accepted = cache.isAccepted(quest.id());
        boolean completed = cache.areObjectivesMet(quest.id());
        boolean rewardDone = cache.isCompleted(quest.id());
        boolean declined = cache.isDeclined(quest.id());

        int totalH = calcExpandedHeight(quest) + 2;
        int innerW = w - PAD * 2;
        int cy = y;

        // ── card background ─────────────────────────────────────────────
        gfx.fill(x, y, x + w, y + totalH, BG_EXPANDED);
        gfx.fill(x, y, x + w, y + 1, ACCENT_COLOR);

        // ── title bar ───────────────────────────────────────────────────
        gfx.drawString(font, quest.id().getPath(), x + PAD, cy + 6, 0xFF_FFD700);
        String status = rewardDone ? Component.translatable("surezs_quest.status.completed").getString() : declined ? Component.translatable("surezs_quest.status.declined").getString() : completed ? Component.translatable("surezs_quest.status.claimable").getString() : accepted ? Component.translatable("surezs_quest.status.in_progress").getString() : Component.translatable("surezs_quest.status.available").getString();
        if (!status.isEmpty()) {
            int sc = rewardDone ? 0xFF_888888 : declined ? 0xFF_CC4444 : completed ? 0xFF_FFD700 : accepted ? 0xFF_88AAFF : 0xFF_FF8844;
            gfx.drawString(font, status, x + w - font.width(status) - PAD - 4, cy + 6, sc);
        }
        cy += 28;
        gfx.fill(x + PAD, cy, x + w - PAD, cy + 1, BORDER_COLOR);
        cy += 4;

        // ── description box ─────────────────────────────────────────────
        String desc = ClientQuestData.getDescription(quest.id());
        if (!desc.isEmpty()) {
            List<String> lines = wrapLines(desc, innerW - 4);
            int boxH = PAD + 14 * lines.size() + PAD;
            gfx.fill(x + PAD, cy, x + w - PAD, cy + boxH, BG_DESC_BOX);
            gfx.fill(x + PAD, cy, x + PAD + 1, cy + boxH, ACCENT_COLOR); // left accent
            int descTextY = cy + PAD + 2; // vertical center offset
            for (int i = 0; i < lines.size(); i++)
                gfx.drawString(font, lines.get(i), x + PAD + 6, descTextY + 14 * i, 0xFF_BBBBBB);
            cy += boxH + PAD;
        }

        // ── progress box ────────────────────────────────────────────────
        int ROW_H = 18;
        int progH = PAD + ROW_H * quest.objectives().size() + PAD;
        gfx.fill(x + PAD, cy, x + w - PAD, cy + progH, BG_PROGRESS_BOX);
        gfx.fill(x + PAD, cy, x + PAD + 1, cy + progH, 0xFF_448844); // green accent

        for (int i = 0; i < quest.objectives().size(); i++) {
            int rowY = cy + PAD + i * ROW_H;
            int max = objectiveMax(quest.objectives().get(i));
            int cur = cache.areObjectivesMet(quest.id()) ? max : cache.getProgress(quest.id(), i);

            String objName = shortName(quest.objectives().get(i));
            String text = clipText(font, objName + " " + cur + "/" + max, innerW - 8 - 64);
            gfx.drawString(font, text, x + PAD + 6, rowY + 2, 0xFF_CCCCCC);

            // mini progress bar
            int barW = 60;
            int barX = x + w - PAD - 4 - barW;
            gfx.fill(barX, rowY + 2, barX + barW, rowY + 12, 0xFF_333333);
            if (cur > 0 && max > 0)
                gfx.fill(barX, rowY + 2, barX + barW * Math.min(cur, max) / max, rowY + 12, 0xFF_448844);
        }
        cy += progH + PAD;

        // ── reward box ──────────────────────────────────────────────────
        var rewardItemList = ClientQuestData.getRewardItems(quest.id());
        if (!rewardItemList.isEmpty()) {
            // calculate layout
            int availW = w - PAD * 2 - 12;
            int rowH = 24;
            int rowY = cy + PAD + 18; // below Component.translatable("surezs_quest.label.rewards").getString() label
            int cellX = x + PAD + 6;
            int rows = 1;
            for (var ri : rewardItemList) {
                int cw = cellWidth(font, ri.count());
                if (cellX + cw > x + w - PAD) { cellX = x + PAD + 6; rowY += rowH; rows++; }
                cellX += cw + 4;
            }

            int boxH = PAD + 10 + 8 + rows * rowH + PAD; // label + gap + cells
            gfx.fill(x + PAD, cy, x + w - PAD, cy + boxH, BG_REWARD_BOX);
            gfx.fill(x + PAD, cy, x + PAD + 1, cy + boxH, 0xFF_886622);
            gfx.drawString(font, Component.translatable("surezs_quest.label.rewards").getString(), x + PAD + 6, cy + PAD + 2, 0xFF_FFD700);

            cellX = x + PAD + 6;
            rowY = cy + PAD + 18;
            for (var ri : rewardItemList) {
                var item = BuiltInRegistries.ITEM.get(ri.itemId());
                if (item == null) {
                    continue;
                }
                int cw = cellWidth(font, ri.count());
                if (cellX + cw > x + w - PAD) { cellX = x + PAD + 6; rowY += rowH; }
                ItemStack stack = new ItemStack(item, ri.count());
                // cell frame
                gfx.fill(cellX, rowY, cellX + cw, rowY + 20, 0xFF_1A1A28);
                gfx.fill(cellX, rowY, cellX + cw, rowY + 1, 0xFF_555555);
                gfx.fill(cellX, rowY, cellX + 1, rowY + 20, 0xFF_555555);
                gfx.fill(cellX, rowY + 19, cellX + cw, rowY + 20, 0xFF_555555);
                gfx.fill(cellX + cw - 1, rowY, cellX + cw, rowY + 20, 0xFF_555555);
                // icon
                gfx.renderFakeItem(stack, cellX + 3, rowY + 2);
                // count text (always show, including x1)
                String cnt = "x" + ri.count();
                gfx.drawString(font, cnt, cellX + 21, rowY + 5, 0xFF_CCCCCC);
                cellX += cw + 4;
            }
            cy += boxH + PAD;
        }

        // ── button bar ──────────────────────────────────────────────────
        if (!rewardDone && !declined) {
            boolean hasBtn = false;
            if (!accepted && !completed) hasBtn = true;
            if (accepted && !completed && quest.objectives().stream().anyMatch(o -> o instanceof QuestObjective.SubmitItems)) hasBtn = true;
            if (completed) hasBtn = true;

            if (hasBtn) {
                int barH = BTN_H + PAD * 2;
                gfx.fill(x + PAD, cy, x + w - PAD, cy + barH, BG_BUTTON_BAR);
                int btnX = x + w - PAD - PAD;
                int btnY = cy + PAD;
                renderedBtnY.put(quest.id(), btnY);
                renderedCardX.put(quest.id(), x);
                renderedCardW.put(quest.id(), w);

                boolean isHover = quest.id().equals(hoveredQuestId);

                if (!accepted && !completed) {
                    int acceptHov = isHover && hoveredButton == 1 ? BTN_ACCEPT_HOV : BTN_ACCEPT;
                    int declineHov = isHover && hoveredButton == 2 ? BTN_DECLINE_HOV : BTN_DECLINE;
                    btnX -= BTN_W; gfx.fill(btnX, btnY, btnX + BTN_W, btnY + BTN_H, acceptHov);
                    gfx.drawCenteredString(font, Component.translatable("surezs_quest.button.accept").getString(), btnX + BTN_W / 2, btnY + 3, 0xFFFFFF);
                    if (quest.canReject()) {
                        btnX -= BTN_W + 4; gfx.fill(btnX, btnY, btnX + BTN_W, btnY + BTN_H, declineHov);
                        gfx.drawCenteredString(font, Component.translatable("surezs_quest.button.decline").getString(), btnX + BTN_W / 2, btnY + 3, 0xFFFFFF);
                    }
                }
                if (accepted && !completed && quest.objectives().stream().anyMatch(o -> o instanceof QuestObjective.SubmitItems)) {
                    int submitHov = isHover && hoveredButton == 3 ? BTN_SUBMIT_HOV : BTN_SUBMIT;
                    btnX -= BTN_W; gfx.fill(btnX, btnY, btnX + BTN_W, btnY + BTN_H, submitHov);
                    gfx.drawCenteredString(font, Component.translatable("surezs_quest.button.submit").getString(), btnX + BTN_W / 2, btnY + 3, 0xFFFFFF);
                }
                if (completed && !rewardDone) {
                    int claimHov = isHover && hoveredButton == 4 ? BTN_CLAIM_HOV : BTN_CLAIM;
                    btnX -= BTN_W; gfx.fill(btnX, btnY, btnX + BTN_W, btnY + BTN_H, claimHov);
                    gfx.drawCenteredString(font, Component.translatable("surezs_quest.button.claim").getString(), btnX + BTN_W / 2, btnY + 3, 0xFFFFFF);
                }
            }
        }

        return totalH;
    }

    private static int cellWidth(Font font, int count) {
        // 16px icon + 4px gap + 4px padding + text
        return 16 + 4 + font.width("x" + count) + 8;
    }

    private static int calcRewardRows(Quest quest, int availWidth) {
        var font = net.minecraft.client.Minecraft.getInstance().font;
        var items = ClientQuestData.getRewardItems(quest.id());
        int x = 0, rows = 1;
        for (var ri : items) {
            int cw = cellWidth(font, ri.count()) + 4;
            if (x + cw > availWidth) { x = 0; rows++; }
            x += cw;
        }
        return rows;
    }

    // ── click ──────────────────────────────────────────────────────────────

    public static boolean mouseClicked(double mx, double my, int cardX, int cardY, int cardWidth, ResourceLocation questId) {
        Quest quest = ClientQuestData.get(questId);
        if (quest == null) return false;

        var cache = ClientQuestDataCache.INSTANCE;
        boolean accepted = cache.isAccepted(quest.id());
        boolean completed = cache.areObjectivesMet(quest.id());
        boolean rewardDone = cache.isCompleted(quest.id());
        boolean declined = cache.isDeclined(quest.id());

        boolean expanded = questId.equals(expandedQuestId);

        if (!expanded) { expandedQuestId = questId; return true; }

        if (rewardDone || declined) { expandedQuestId = null; return true; }

        // use the actual rendered button positions (stored during last render)
        int btnY = renderedBtnY.getOrDefault(questId, cardY + 120);
        int cardX2 = renderedCardX.getOrDefault(questId, cardX);
        int cardW2 = renderedCardW.getOrDefault(questId, cardWidth);
        int btnX = cardX2 + cardW2 - PAD - PAD;

        if (!accepted && !completed) {
            btnX -= BTN_W;
            if (hit(mx, my, btnX, btnY, BTN_W, BTN_H)) {
                PacketDistributor.sendToServer(new AcceptQuestPacket(questId));
                cache.addAccepted(questId);
                DialoguePopup.show(quest.npcId(), ClientQuestData.getAcceptText(questId));
                return true;
            }
            if (quest.canReject()) { btnX -= BTN_W + 4;
                if (hit(mx, my, btnX, btnY, BTN_W, BTN_H)) {
                    PacketDistributor.sendToServer(new DeclineQuestPacket(questId));
                    cache.markDeclined(questId);
                    DialoguePopup.show(quest.npcId(), ClientQuestData.getDeclineText(questId));
                    return true;
                }
            }
        }
        if (accepted && !completed && quest.objectives().stream().anyMatch(o -> o instanceof QuestObjective.SubmitItems)) {
            btnX -= BTN_W;
            if (hit(mx, my, btnX, btnY, BTN_W, BTN_H)) {
                PacketDistributor.sendToServer(new RequestSubmitItemsPacket(questId)); return true;
            }
        }
        if (completed && !rewardDone) {
            btnX -= BTN_W;
            if (hit(mx, my, btnX, btnY, BTN_W, BTN_H)) {
                PacketDistributor.sendToServer(new ClaimRewardPacket(questId));
                cache.markCompleted(questId);
                DialoguePopup.show(quest.npcId(), ClientQuestData.getCompleteText(questId));
                return true;
            }
        }

        expandedQuestId = null; return true;
    }

    /** Called from QuestScreen.mouseMoved to track hover */
    public static void updateHover(double mx, double my, int cardX, int cardY, int cardWidth, ResourceLocation questId) {
        if (!questId.equals(expandedQuestId)) { hoveredQuestId = null; hoveredButton = 0; return; }
        Quest quest = ClientQuestData.get(questId);
        if (quest == null) return;
        var cache = ClientQuestDataCache.INSTANCE;
        boolean accepted = cache.isAccepted(quest.id());
        boolean completed = cache.areObjectivesMet(quest.id());
        boolean rewardDone = cache.isCompleted(quest.id());
        boolean declined = cache.isDeclined(quest.id());
        if (rewardDone || declined) { hoveredQuestId = null; hoveredButton = 0; return; }

        int btnY = renderedBtnY.getOrDefault(questId, cardY + 120);
        int cardX2 = renderedCardX.getOrDefault(questId, cardX);
        int cardW2 = renderedCardW.getOrDefault(questId, cardWidth);
        int btnX = cardX2 + cardW2 - PAD - PAD;

        hoveredQuestId = questId;
        hoveredButton = 0;

        if (!accepted && !completed) {
            btnX -= BTN_W;
            if (hit(mx, my, btnX, btnY, BTN_W, BTN_H)) { hoveredButton = 1; return; }
            if (quest.canReject()) { btnX -= BTN_W + 4;
                if (hit(mx, my, btnX, btnY, BTN_W, BTN_H)) { hoveredButton = 2; return; }
            }
        }
        if (accepted && !completed && quest.objectives().stream().anyMatch(o -> o instanceof QuestObjective.SubmitItems)) {
            btnX -= BTN_W;
            if (hit(mx, my, btnX, btnY, BTN_W, BTN_H)) { hoveredButton = 3; return; }
        }
        if (completed) {
            btnX -= BTN_W;
            if (hit(mx, my, btnX, btnY, BTN_W, BTN_H)) { hoveredButton = 4; return; }
        }
    }

    // ── helpers ─────────────────────────────────────────────────────────────

    private static boolean hit(double mx, double my, int bx, int by, int bw, int bh) {
        return mx >= bx && mx <= bx + bw && my >= by && my <= by + bh;
    }

    private static String progressLine(Quest quest) {
        var cache = ClientQuestDataCache.INSTANCE;
        boolean completed = cache.areObjectivesMet(quest.id());
        var sb = new StringBuilder();
        for (int i = 0; i < quest.objectives().size(); i++) {
            if (i > 0) sb.append("  ");
            int max = objectiveMax(quest.objectives().get(i));
            int cur = completed ? max : cache.getProgress(quest.id(), i);
            sb.append(shortName(quest.objectives().get(i)))
              .append(" ").append(cur)
              .append("/").append(max);
        }
        return sb.toString();
    }

    private static String shortName(QuestObjective obj) {
        return switch (obj) {
            case QuestObjective.FindItems f -> f.item().getPath();
            case QuestObjective.SubmitItems s -> s.item().getPath() + Component.translatable("surezs_quest.objective.submit_suffix").getString();
            case QuestObjective.KillEntity k -> k.entityType().getPath();
            case QuestObjective.CraftItem c -> c.item().getPath();
            case QuestObjective.ReachLocation r -> Component.translatable("surezs_quest.objective.reach_location").getString();
        };
    }

    /** Wrap text to fit maxWidth pixels. Splits on word boundaries when possible. */
    private static List<String> wrapLines(String text, int maxWidth) {
        var font = net.minecraft.client.Minecraft.getInstance().font;
        var lines = new java.util.ArrayList<String>();
        for (String paragraph : text.split("\n")) {
            if (paragraph.isEmpty()) { lines.add(""); continue; }
            var current = new StringBuilder();
            for (String word : paragraph.split(" ")) {
                String test = current.isEmpty() ? word : current + " " + word;
                if (font.width(test) > maxWidth && !current.isEmpty()) {
                    lines.add(current.toString());
                    current = new StringBuilder(word);
                } else {
                    if (!current.isEmpty()) current.append(' ');
                    current.append(word);
                }
            }
            if (!current.isEmpty()) lines.add(current.toString());
        }
        return lines.isEmpty() ? List.of(text) : lines;
    }

    /** Clip text with "…" if too wide */
    private static String clipText(Font font, String text, int maxWidth) {
        if (font.width(text) <= maxWidth) return text;
        String ellipsis = "…";
        int w = font.width(ellipsis);
        for (int i = text.length() - 1; i > 0; i--) {
            if (font.width(text.substring(0, i)) + w <= maxWidth)
                return text.substring(0, i) + ellipsis;
        }
        return ellipsis;
    }
}
