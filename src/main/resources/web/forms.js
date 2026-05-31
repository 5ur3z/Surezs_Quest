let formData = null;
let formTarget = '#editor-body'; // '#editor-body' (quest editor) or '#editor-area' (NPCs tab)
let formViewMode = 'form'; // 'form' or 'raw'

// ── Form view ────────────────────────────────────────────────────────
async function openQuestForm(id) {
  formTarget = '#editor-area';
  currentQuestId = id;
  const resp = await fetch('/api/quests/' + id);
  if (!resp.ok) {
    document.querySelector(formTarget).innerHTML = '<p class="empty-state">Failed to load quest.</p>';
    return;
  }
  formData = await resp.json();

  // fetch NPCs for select + quests for prerequisites
  const [npcs, quests] = await Promise.all([
    fetch('/api/npcs').then(r => r.json()),
    fetch('/api/quests').then(r => r.json())
  ]);

  const html = buildFormHtml(formData, npcs, quests, true);
  document.querySelector(formTarget).innerHTML = html;

  // scope change → toggle Mode visibility
  document.querySelectorAll('input[name="scope"]').forEach(r => {
    r.addEventListener('change', () => {
      const mode = $('#fs-mode');
      if (mode) mode.style.display = r.value === 'SERVER' ? '' : 'none';
    });
  });

  // prerequisite_mode change → toggle prereq rows visibility
  document.querySelectorAll('input[name="prerequisite_mode"]').forEach(r => {
    r.addEventListener('change', () => {
      const isNone = r.value === 'NONE';
      const addRow = document.getElementById('prereq-add-row');
      const rows = document.getElementById('prereq-rows');
      if (addRow) addRow.style.display = isNone ? 'none' : '';
      if (rows) rows.style.display = isNone ? 'none' : '';
    });
  });

  // Initial state: hide prereq rows if NONE mode
  if (document.querySelector('input[name="prerequisite_mode"]:checked')?.value === 'NONE') {
    const addRow = document.getElementById('prereq-add-row');
    const rows = document.getElementById('prereq-rows');
    if (addRow) addRow.style.display = 'none';
    if (rows) rows.style.display = 'none';
  }

  // sidebar active
  $$('.sidebar-item').forEach(b => b.classList.toggle('active', b.textContent === id));
}

