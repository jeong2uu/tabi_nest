(async function () {
  const objectEl = document.getElementById('japanMapObject');
  const panel = document.getElementById('regionPanel');
  if (!objectEl || !panel) return;

  const imgEl = document.getElementById('regionImage');
  const titleEl = document.getElementById('regionTitle');
  const descEl = document.getElementById('regionDesc');
  const ctaEl = document.getElementById('regionCta');

  const zoomInBtn = document.getElementById('zoomInBtn');
  const zoomOutBtn = document.getElementById('zoomOutBtn');
  const zoomResetBtn = document.getElementById('zoomResetBtn');

  const META_URL  = '/data/regions-jp-meta.json';
  const NAMES_URL = (lang) => `/api/regions/names?lang=${encodeURIComponent(lang)}`;
  const DESCS_URL = (lang) => `/api/regions/descs?lang=${encodeURIComponent(lang)}`;

  const ACTIVE_CLASS = 'svg-active';
  const HOVER_CLASS  = 'svg-hover';

  let metaById = {};
  let nameById = {};
  let descById = {};
  let activeId = null;

  // viewBox zoom state
  let initialViewBox = null;  // {x,y,w,h}
  let currentViewBox = null;

  function getLang(){ return (window.TabiI18n && window.TabiI18n.lang) ? window.TabiI18n.lang : (document.documentElement.lang || 'ko').slice(0,2); }

  function setCard(regionId) {
    const meta = metaById[regionId];
    const name = nameById[regionId] || regionId;
    const desc = descById[regionId] || '';

    if (imgEl && meta && meta.image) imgEl.src = meta.image;
    if (titleEl) titleEl.textContent = name;
    if (descEl) descEl.textContent = desc || '—';
    if (ctaEl) ctaEl.href = '/accommodations?region=' + encodeURIComponent(regionId);
  }

  function injectSvgStyle(svgDoc) {
    const style = svgDoc.createElementNS('http://www.w3.org/2000/svg', 'style');
    style.textContent = `
      path[id^="JP-"] {
        fill: var(--map-fill, #EAECEF);
        stroke: var(--map-stroke, #1F2937);
        stroke-width: 1;
        cursor: pointer;
        transition: fill .12s ease-in;
      }
      path[id^="JP-"].${HOVER_CLASS}  { fill: var(--map-hover, #FFE08A); }
      path[id^="JP-"].${ACTIVE_CLASS} { fill: var(--map-active, #FFB020); }
    `;
    svgDoc.documentElement.appendChild(style);
  }

  function toViewBoxStr(v) { return `${v.x} ${v.y} ${v.w} ${v.h}`; }

  // Fit viewBox to content bbox (remove huge margins)
  function fitViewBoxToContent(svgDoc) {
    const paths = Array.from(svgDoc.querySelectorAll('path[id^="JP-"]'));
    if (!paths.length) return;

    let minX = Infinity, minY = Infinity, maxX = -Infinity, maxY = -Infinity;

    paths.forEach(p => {
      try {
        const b = p.getBBox();
        minX = Math.min(minX, b.x);
        minY = Math.min(minY, b.y);
        maxX = Math.max(maxX, b.x + b.width);
        maxY = Math.max(maxY, b.y + b.height);
      } catch (_) {}
    });

    if (!isFinite(minX) || !isFinite(minY) || !isFinite(maxX) || !isFinite(maxY)) return;

    const pad = 20;
    const x = Math.max(0, minX - pad);
    const y = Math.max(0, minY - pad);
    const w = (maxX - minX) + pad * 2;
    const h = (maxY - minY) + pad * 2;

    const root = svgDoc.documentElement;
    root.setAttribute('preserveAspectRatio', 'xMidYMid meet');
    root.setAttribute('viewBox', toViewBoxStr({ x, y, w, h }));

    initialViewBox = { x, y, w, h };
    currentViewBox = { x, y, w, h };
  }

  function applyViewBox(svgDoc) {
    if (!currentViewBox) return;
    svgDoc.documentElement.setAttribute('viewBox', toViewBoxStr(currentViewBox));
  }

  function zoom(svgDoc, factor) {
    if (!currentViewBox || !initialViewBox) return;

    const cx = currentViewBox.x + currentViewBox.w / 2;
    const cy = currentViewBox.y + currentViewBox.h / 2;

    const newW = currentViewBox.w / factor;
    const newH = currentViewBox.h / factor;

    const minW = initialViewBox.w * 0.35;
    const maxW = initialViewBox.w * 1.25;

    const w = Math.max(minW, Math.min(maxW, newW));
    const h = (w / newW) * newH;

    currentViewBox = { x: cx - w/2, y: cy - h/2, w, h };
    applyViewBox(svgDoc);
  }

  function resetZoom(svgDoc) {
    if (!initialViewBox) return;
    currentViewBox = { ...initialViewBox };
    applyViewBox(svgDoc);
  }

  function bindZoomControls(svgDoc) {
    if (zoomInBtn) zoomInBtn.addEventListener('click', () => zoom(svgDoc, 1.18));
    if (zoomOutBtn) zoomOutBtn.addEventListener('click', () => zoom(svgDoc, 1/1.18));
    if (zoomResetBtn) zoomResetBtn.addEventListener('click', () => resetZoom(svgDoc));
  }

  function setActive(svgDoc, regionId) {
    const prev = activeId ? svgDoc.getElementById(activeId) : null;
    if (prev) prev.classList.remove(ACTIVE_CLASS);

    const el = svgDoc.getElementById(regionId);
    if (el) el.classList.add(ACTIVE_CLASS);

    activeId = regionId;
  }

  function addHover(svgDoc, regionId) {
    const el = svgDoc.getElementById(regionId);
    if (el && regionId !== activeId) el.classList.add(HOVER_CLASS);
  }
  function removeHover(svgDoc, regionId) {
    const el = svgDoc.getElementById(regionId);
    if (el) el.classList.remove(HOVER_CLASS);
  }

  async function loadData() {
    const [metaRes, namesRes, descsRes] = await Promise.all([
      fetch(META_URL,  { cache: 'no-store' }),
      fetch(NAMES_URL(getLang()), { cache: 'no-store' }),
      fetch(DESCS_URL(getLang()), { cache: 'no-store' })
    ]);

    if (!metaRes.ok)  throw new Error('META load failed: ' + metaRes.status);
    if (!namesRes.ok) throw new Error('NAMES load failed: ' + namesRes.status);
    if (!descsRes.ok) throw new Error('DESCS load failed: ' + descsRes.status);

    const metaList = await metaRes.json();
    metaById = Object.fromEntries(metaList.map(x => [x.id, x]));
    nameById = await namesRes.json();
    descById = await descsRes.json();
  }

  // Refresh only names/descs on language change
  window.addEventListener('tabi:lang-changed', async () => {
    try {
      const [namesRes, descsRes] = await Promise.all([
        fetch(NAMES_URL(getLang()), { cache: 'no-store' }),
        fetch(DESCS_URL(getLang()), { cache: 'no-store' })
      ]);
      if (namesRes.ok) nameById = await namesRes.json();
      if (descsRes.ok) descById = await descsRes.json();
      if (activeId) setCard(activeId);
    } catch (_) {}
  });

  function bindSvg(svgDoc) {
    if (!svgDoc) return;

    injectSvgStyle(svgDoc);

    // center + scale fix
    fitViewBoxToContent(svgDoc);

    // zoom controls
    bindZoomControls(svgDoc);

    const regionPaths = Array.from(svgDoc.querySelectorAll('path[id^="JP-"]'));
    regionPaths.forEach(p => {
      const id = p.getAttribute('id');
      if (!id) return;

      p.addEventListener('mouseenter', () => { addHover(svgDoc, id); setCard(id); });
      p.addEventListener('mouseleave', () => { removeHover(svgDoc, id); if (activeId) setCard(activeId); });
      p.addEventListener('click', () => { setActive(svgDoc, id); setCard(id); });
    });

    // default Tokyo
    const defaultId = 'JP-13';
    if (svgDoc.getElementById(defaultId)) {
      setActive(svgDoc, defaultId);
      setCard(defaultId);
    }
  }

  try {
    await loadData();

    objectEl.addEventListener('load', () => bindSvg(objectEl.contentDocument));
    if (objectEl.contentDocument) bindSvg(objectEl.contentDocument);

  } catch (e) {
    console.error(e);
    if (titleEl) titleEl.textContent = 'Map data error';
    if (descEl) descEl.textContent = '지도 데이터를 불러오지 못했습니다.';
  }
})();