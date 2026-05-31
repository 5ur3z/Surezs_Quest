const $ = (s) => document.querySelector(s);

let currentQuestId = null;
let currentQuestLine = 'default';
let currentTab = 'quests';
let allQuests = [];
let redrawFn = null;
const viewState = Mindmap.createViewState();

const DEFAULT_QUEST_TEMPLATE = {
  id: '',
  npc_id: '',
  scope: 'PLAYER',
  reward_mode: 'PER_CONTRIBUTOR',
  prerequisite_mode: 'ALL',
  can_reject: false,
  repeatable: false,
  auto_accept: false,
  time_limit_ticks: 0,
  prerequisites: [],
  objectives: [{ type: 'submit_items', item: 'minecraft:', count: 1 }],
  rewards: [{ type: 'ITEM', item: { id: 'minecraft:', count: 1 } }],
  dialogue: { give: '', accept: '', decline: '', in_progress: '', complete: '' }
};

let isFormDirty = false;
function markFormClean() { isFormDirty = false; }

// ── Callbacks for mindmap interactions ──────────────────────────────────
const mindmapCallbacks = {
  onNodeClick(node) {
    openQuestEditor(node.id);
  },
  getContextMenuItems(node) {
    const items = [{ label: 'Edit', action: (n) => openQuestEditor(n.id) }];
    if (currentQuestLine !== 'default') {
      items.push({
        label: 'Remove from Quest Line',
        action: async (n) => {
          await QuestLineStore.removeQuest(currentQuestLine, n.id);
          populateToolbarAddQuest();
          refreshMindmap();
        }
      });
    }
    return items;
  },
};

// ── Tab switching ────────────────────────────────────────────────────
function switchTab(tab) {
  currentTab = tab;
  $('#tab-quests').classList.toggle('active', tab === 'quests');
  $('#tab-npcs').classList.toggle('active', tab === 'npcs');

  if (tab === 'quests') {
    $('#tab-quests-content').style.display = 'flex';
    $('#tab-npcs-content').style.display = 'none';
    loadQuests();
  } else {
    $('#tab-quests-content').style.display = 'none';
    $('#tab-npcs-content').style.display = 'flex';
    loadNpcs();
  }
}

// ── Quest list ───────────────────────────────────────────────────────
async function loadQuests() {
  const resp = await fetch('/api/quests');
  allQuests = await resp.json();

  const allIds = allQuests.map(q => q.id);
  await QuestLineStore.init(allIds);

  // update toolbar quest line name
  $('#tl-current-name').textContent = currentQuestLine;

  renderQuestLineSidebar();

  // init mindmap once
  if (!redrawFn) {
    const canvas = $('#mindmap-canvas');
    const result = Mindmap.attachEvents(canvas, viewState, mindmapCallbacks);
    redrawFn = result.redraw;
    // bind editor close once
    $('#editor-close').addEventListener('click', closeQuestEditor);
  }

  // Attach dirty-tracking listeners once (avoids stacking on tab switches)
  if (!window._dirtyListenersAttached) {
    window._dirtyListenersAttached = true;
    $('#editor-body').addEventListener('input', () => { isFormDirty = true; });
    $('#editor-body').addEventListener('change', () => { isFormDirty = true; });
  }

  refreshMindmap();
}

function refreshMindmap() {
  const questIds = QuestLineStore.get(currentQuestLine) || [];
  const questSet = new Set(questIds);
  const currentQuests = allQuests.filter(q => questSet.has(q.id));
  viewState.nodes = Mindmap.layoutNodes(currentQuests);
  viewState.hoveredNode = null;
  viewState.selectedNode = null;
  viewState.panX = 0;
  viewState.panY = 0;
  viewState.zoom = 1.0;
  if (redrawFn) redrawFn();
}

