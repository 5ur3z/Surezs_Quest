const $ = (s) => document.querySelector(s);

let currentQuestId = null;
let currentQuestLine = 'default';
let currentTab = 'quests';
let allQuests = [];
let npcNameMap = {};
let redrawFn = null;
const viewState = Mindmap.createViewState();

const DEFAULT_QUEST_TEMPLATE = {
  id: '',
  name: '',
  npc_id: '',
  scope: 'PLAYER',
  reward_mode: 'PER_CONTRIBUTOR',
  prerequisite_mode: 'NONE',
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
    const items = [{ label: I18n.t('ctx.edit'), action: (n) => openQuestEditor(n.id) }];
    if (currentQuestLine !== 'default') {
      items.push({
        label: I18n.t('ctx.remove_from_line'),
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

  // cache NPC names for mindmap
  try {
    const npcResp = await fetch('/api/npcs');
    const npcs = await npcResp.json();
    npcNameMap = {};
    npcs.forEach(n => { npcNameMap[n.id] = n.name || n.id; });
  } catch (_) {}

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
  const currentQuests = allQuests.filter(q => questSet.has(q.id)).map(q => ({
    ...q,
    npc_name: npcNameMap[q.npc_id] || ''
  }));
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
    renameBtn.title = I18n.t('ql.rename');
    renameBtn.onclick = async (e) => {
      e.stopPropagation();
      const newName = prompt(I18n.t('ql.rename_prompt'), name);
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
    delBtn.title = I18n.t('ql.delete');
    delBtn.onclick = async (e) => {
      e.stopPropagation();
      if (!confirm(I18n.t('ql.delete_confirm', {name: name}))) return;
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
    const name = prompt(I18n.t('ql.name_prompt'));
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
  if (isFormDirty && !confirm(I18n.t('msg.discard_confirm'))) return;
  $('#quest-editor').classList.remove('collapsed');
  setTimeout(() => { if (redrawFn) redrawFn(); }, 350);
  currentQuestId = questId;
  const resp = await fetch('/api/quests/' + questId);
  if (!resp.ok) return;
  const data = await resp.json();

  $('#editor-title').textContent = questId;
  $('#editor-footer').style.display = '';
  $('#editor-save-msg').textContent = '';
  $('#editor-save').disabled = false;
  $('#editor-delete').disabled = false;

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
        $('#editor-save-msg').textContent = I18n.t('msg.saved');
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

  $('#editor-raw').textContent = I18n.t('btn.raw_json');
  $('#editor-raw').onclick = () => {
    if (typeof switchToRaw === 'function') switchToRaw();
  };

  // Delete handler
  $('#editor-delete').style.display = '';
  $('#editor-delete').onclick = async () => {
    if (!confirm(I18n.t('btn.delete_confirm', {id: currentQuestId}))) return;
    $('#editor-save').disabled = true;
    $('#editor-delete').disabled = true;
    try {
      var resp = await fetch('/api/quests/' + encodeURIComponent(currentQuestId), { method: 'DELETE' });
      if (!resp.ok) {
        var err = await resp.json().catch(() => ({}));
        alert(I18n.t('msg.delete_failed', {error: err.error || resp.status}));
        $('#editor-save').disabled = false;
        $('#editor-delete').disabled = false;
        return;
      }
      // remove from all quest lines
      var allLines = QuestLineStore.getAll();
      for (var name of Object.keys(allLines)) {
        try { await QuestLineStore.removeQuest(name, currentQuestId); } catch (_) {}
      }
      isFormDirty = false;
      $('#editor-save').disabled = false;
      $('#editor-delete').disabled = false;
      closeQuestEditor();
      loadQuests();
    } catch (e) {
      alert(I18n.t('msg.delete_failed', {error: e.message}));
      $('#editor-save').disabled = false;
      $('#editor-delete').disabled = false;
    }
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

  if (typeof wireNameToIdSync === 'function') wireNameToIdSync();
}

async function openNewQuestEditor() {
  if (isFormDirty && !confirm(I18n.t('msg.discard_confirm'))) return;
  $('#quest-editor').classList.remove('collapsed');
  setTimeout(() => { if (redrawFn) redrawFn(); }, 350);

  const template = JSON.parse(JSON.stringify(DEFAULT_QUEST_TEMPLATE));
  currentQuestId = '__new__';

  $('#editor-title').textContent = I18n.t('editor.new_title');
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

  $('#editor-delete').style.display = 'none';
}

// ── NPC list ─────────────────────────────────────────────────────────
async function loadNpcs() {
  const resp = await fetch('/api/npcs');
  const npcs = await resp.json();

  // cache NPC names
  npcNameMap = {};
  npcs.forEach(n => { npcNameMap[n.id] = n.name || n.id; });

  const sidebar = $('#sidebar');
  sidebar.innerHTML = '';

  if (npcs.length === 0) {
    $('#editor-area').innerHTML = '<p class="empty-state">' + I18n.t('msg.no_npcs') + '</p>';
  } else {
    npcs.forEach((n, i) => {
      const btn = document.createElement('button');
      btn.className = 'sidebar-item';
      btn.textContent = n.name || n.id;
      btn.setAttribute('data-npc-id', n.id);
      btn.style.animationDelay = (i * 50) + 'ms';
      btn.onclick = () => showNpc(n);
      sidebar.appendChild(btn);
    });
    $('#editor-area').innerHTML = '<p class="empty-state">' + I18n.t('msg.select_npc') + '</p>';
  }

  var newBtn = document.createElement('button');
  newBtn.className = 'ql-new-btn';
  newBtn.style.width = 'calc(100% - 12px)';
  newBtn.style.margin = '8px 6px';
  newBtn.textContent = I18n.t('btn.new_npc');
  newBtn.onclick = function() {
    showNpc({ id: '', avatar: '' });
    var idInput = document.querySelector('[name="npc_id"]');
    if (idInput) idInput.removeAttribute('readonly');
    if (idInput) idInput.placeholder = 'surezs_quest:my_npc';
  };
  sidebar.appendChild(newBtn);
}

function showNpc(npc) {
  var html = '<div class="breadcrumb">' + escapeHtml(npc.name || npc.id || I18n.t('btn.breadcrumb_new')) + '</div>';
  html += '<div id="npc-form-wrap">';
  html += buildNpcFormHtml(npc);
  html += '</div>';
  html += '<textarea id="npc-json-editor" style="display:none; width:100%; min-height:300px; margin-top:12px;"></textarea>';
  html += '<div class="toolbar">';
  html += '<button class="btn-save" id="npc-save-btn">' + I18n.t('btn.save') + '</button>';
  html += '<span class="save-msg" id="npc-save-msg"></span>';
  html += '<button class="btn-raw" id="npc-raw-btn">' + I18n.t('btn.raw_json') + '</button>';
  html += '</div>';

  $('#editor-area').innerHTML = html;

  $$('.sidebar-item').forEach(b => {
    var btnNpcId = b.getAttribute('data-npc-id');
    b.classList.toggle('active', btnNpcId === npc.id);
  });

  var formWrap = document.getElementById('npc-form-wrap');
  var saveBtn = document.getElementById('npc-save-btn');
  if (saveBtn) {
    saveBtn.onclick = async function() {
      var jsonEditor = document.getElementById('npc-json-editor');
      var isRaw = jsonEditor && jsonEditor.style.display !== 'none';
      var obj;
      if (isRaw) {
        try {
          obj = JSON.parse(jsonEditor.value);
        } catch(e) {
          var msg = document.getElementById('npc-save-msg');
          if (msg) { msg.textContent = I18n.t('msg.invalid_json', {error: e.message}); msg.className = 'save-msg err'; }
          return;
        }
      } else {
        obj = serializeNpcForm();
      }

      try {
        var resp = await fetch('/api/npcs/' + encodeURIComponent(obj.id), {
          method: 'PUT',
          body: JSON.stringify(obj, null, 2)
        });
        var msg = document.getElementById('npc-save-msg');
        if (resp.ok) {
          if (msg) {
            msg.textContent = I18n.t('msg.saved');
            msg.className = 'save-msg ok';
            setTimeout(function() { if (msg) msg.textContent = ''; }, 5000);
          }
          loadNpcs();
        } else {
          if (msg) {
            msg.textContent = I18n.t('msg.save_failed');
            msg.className = 'save-msg err';
          }
        }
      } catch(e) {
        var msg = document.getElementById('npc-save-msg');
        if (msg) { msg.textContent = I18n.t('msg.serialization_error', {error: e.message}); msg.className = 'save-msg err'; }
      }
    };
  }

  var rawBtn = document.getElementById('npc-raw-btn');
  var jsonEditor = document.getElementById('npc-json-editor');
  if (rawBtn && jsonEditor) {
    rawBtn.onclick = function() {
      if (jsonEditor.style.display === 'none') {
        var obj = serializeNpcForm();
        jsonEditor.value = JSON.stringify(obj, null, 2);
        jsonEditor.style.display = '';
        if (formWrap) formWrap.style.display = 'none';
        rawBtn.textContent = I18n.t('btn.form');
      } else {
        try {
          var obj = JSON.parse(jsonEditor.value);
          document.querySelector('[name="npc_id"]').value = obj.id || '';
          document.querySelector('[name="npc_name"]').value = obj.name || '';
          document.querySelector('[name="npc_avatar"]').value = obj.avatar || '';
        } catch(e) {
          alert('Invalid JSON: ' + e.message);
          return;
        }
        jsonEditor.style.display = 'none';
        if (formWrap) formWrap.style.display = '';
        rawBtn.textContent = I18n.t('btn.raw_json');
      }
    };
  }
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
(async () => {
  await I18n.init();
  loadQuests();
})();
