const QuestLineStore = (() => {
  const BASE = '/api/questlines';
  let _lines = {}; // {name: string[]}
  let _backendAvailable = false;

  async function _fetchAll() {
    try {
      const resp = await fetch(BASE);
      if (!resp.ok) throw new Error(`GET ${BASE} ${resp.status}`);
      const list = await resp.json(); // [{name, quests}, ...]
      _backendAvailable = true;
      _lines = {};
      for (const entry of list) _lines[entry.name] = entry.quests;
      return true;
    } catch (_) {
      _backendAvailable = false;
      return false;
    }
  }

  async function init(allQuestIds) {
    const ok = await _fetchAll();
    if (!ok || !_lines['default']) {
      // backend not available or no default — create in-memory
      _lines['default'] = [...allQuestIds];
    }
    return _lines;
  }

  function getAll() {
    return _lines;
  }

  function get(name) {
    return _lines[name];
  }

  async function create(name) {
    if (!name || _lines[name]) throw new Error('Invalid or duplicate quest line name');
    if (_backendAvailable) {
      const resp = await fetch(BASE, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ name, quests: [] })
      });
      if (!resp.ok) {
        const err = await resp.json().catch(() => ({}));
        throw new Error(err.error || `POST ${BASE} ${resp.status}`);
      }
      await _fetchAll();
    } else {
      _lines[name] = [];
    }
    return _lines;
  }

  async function _delete(name) {
    if (_backendAvailable) {
      const resp = await fetch(`${BASE}/${encodeURIComponent(name)}`, { method: 'DELETE' });
      if (!resp.ok) {
        const err = await resp.json().catch(() => ({}));
        throw new Error(err.error || `DELETE ${name} ${resp.status}`);
      }
      await _fetchAll();
    } else {
      delete _lines[name];
    }
    return _lines;
  }

  async function rename(oldName, newName) {
    if (!newName || _lines[newName]) throw new Error('Invalid or duplicate quest line name');
    if (_backendAvailable) {
      const resp = await fetch(`${BASE}/${encodeURIComponent(oldName)}`, {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ name: newName })
      });
      if (!resp.ok) {
        const err = await resp.json().catch(() => ({}));
        throw new Error(err.error || `PUT ${oldName} ${resp.status}`);
      }
      await _fetchAll();
    } else {
      _lines[newName] = _lines[oldName];
      delete _lines[oldName];
    }
    return _lines;
  }

  async function addQuest(lineName, questId) {
    const quests = _lines[lineName];
    if (!quests) throw new Error(`Quest line "${lineName}" not found`);
    if (quests.includes(questId)) return _lines;
    const updated = [...quests, questId];
    if (_backendAvailable) {
      const resp = await fetch(`${BASE}/${encodeURIComponent(lineName)}/quests`, {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ quests: updated })
      });
      if (!resp.ok) throw new Error(`PUT quests ${lineName} ${resp.status}`);
    }
    _lines[lineName] = updated;
    return _lines;
  }

  async function removeQuest(lineName, questId) {
    const quests = _lines[lineName];
    if (!quests) throw new Error(`Quest line "${lineName}" not found`);
    if (!quests.includes(questId)) return _lines;
    const updated = quests.filter(q => q !== questId);
    if (_backendAvailable) {
      const resp = await fetch(`${BASE}/${encodeURIComponent(lineName)}/quests`, {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ quests: updated })
      });
      if (!resp.ok) throw new Error(`PUT quests ${lineName} ${resp.status}`);
    }
    _lines[lineName] = updated;
    return _lines;
  }

  function getQuestsNotIn(lineName, allQuestIds) {
    const quests = _lines[lineName];
    if (!quests) return allQuestIds;
    const set = new Set(quests);
    return allQuestIds.filter(id => !set.has(id));
  }

  return { init, getAll, get, create, delete: _delete, rename, addQuest, removeQuest, getQuestsNotIn };
})();