// ── Quest line sidebar (left) ────────────────────────────────────────
function renderQuestLineSidebar() {
  const container = $('#questline-list');
  const lines = QuestLineStore.getAll();

  container.innerHTML = '';
  for (const name of Object.keys(lines)) {
    const row = document.createElement('div');
    row.className = 'questline-item';
    if (name === currentQuestLine) row.classList.add('active');

    const nameSpan = document.createElement('span');
    nameSpan.className = 'ql-name';
    nameSpan.textContent = name;
    nameSpan.onclick = () => selectQuestLine(name);

    const renameBtn = document.createElement('button');
    renameBtn.className = 'ql-btn';
    renameBtn.textContent = '✎'; // ✎
    renameBtn.title = 'Rename';
    renameBtn.onclick = async (e) => {
      e.stopPropagation();
      const newName = prompt('New name:', name);
      if (newName && newName !== name) {
        try {
          await QuestLineStore.rename(name, newName);
          if (currentQuestLine === name) currentQuestLine = newName;
          renderQuestLineSidebar();
        } catch (err) { alert(err.message); }
      }
    };

    const delBtn = document.createElement('button');
    delBtn.className = 'ql-btn';
    delBtn.textContent = '×'; // ×
    delBtn.title = 'Delete';
    delBtn.onclick = async (e) => {
      e.stopPropagation();
      if (!confirm('Delete quest line "' + name + '"?')) return;
      try {
        await QuestLineStore.delete(name);
        if (currentQuestLine === name) currentQuestLine = 'default';
        renderQuestLineSidebar();
        refreshMindmap();
        $('#tl-current-name').textContent = currentQuestLine;
      } catch (err) { alert(err.message); }
    };

    row.append(nameSpan, renameBtn, delBtn);
    container.appendChild(row);
  }

  // new quest line button
  $('#ql-new-btn').onclick = async () => {
    const name = prompt('Quest line name:');
    if (!name) return;
    try {
      await QuestLineStore.create(name);
      renderQuestLineSidebar();
    } catch (err) { alert(err.message); }
  };
}

function selectQuestLine(name) {
  currentQuestLine = name;
  $('#tl-current-name').textContent = name;
  renderQuestLineSidebar();
  refreshMindmap();
}
function closeQuestEditor() {
  currentQuestId = null;
  isFormDirty = false;
  $('#quest-editor').classList.add('collapsed');
  setTimeout(() => { if (redrawFn) redrawFn(); }, 350);
}

async function openQuestEditor(questId) {
  if (isFormDirty && !confirm('You have unsaved changes. Discard them?')) return;
  $('#quest-editor').classList.remove('collapsed');
  setTimeout(() => { if (redrawFn) redrawFn(); }, 350);
  currentQuestId = questId;
  const resp = await fetch('/api/quests/' + questId);
  if (!resp.ok) return;
  const data = await resp.json();

  $('#editor-title').textContent = questId;
  $('#editor-footer').style.display = '';
  $('#editor-save-msg').textContent = '';

  if (typeof buildFormHtml === 'function') {
    let npcs = [];
    try {
      const npcResp = await fetch('/api/npcs');
      npcs = await npcResp.json();
    } catch (_) {}

    if (typeof formTarget !== 'undefined') formTarget = '#editor-body';

    // compute quest line membership
    const allLines = typeof QuestLineStore !== 'undefined' ? QuestLineStore.getAll() : {};
    const allQuestLineNames = Object.keys(allLines);
    const questLineNames = allQuestLineNames.filter(name => {
      const quests = allLines[name];
      return quests && quests.includes(questId);
    });

    const formEl = buildFormHtml(data, npcs, allQuests.map(q => ({ id: q.id })), false, questLineNames, allQuestLineNames);
    $('#editor-body').innerHTML = '';
    if (typeof formEl === 'string') {
      $('#editor-body').innerHTML = formEl;
    } else if (formEl) {
      $('#editor-body').appendChild(formEl);
    }
  }

  wireEditorEvents();
  isFormDirty = false;

  // wire buttons
  $('#editor-save').onclick = async () => {
    if (typeof saveForm === 'function') {
      const ok = await saveForm();
      if (ok) {
        $('#editor-save-msg').textContent = 'Saved!';
        $('#editor-save-msg').className = 'save-msg ok';
        if (typeof formViewMode === 'undefined' || formViewMode === 'form') {
          setTimeout(() => {
            closeQuestEditor();
            loadQuests();
          }, 500);
        } else {
          setTimeout(() => {
            $('#editor-save-msg').textContent = '';
          }, 3000);
        }
      }
    }
  };

  $('#editor-raw').textContent = 'Raw JSON';
  $('#editor-raw').onclick = () => {
    if (typeof switchToRaw === 'function') switchToRaw();
  };
}

function wireEditorEvents() {
  document.querySelectorAll('input[name="scope"]').forEach(r => {
    r.addEventListener('change', () => {
      const mode = $('#fs-mode');
      if (mode) mode.style.display = r.value === 'SERVER' ? '' : 'none';
    });
  });

  document.querySelectorAll('input[name="prerequisite_mode"]').forEach(r => {
    r.addEventListener('change', () => {
      const isNone = r.value === 'NONE';
      const addRow = document.getElementById('prereq-add-row');
      const rows = document.getElementById('prereq-rows');
      if (addRow) addRow.style.display = isNone ? 'none' : '';
      if (rows) rows.style.display = isNone ? 'none' : '';
    });
  });

  if (document.querySelector('input[name="prerequisite_mode"]:checked')?.value === 'NONE') {
    const addRow = document.getElementById('prereq-add-row');
    const rows = document.getElementById('prereq-rows');
    if (addRow) addRow.style.display = 'none';
    if (rows) rows.style.display = 'none';
  }
}