function buildFormHtml(data, npcs, quests, showToolbar, questLineNames, allQuestLineNames) {
  if (showToolbar === undefined) showToolbar = true;
  if (questLineNames === undefined) questLineNames = [];
  if (allQuestLineNames === undefined) allQuestLineNames = [];
  const npcOpts = npcs.map(n => `<option value="${n.id}" ${n.id === data.npc_id ? 'selected' : ''}>${n.id}</option>`).join('');
  const prereqIds = data.prerequisites || [];

  const objRows = (data.objectives || []).map(o => buildObjectiveRow(o)).join('');
  const rewRows = (data.rewards || []).map(r => buildRewardRow(r)).join('');

  const qlCheckboxes = allQuestLineNames.map(name => {
    const checked = questLineNames.includes(name) ? ' checked' : '';
    return `<div class="flag-group"><label><input type="checkbox" name="questline" value="${esc(name)}"${checked}> ${esc(name)}</label></div>`;
  }).join('');

  const toolbar = showToolbar ? `
<div class="toolbar">
  <button class="btn-save" onclick="saveForm()">Save</button>
  <span class="save-msg" id="save-msg"></span>
  <button class="btn-raw" onclick="switchToRaw()">Raw JSON</button>
</div>` : '';

  return `
<div class="form-section"><h3 class="section-title">Identity</h3>
  <div class="form-row"><span class="form-label">ID</span><input name="id" value="${data.id}"></div>
  <div class="form-row"><span class="form-label">NPC</span><select name="npc_id">${npcOpts}</select></div>
</div>

<div class="form-section"><h3 class="section-title">Quest Lines</h3>
  ${qlCheckboxes || '<p class="empty-state">No quest lines defined</p>'}
</div>

<div class="form-section"><h3 class="section-title">Scope</h3>
  <div class="radio-group"><label><input type="radio" name="scope" value="PLAYER" ${data.scope !== 'SERVER' ? 'checked' : ''}> PLAYER</label></div>
  <div class="radio-group"><label><input type="radio" name="scope" value="SERVER" ${data.scope === 'SERVER' ? 'checked' : ''}> SERVER</label></div>
</div>

<div id="fs-mode" class="form-section" style="display:${data.scope === 'SERVER' ? '' : 'none'}"><h3 class="section-title">Reward Mode</h3>
  <div class="radio-group"><label><input type="radio" name="reward_mode" value="PER_CONTRIBUTOR" ${data.reward_mode !== 'ALL_ONLINE' ? 'checked' : ''}> PER_CONTRIBUTOR (per contributor)</label></div>
  <div class="radio-group"><label><input type="radio" name="reward_mode" value="ALL_ONLINE" ${data.reward_mode === 'ALL_ONLINE' ? 'checked' : ''}> ALL_ONLINE</label></div>
</div>

<div class="form-section"><h3 class="section-title">Flags</h3>
  <div class="flag-group"><label><input type="checkbox" name="can_reject" ${data.can_reject ? 'checked' : ''}> can_reject</label></div>
  <div class="flag-group"><label><input type="checkbox" name="repeatable" ${data.repeatable ? 'checked' : ''}> repeatable</label></div>
  <div class="flag-group"><label><input type="checkbox" name="auto_accept" ${data.auto_accept ? 'checked' : ''}> auto_accept</label></div>
</div>

<div class="form-section"><h3 class="section-title">Prerequisites</h3>
  <div class="radio-group" style="margin-bottom:6px"><label><input type="radio" name="prerequisite_mode" value="ALL" ${data.prerequisite_mode !== 'ANY' && data.prerequisite_mode !== 'NONE' ? 'checked' : ''}> ALL</label></div>
  <div class="radio-group" style="margin-bottom:6px"><label><input type="radio" name="prerequisite_mode" value="ANY" ${data.prerequisite_mode === 'ANY' ? 'checked' : ''}> ANY</label></div>
  <div class="radio-group" style="margin-bottom:6px"><label><input type="radio" name="prerequisite_mode" value="NONE" ${data.prerequisite_mode === 'NONE' ? 'checked' : ''}> NONE</label></div>
  <div id="prereq-rows">${renderPrereqRows(prereqIds)}</div>
  <div id="prereq-add-row" class="form-row" style="margin-top:6px">
    <select id="prereq-select">${quests.filter(q => q.id !== data.id && !prereqIds.includes(q.id)).map(q => `<option value="${q.id}">${q.id}</option>`).join('')}</select>
    <button class="btn-add" onclick="addPrereq()" ${quests.filter(q => q.id !== data.id && !prereqIds.includes(q.id)).length === 0 ? 'disabled' : ''}>+ Add</button>
  </div>
</div>

<div class="form-section"><h3 class="section-title">Timing</h3>
  <div class="form-row"><span class="form-label">Time limit</span><input type="number" name="time_limit_ticks" value="${data.time_limit_ticks || 0}" min="0" style="flex:0 0 100px"><span style="font-size:0.78rem;color:var(--text-dim);flex-shrink:0">ticks</span></div>
</div>

<div class="form-section"><h3 class="section-title">Objectives</h3>
  <div id="obj-rows">${objRows || '<p class="empty-state">No objectives</p>'}</div>
  <button class="btn-add" onclick="addObjRow()">+ Add Objective</button>
</div>

<div class="form-section"><h3 class="section-title">Rewards</h3>
  <div id="rew-rows">${rewRows || '<p class="empty-state">No rewards</p>'}</div>
  <button class="btn-add" onclick="addRewRow()">+ Add Reward</button>
</div>

<div class="form-section"><h3 class="section-title">Dialogue</h3>
  <div class="form-row"><span class="form-label">Give (desc)</span><textarea name="dlg_give" rows="2">${esc(data.dialogue?.give || '')}</textarea></div>
  <div class="form-row"><span class="form-label">Accept</span><textarea name="dlg_accept" rows="2">${esc(data.dialogue?.accept || '')}</textarea></div>
  <div class="form-row"><span class="form-label">Decline</span><textarea name="dlg_decline" rows="2">${esc(data.dialogue?.decline || '')}</textarea></div>
  <div class="form-row"><span class="form-label">In Progress</span><textarea name="dlg_in_progress" rows="2">${esc(data.dialogue?.in_progress || '')}</textarea></div>
  <div class="form-row"><span class="form-label">Complete</span><textarea name="dlg_complete" rows="2">${esc(data.dialogue?.complete || '')}</textarea></div>
</div>

${toolbar}
`;
}

