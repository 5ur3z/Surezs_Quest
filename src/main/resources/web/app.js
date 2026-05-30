const $ = (s) => document.querySelector(s);
let currentQuestId = null;
let currentTab = 'quests';

// ── Tab switching ────────────────────────────────────────────────────
function switchTab(tab) {
  currentTab = tab;
  $('#tab-quests').classList.toggle('active', tab === 'quests');
  $('#tab-npcs').classList.toggle('active', tab === 'npcs');
  $('#sidebar').innerHTML = '';
  $('#editor-area').innerHTML = '<p class="empty-state">Loading...</p>';
  if (tab === 'quests') loadQuests();
  else loadNpcs();
}

// ── Quest list ───────────────────────────────────────────────────────
async function loadQuests() {
  const resp = await fetch('/api/quests');
  const quests = await resp.json();
  const sidebar = $('#sidebar');
  sidebar.innerHTML = '';

  if (quests.length === 0) {
    $('#editor-area').innerHTML = '<p class="empty-state">No quests found.</p>';
    return;
  }

  quests.forEach((q, i) => {
    const btn = document.createElement('button');
    btn.className = 'sidebar-item';
    btn.textContent = q.id;
    btn.style.animationDelay = (i * 50) + 'ms';
    btn.onclick = () => openQuestForm(q.id);
    sidebar.appendChild(btn);
  });

  $('#editor-area').innerHTML = '<p class="empty-state">Select a quest from the sidebar to edit.</p>';
}

// ── Open quest for editing ───────────────────────────────────────────
async function openQuest(id) {
  currentQuestId = id;
  const resp = await fetch('/api/quests/' + id);
  if (!resp.ok) {
    $('#editor-area').innerHTML = '<p class="empty-state">Failed to load quest.</p>';
    return;
  }
  const data = await resp.json();
  const jsonText = JSON.stringify(data, null, 2);

  $('#editor-area').innerHTML = ''
    + '<div class="breadcrumb">' + id + '</div>'
    + '<textarea id="json-editor">' + escapeHtml(jsonText) + '</textarea>'
    + '<div class="toolbar">'
    + '  <button class="btn-save" id="save-btn" onclick="saveQuest()">Save</button>'
    + '  <span class="save-msg" id="save-msg"></span>'
    + '</div>';

  // update sidebar active
  $$('.sidebar-item').forEach(b => {
    b.classList.toggle('active', b.textContent === id);
  });
}

// ── Save quest ───────────────────────────────────────────────────────
async function saveQuest() {
  const raw = $('#json-editor').value;
  try {
    JSON.parse(raw);
  } catch (e) {
    const msg = $('#save-msg');
    msg.textContent = 'Invalid JSON: ' + e.message;
    msg.className = 'save-msg err';
    return;
  }
  const resp = await fetch('/api/quests/' + currentQuestId, { method: 'PUT', body: raw });
  const msg = $('#save-msg');
  if (resp.ok) {
    msg.textContent = 'Saved! Run /quest reload in-game.';
    msg.className = 'save-msg ok';
    setTimeout(() => { msg.textContent = ''; }, 5000);
  } else {
    msg.textContent = 'Save failed.';
    msg.className = 'save-msg err';
  }
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

// ── Helpers ──────────────────────────────────────────────────────────
function escapeHtml(s) {
  return s.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;');
}

function $$(sel) { return document.querySelectorAll(sel); }

// ── Keyboard shortcut ────────────────────────────────────────────────
document.addEventListener('keydown', (e) => {
  if ((e.ctrlKey || e.metaKey) && e.key === 's') {
    e.preventDefault();
    if (!currentQuestId || currentTab !== 'quests') return;
    // form view takes priority over raw textarea
    if (typeof saveForm === 'function' && document.querySelector('fieldset')) {
      saveForm();
    } else {
      saveQuest();
    }
  }
});

// ── Init ─────────────────────────────────────────────────────────────
loadQuests();