async function openNewQuestEditor() {
  if (isFormDirty && !confirm('You have unsaved changes. Discard them?')) return;
  $('#quest-editor').classList.remove('collapsed');
  setTimeout(() => { if (redrawFn) redrawFn(); }, 350);

  const template = JSON.parse(JSON.stringify(DEFAULT_QUEST_TEMPLATE));
  currentQuestId = '__new__';

  $('#editor-title').textContent = 'New Quest';
  $('#editor-footer').style.display = '';
  $('#editor-save-msg').textContent = '';

  if (typeof buildFormHtml === 'function') {
    let npcs = [];
    try {
      const npcResp = await fetch('/api/npcs');
      npcs = await npcResp.json();
    } catch (_) {}

    if (currentQuestId !== '__new__') return; // user already navigated away

    const formEl = buildFormHtml(template, npcs, allQuests.map(q => ({ id: q.id })), false, [currentQuestLine], Object.keys(QuestLineStore.getAll()));
    $('#editor-body').innerHTML = '';
    if (typeof formEl === 'string') {
      $('#editor-body').innerHTML = formEl;
    } else if (formEl) {
      $('#editor-body').appendChild(formEl);
    }
    wireEditorEvents();
    isFormDirty = false;
  }

  // Override save to add quest to current quest line
  $('#editor-save').onclick = async () => {
    if (typeof saveForm !== 'function') return;
    const ok = await saveForm();
    if (ok) {
      const savedId = currentQuestId;
      if (savedId && savedId !== '__new__') {
        await QuestLineStore.addQuest(currentQuestLine, savedId);
      }
      $('#editor-save-msg').textContent = 'Saved!';
      $('#editor-save-msg').className = 'save-msg ok';
      setTimeout(() => {
        closeQuestEditor();
        loadQuests();
      }, 500);
    }
  };

  $('#editor-raw').textContent = 'Raw JSON';
  $('#editor-raw').onclick = () => {
    if (typeof switchToRaw === 'function') switchToRaw();
  };
}

// ── NPC list ─────────────────────────────────────────────────────────
async function loadNpcs() {
  const resp = await fetch('/api/npcs');
  const npcs = await resp.json();
  const sidebar = $('#sidebar');
  sidebar.innerHTML = '';

  if (npcs.length === 0) {
    $('#editor-area').innerHTML = '<p class="empty-state">No NPCs found.</p>';
    return;
  }

  npcs.forEach((n, i) => {
    const btn = document.createElement('button');
    btn.className = 'sidebar-item';
    btn.textContent = n.id;
    btn.style.animationDelay = (i * 50) + 'ms';
    btn.onclick = () => showNpc(n);
    sidebar.appendChild(btn);
  });

  $('#editor-area').innerHTML = '<p class="empty-state">Select an NPC from the sidebar to view.</p>';
}

function showNpc(npc) {
  const text = JSON.stringify(npc, null, 2);
  $('#editor-area').innerHTML = '<h2>NPCs</h2><pre>' + escapeHtml(text) + '</pre>';
  $$('.sidebar-item').forEach(b => {
    b.classList.toggle('active', b.textContent === npc.id);
  });
}

// ── Toolbar buttons ──────────────────────────────────────────────────
$('#tl-add-quest-btn').addEventListener('click', () => {
  openNewQuestEditor();
});

$('#tl-fit-btn').addEventListener('click', () => {
  viewState.zoom = 1.0;
  viewState.panX = 0;
  viewState.panY = 0;
  if (redrawFn) redrawFn();
});

// ── Helpers ──────────────────────────────────────────────────────────
function escapeHtml(s) {
  return s.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;');
}

function $$(sel) { return document.querySelectorAll(sel); }

// ── Keyboard shortcut ────────────────────────────────────────────────
document.addEventListener('keydown', (e) => {
  if ((e.ctrlKey || e.metaKey) && e.key === 's') {
    e.preventDefault();
    const footer = $('#editor-footer');
    if (footer && footer.style.display !== 'none' && typeof saveForm === 'function') {
      saveForm();
    }
  }
});

// ── Init ─────────────────────────────────────────────────────────────
loadQuests();