function buildObjectiveRow(obj) {
  const types = ['submit_items','find_items','kill_entity','craft_item','reach_location'];
  const opts = types.map(t => `<option value="${t}" ${obj.type === t ? 'selected' : ''}>${t}</option>`).join('');
  return `<div class="obj-row" data-type="${obj.type}">
    <select class="obj-type" onchange="rebuildObjRow(this)">${opts}</select>
    ${objFields(obj)}
    <button class="btn-del" onclick="delObjRow(this)" title="Remove">−</button>
  </div>`;
}

function objFields(obj) {
  switch (obj.type) {
    case 'reach_location':
      return `<input class="obj-dim" value="${obj.dimension || 'minecraft:overworld'}">
        x<input type="number" class="obj-x" value="${obj.x || 0}">
        y<input type="number" class="obj-y" value="${obj.y || 64}">
        z<input type="number" class="obj-z" value="${obj.z || 0}">
        r<input type="number" class="obj-r" value="${obj.radius || 5}">`;
    case 'kill_entity':
      return `<input class="obj-item" value="${obj.entity_type_id || ''}">
        x<input type="number" class="obj-count" value="${obj.count || 1}" min="1">`;
    default:
      return `<input class="obj-item" value="${obj.item || ''}">
        x<input type="number" class="obj-count" value="${obj.count || 1}" min="1">`;
  }
}

function buildRewardRow(rw) {
  const types = ['ITEM','EXPERIENCE','COMMAND','FUNCTION'];
  const opts = types.map(t => `<option value="${t}" ${rw.type === t ? 'selected' : ''}>${t}</option>`).join('');
  return `<div class="rew-row" data-type="${rw.type}">
    <select class="rew-type" onchange="rebuildRewRow(this)">${opts}</select>
    ${rewFields(rw)}
    <button class="btn-icon-toggle" onclick="toggleRewIcon(this)" title="Toggle icon">icon</button>
    <button class="btn-del" onclick="delRewRow(this)" title="Remove">−</button>
  </div>`;
}

function rewFields(rw) {
  const hasIcon = !!(rw.icon);
  const iconHtml = `<span class="rew-icon-wrap${hasIcon ? ' show' : ''}">icon:<input class="rew-icon" value="${esc(rw.icon || '')}"></span>`;
  switch (rw.type) {
    case 'ITEM':
      return `<input class="rew-item-id" value="${rw.item?.id || ''}">
        x<input type="number" class="rew-item-count" value="${rw.item?.count || 1}" min="1">${iconHtml}`;
    case 'EXPERIENCE':
      return `<input type="number" class="rew-exp" value="${rw.experience || 0}" min="0">${iconHtml}`;
    case 'COMMAND':
      return `<input class="rew-cmd" value="${esc(rw.command || '')}">${iconHtml}`;
    case 'FUNCTION':
      return `<input class="rew-func" value="${rw.function || ''}">${iconHtml}`;
    default:
      return '';
  }
}

// ── Prerequisites add/remove ──────────────────────────────────────────
function renderPrereqRows(ids) {
  if (!ids || ids.length === 0) return '';
  return ids.map(id => `<div class="prereq-row" data-id="${id}"><span>${id}</span><button class="btn-del" onclick="delPrereq(this)">−</button></div>`).join('');
}

function addPrereq() {
  const sel = $('#prereq-select');
  const val = sel.value;
  if (!val) return;
  // add row
  const container = $('#prereq-rows');
  const empty = container.querySelector('.empty-state');
  if (empty) empty.remove();
  const div = document.createElement('div');
  div.innerHTML = `<div class="prereq-row" data-id="${val}"><span>${val}</span><button class="btn-del" onclick="delPrereq(this)">−</button></div>`;
  container.appendChild(div.firstElementChild);
  // remove from select
  sel.querySelector(`option[value="${val}"]`).remove();
  // disable [+] if select empty
  if (sel.options.length === 0) sel.nextElementSibling.disabled = true;
}

