const I18n = (() => {
  let _strings = {};
  let _lang = 'en_us';
  let _ready = false;

  async function init() {
    try {
      const resp = await fetch('/api/lang');
      if (!resp.ok) { _ready = true; return; }
      const data = await resp.json();
      _lang = data.lang || 'en_us';
      _strings = data.strings || {};
    } catch (_) {
      // fallback: empty strings, keys will show as-is
    }
    _ready = true;
  }

  /**
   * Translate a key. Supports {placeholder} substitution.
   * Usage: I18n.t('msg.delete_confirm', { id: 'my_quest' })
   */
  function t(key, params) {
    let text = _strings[key];
    if (text === undefined) return key;
    if (params) {
      for (const [k, v] of Object.entries(params)) {
        text = text.replace('{' + k + '}', v);
      }
    }
    return text;
  }

  function lang() {
    return _lang;
  }

  function ready() {
    return _ready;
  }

  return { init, t, lang, ready };
})();
