/**
 * Client-side i18n (no page reload).
 * - Loads /api/i18n?lang=<ko|ja|en> (JSON map of {key:value})
 * - Applies to DOM:
 *    data-i18n="key" => textContent
 *    data-i18n-html="key" => innerHTML
 *    data-i18n-placeholder="key" => placeholder
 *    data-i18n-title="key" => title
 *
 * Language persistence:
 * - localStorage: TABI_LANG
 * - Also calls POST /api/i18n/lang?lang=xx to set TABI_LANG cookie for server-side endpoints (optional)
 */
(function () {
  const SUPPORTED = new Set(['ko', 'ja', 'en']);
  const LS_KEY = 'TABI_LANG';
  const DEFAULT = 'ko';

  let dict = {};
  let lang = getLang();

  function getLang() {
    const fromLS = (localStorage.getItem(LS_KEY) || '').trim();
    if (SUPPORTED.has(fromLS)) return fromLS;

    // fallback: <html lang="..">
    const docLang = (document.documentElement.getAttribute('lang') || '').trim().slice(0,2);
    if (SUPPORTED.has(docLang)) return docLang;

    return DEFAULT;
  }

  function setLang(next) {
    if (!SUPPORTED.has(next)) return;
    lang = next;
    localStorage.setItem(LS_KEY, next);
    document.documentElement.setAttribute('lang', next);

    // best-effort: set cookie for server-side locale use (no reload needed)
    fetch(`/api/i18n/lang?lang=${encodeURIComponent(next)}`, { method: 'POST' }).catch(() => {});
  }

  function t(key) {
    return dict[key] ?? key;
  }

  function applyTranslations(root = document) {
    root.querySelectorAll('[data-i18n]').forEach(el => {
      const key = el.getAttribute('data-i18n');
      el.textContent = t(key);
    });
    root.querySelectorAll('[data-i18n-html]').forEach(el => {
      const key = el.getAttribute('data-i18n-html');
      el.innerHTML = t(key);
    });
    root.querySelectorAll('[data-i18n-placeholder]').forEach(el => {
      const key = el.getAttribute('data-i18n-placeholder');
      el.setAttribute('placeholder', t(key));
    });
    root.querySelectorAll('[data-i18n-title]').forEach(el => {
      const key = el.getAttribute('data-i18n-title');
      el.setAttribute('title', t(key));
    });
  }

  async function loadBundle(nextLang) {
    const res = await fetch(`/api/i18n?lang=${encodeURIComponent(nextLang)}`, { cache: 'no-store' });
    if (!res.ok) throw new Error('i18n bundle load failed: ' + res.status);
    dict = await res.json();
  }

  async function changeLanguage(nextLang) {
    if (!SUPPORTED.has(nextLang)) return;
    setLang(nextLang);
    await loadBundle(nextLang);
    applyTranslations(document);

    // notify listeners (map/search components, etc.)
    window.dispatchEvent(new CustomEvent('tabi:lang-changed', { detail: { lang: nextLang, dict } }));
  }

  // init
  async function init() {
    try {
      await loadBundle(lang);
      applyTranslations(document);
      window.dispatchEvent(new CustomEvent('tabi:i18n-ready', { detail: { lang, dict } }));
    } catch (e) {
      console.error(e);
    }
  }

  // expose
  window.TabiI18n = {
    get lang() { return lang; },
    t,
    changeLanguage,
    applyTranslations,
  };

  document.addEventListener('DOMContentLoaded', init);
})();