function delPrereq(btn) {
  const row = btn.closest('.prereq-row');
  const id = row.dataset.id;
  // add back to select
  const sel = $('#prereq-select');
  const opt = document.createElement('option');
  opt.value = id;
  opt.textContent = id;
  sel.appendChild(opt);
  sel.nextElementSibling.disabled = false;
  row.remove();
  // show empty state if no rows left
  if (!document.querySelector('.prereq-row')) {
    $('#prereq-rows').innerHTML = '';
  }
}

// ── Objective add/remove ───────────────────────────────────────────────
function addObjRow() {
  const container = $('#obj-rows');
  const dummy = { type: 'submit_items', item: 'minecraft:', count: 1 };
  const div = document.createElement('div');
  div.innerHTML = buildObjectiveRow(dummy);
  container.appendChild(div.firstElementChild);
}

function delObjRow(btn) {
  const rows = document.querySelectorAll('.obj-row');
  if (rows.length <= 1) return; // minimum 1
  btn.closest('.obj-row').remove();
}

function addRewRow() {
  const container = $('#rew-rows');
  const dummy = { type: 'ITEM', item: { id: 'minecraft:', count: 1 } };
  const div = document.createElement('div');
  div.innerHTML = buildRewardRow(dummy);
  container.appendChild(div.firstElementChild);
}

function delRewRow(btn) {
  const rows = document.querySelectorAll('.rew-row');
  if (rows.length <= 1) return;
  btn.closest('.rew-row').remove();
}

function toggleRewIcon(btn) {
  const wrap = btn.closest('.rew-row').querySelector('.rew-icon-wrap');
  wrap.classList.toggle('show');
}

// ── Serialize ──────────────────────────────────────────────────────────
function serializeForm() {
  const el = (s) => document.querySelector(s);
  const radio = (name) => { const r = document.querySelector(`input[name="${name}"]:checked`); return r ? r.value : ''; };
  const checks = (name) => [...document.querySelectorAll(`input[name="${name}"]:checked`)].map(c => c.value);

  const objRows = [...document.querySelectorAll('.obj-row')].map(row => {
    const type = row.querySelector('.obj-type').value;
    const obj = { type };
    switch (type) {
      case 'reach_location':
        obj.dimension = row.querySelector('.obj-dim')?.value || '';
        obj.x = parseInt(row.querySelector('.obj-x')?.value) || 0;
        obj.y = parseInt(row.querySelector('.obj-y')?.value) || 0;
        obj.z = parseInt(row.querySelector('.obj-z')?.value) || 0;
        obj.radius = parseInt(row.querySelector('.obj-r')?.value) || 5;
        break;
      case 'kill_entity':
        obj.entity_type_id = row.querySelector('.obj-item')?.value || '';
        obj.count = parseInt(row.querySelector('.obj-count')?.value) || 1;
        break;
      default:
        obj.item = row.querySelector('.obj-item')?.value || '';
        obj.count = parseInt(row.querySelector('.obj-count')?.value) || 1;
        break;
    }
    return obj;
  });

  const rewRows = [...document.querySelectorAll('.rew-row')].map(row => {
    const type = row.querySelector('.rew-type').value;
    const rw = { type };
    const icon = row.querySelector('.rew-icon')?.value || '';
    if (icon) rw.icon = icon;
    switch (type) {
      case 'ITEM':
        rw.item = {
          id: row.querySelector('.rew-item-id')?.value || '',
          count: parseInt(row.querySelector('.rew-item-count')?.value) || 1
        };
        break;
      case 'EXPERIENCE':
        rw.experience = parseInt(row.querySelector('.rew-exp')?.value) || 0;
        break;
      case 'COMMAND':
        rw.command = row.querySelector('.rew-cmd')?.value || '';
        break;
      case 'FUNCTION':
        rw.function = row.querySelector('.rew-func')?.value || '';
        break;
    }
    return rw;
  });

  return {
    id: el('[name="id"]').value,
    npc_id: el('[name="npc_id"]').value,
    scope: radio('scope'),
    reward_mode: radio('reward_mode'),
    prerequisite_mode: radio('prerequisite_mode'),
    can_reject: el('[name="can_reject"]').checked,
    repeatable: el('[name="repeatable"]').checked,
    auto_accept: el('[name="auto_accept"]').checked,
    time_limit_ticks: parseInt(el('[name="time_limit_ticks"]').value) || 0,
    objectives: objRows,
    rewards: rewRows,
    prerequisites: radio('prerequisite_mode') === 'NONE' ? [] : [...document.querySelectorAll('.prereq-row')].map(r => r.dataset.id),
    dialogue: {
      give: el('[name="dlg_give"]').value,
      accept: el('[name="dlg_accept"]').value,
      decline: el('[name="dlg_decline"]').value,
      in_progress: el('[name="dlg_in_progress"]').value,
      complete: el('[name="dlg_complete"]').value
    }
  };
}

async function saveForm() {
  let obj;
  if (formViewMode === 'raw') {
    const raw = $('#json-editor').value;
    try {
      obj = JSON.parse(raw);
    } catch (e) {
      const msg = $('#editor-save-msg');
      if (msg) {
        msg.textContent = 'Invalid JSON: ' + e.message;
        msg.className = 'save-msg err';
      }
      return false;
    }
  } else {
    obj = serializeForm();
  }

  if (!obj.id || obj.id.trim() === '') {
    const msg = $('#editor-save-msg');
    if (msg) {
      msg.textContent = 'Quest ID cannot be empty.';
      msg.className = 'save-msg err';
    }
    return false;
  }

  try {
    const raw = JSON.stringify(obj, null, 2);
    const newId = obj.id;
    const isNew = currentQuestId === '__new__';

    // Conflict detection for new quests
    if (isNew) {
      const exists = allQuests.some(q => q.id === newId);
      if (exists && !confirm('Quest "' + newId + '" already exists. Overwrite it?')) {
        return false;
      }
    }

    const resp = await fetch('/api/quests/' + newId, { method: 'PUT', body: raw });
    const msg = $('#editor-save-msg');
    if (resp.ok) {
      if (newId !== currentQuestId || isNew) {
        currentQuestId = newId;
        if (typeof formData !== 'undefined') formData.id = newId;
        if (typeof markFormClean === 'function') markFormClean();
      }
      // Update the editor title to reflect the real quest ID
      const titleEl = $('#editor-title');
      if (titleEl) titleEl.textContent = newId;
      if (msg) {
        msg.textContent = 'Saved! Run /quest reload in-game.';
        msg.className = 'save-msg ok';
        setTimeout(() => { if (msg) msg.textContent = ''; }, 5000);
      }
      // sync quest line membership
      if (typeof QuestLineStore !== 'undefined') {
        const checkedLines = [...document.querySelectorAll('input[name="questline"]:checked')].map(c => c.value);
        const allLines = QuestLineStore.getAll();
        const currentLines = Object.keys(allLines).filter(name => {
          const quests = allLines[name];
          return quests && quests.includes(newId);
        });
        for (const line of checkedLines) {
          if (!currentLines.includes(line)) {
            await QuestLineStore.addQuest(line, newId);
          }
        }
        for (const line of currentLines) {
          if (!checkedLines.includes(line)) {
            await QuestLineStore.removeQuest(line, newId);
          }
        }
      }
      return true;
    } else {
      if (msg) {
        msg.textContent = 'Save failed.';
        msg.className = 'save-msg err';
      }
      return false;
    }
  } catch (e) {
    const msg = $('#editor-save-msg');
    if (msg) {
      msg.textContent = 'Serialization error: ' + e.message;
      msg.className = 'save-msg err';
    }
    return false;
  }
}

// ── View switching ─────────────────────────────────────────────────────
function switchToRaw() {
  formViewMode = 'raw';
  const obj = serializeForm();
  currentQuestId = obj.id;
  const jsonText = JSON.stringify(obj, null, 2);
  document.querySelector(formTarget).innerHTML = ''
    + '<div class="breadcrumb">' + currentQuestId + '</div>'
    + '<textarea id="json-editor">' + esc(jsonText) + '</textarea>';

  // Toggle footer button to "Form"
  const rawBtn = $('#editor-raw');
  if (rawBtn) {
    rawBtn.textContent = 'Form';
    rawBtn.onclick = () => { if (typeof switchToForm === 'function') switchToForm(); };
  }
}

function switchToForm() {
  formViewMode = 'form';
  const raw = $('#json-editor').value;
  try {
    formData = JSON.parse(raw);
    rebuildForm();
    // Toggle footer button back to "Raw JSON"
    const rawBtn = $('#editor-raw');
    if (rawBtn) {
      rawBtn.textContent = 'Raw JSON';
      rawBtn.onclick = () => { if (typeof switchToRaw === 'function') switchToRaw(); };
    }
  } catch (e) {
    alert('Invalid JSON: ' + e.message);
  }
}

async function rebuildForm() {
  formViewMode = 'form';
  const [npcs, quests] = await Promise.all([
    fetch('/api/npcs').then(r => r.json()),
    fetch('/api/quests').then(r => r.json())
  ]);
  const showToolbar = formTarget === '#editor-area';
  document.querySelector(formTarget).innerHTML = buildFormHtml(formData, npcs, quests, showToolbar);

  // prerequisite_mode change → toggle prereq rows visibility
  document.querySelectorAll('input[name="prerequisite_mode"]').forEach(r => {
    r.addEventListener('change', () => {
      const isNone = r.value === 'NONE';
      const addRow = document.getElementById('prereq-add-row');
      const rows = document.getElementById('prereq-rows');
      if (addRow) addRow.style.display = isNone ? 'none' : '';
      if (rows) rows.style.display = isNone ? 'none' : '';
    });
  });

  // Initial state: hide prereq rows if NONE mode
  if (document.querySelector('input[name="prerequisite_mode"]:checked')?.value === 'NONE') {
    const addRow = document.getElementById('prereq-add-row');
    const rows = document.getElementById('prereq-rows');
    if (addRow) addRow.style.display = 'none';
    if (rows) rows.style.display = 'none';
  }
}

// ── Type switch helpers ────────────────────────────────────────────────
function rebuildObjRow(select) {
  const row = select.closest('.obj-row');
  const type = select.value;
  row.setAttribute('data-type', type);
  const fields = row.querySelectorAll('input');
  // preserve common values where possible, rebuild field markup
  const countVal = row.querySelector('.obj-count')?.value || 1;
  const itemVal = row.querySelector('.obj-item')?.value || 'minecraft:';
  const dimVal = row.querySelector('.obj-dim')?.value || 'minecraft:overworld';
  const xVal = row.querySelector('.obj-x')?.value || 0;
  const yVal = row.querySelector('.obj-y')?.value || 64;
  const zVal = row.querySelector('.obj-z')?.value || 0;
  const rVal = row.querySelector('.obj-r')?.value || 5;

  // rebuild dummy obj and re-render
  const dummy = { type };
  if (type === 'reach_location') {
    dummy.dimension = dimVal; dummy.x = parseInt(xVal); dummy.y = parseInt(yVal);
    dummy.z = parseInt(zVal); dummy.radius = parseInt(rVal);
  } else if (type === 'kill_entity') {
    dummy.entity_type_id = itemVal; dummy.count = parseInt(countVal);
  } else {
    dummy.item = itemVal; dummy.count = parseInt(countVal);
  }
  select.insertAdjacentHTML('afterend', objFields(dummy));
  // remove old fields (everything after select)
  while (row.children.length > 1) row.removeChild(row.lastChild);
  row.insertAdjacentHTML('beforeend', objFields(dummy));
}

function rebuildRewRow(select) {
  const row = select.closest('.rew-row');
  const type = select.value;
  row.setAttribute('data-type', type);
  const iconVal = row.querySelector('.rew-icon')?.value || '';
  const iconShown = row.querySelector('.rew-icon-wrap')?.classList.contains('show');
  const dummy = { type };
  if (iconVal) dummy.icon = iconVal;
  if (type === 'ITEM') { dummy.item = { id: 'minecraft:', count: 1 }; }
  else if (type === 'EXPERIENCE') { dummy.experience = 0; }
  else if (type === 'COMMAND') { dummy.command = ''; }
  else if (type === 'FUNCTION') { dummy.function = ''; }
  while (row.children.length > 1) row.removeChild(row.lastChild);
  row.insertAdjacentHTML('beforeend', rewFields(dummy));
  if (iconShown) {
    const wrap = row.querySelector('.rew-icon-wrap');
    if (wrap) wrap.classList.add('show');
  }
}

// ── Helpers ────────────────────────────────────────────────────────────
function esc(s) {
  return (s || '').replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/"/g, '&quot;');
}
