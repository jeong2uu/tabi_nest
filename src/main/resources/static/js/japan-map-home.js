(async function () {
  // ✅ 이제 object가 아니라 "컨테이너 div"를 잡습니다.
  const containerEl = document.getElementById('japanMapObject');
  const panel = document.getElementById('regionPanel');
  if (!containerEl || !panel) return;

  const imgEl = document.getElementById('regionImage');
  const titleEl = document.getElementById('regionTitle');
  const descEl = document.getElementById('regionDesc');
  const ctaEl = document.getElementById('regionCta');

  const zoomInBtn = document.getElementById('zoomInBtn');
  const zoomOutBtn = document.getElementById('zoomOutBtn');
  const zoomResetBtn = document.getElementById('zoomResetBtn');

  const SVG_URL   = '/map/japanHigh.svg';
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

  function getLang() {
    return (window.TabiI18n && window.TabiI18n.lang)
      ? window.TabiI18n.lang
      : (document.documentElement.lang || 'ko').slice(0,2);
  }

  function setCard(regionId) {
    const meta = metaById[regionId];
    const name = nameById[regionId] || regionId;
    const desc = descById[regionId] || '';

    if (imgEl && meta && meta.image) imgEl.src = meta.image;
    if (titleEl) titleEl.textContent = name;
    if (descEl) descEl.textContent = desc || '—';
    if (ctaEl) ctaEl.href = '/accommodations?region=' + encodeURIComponent(regionId);
  }

  function injectSvgStyle(svgRoot) {
    // 이미 style이 있으면 중복 삽입 방지
    if (svgRoot.querySelector('style[data-tabinest="map-style"]')) return;

    const style = document.createElementNS('http://www.w3.org/2000/svg', 'style');
    style.setAttribute('data-tabinest', 'map-style');
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
    svgRoot.appendChild(style);
  }

  function toViewBoxStr(v) { return `${v.x} ${v.y} ${v.w} ${v.h}`; }

  // ✅ SVG 내부 element id lookup (document.getElementById는 전체 문서 기준이라 충돌 가능)
  function getSvgElById(svgRoot, id) {
    try {
      const esc = (window.CSS && CSS.escape) ? CSS.escape(id) : id.replace(/"/g, '\\"');
      return svgRoot.querySelector(`#${esc}`);
    } catch (_) {
      return svgRoot.querySelector(`[id="${id}"]`);
    }
  }

  // Fit viewBox to content bbox (remove huge margins)
  function fitViewBoxToContent(svgRoot) {
    const paths = Array.from(svgRoot.querySelectorAll('path[id^="JP-"]'));
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

    svgRoot.setAttribute('preserveAspectRatio', 'xMidYMid meet');
    svgRoot.setAttribute('viewBox', toViewBoxStr({ x, y, w, h }));

    // inline 방식에서 width/height 고정값이 있으면 레이아웃이 깨질 수 있어 제거
    svgRoot.removeAttribute('width');
    svgRoot.removeAttribute('height');
    svgRoot.style.width = '100%';
    svgRoot.style.height = '100%';
    svgRoot.style.display = 'block';

    initialViewBox = { x, y, w, h };
    currentViewBox = { x, y, w, h };
  }

  function applyViewBox(svgRoot) {
    if (!currentViewBox) return;
    svgRoot.setAttribute('viewBox', toViewBoxStr(currentViewBox));
  }

  function zoom(svgRoot, factor) {
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
    applyViewBox(svgRoot);
  }

  function resetZoom(svgRoot) {
    if (!initialViewBox) return;
    currentViewBox = { ...initialViewBox };
    applyViewBox(svgRoot);
  }

  function bindZoomControls(svgRoot) {
    // 중복 바인딩 방지: 버튼에 data 속성으로 표시
    if (zoomInBtn && zoomInBtn.dataset.bound !== '1') {
      zoomInBtn.dataset.bound = '1';
      zoomInBtn.addEventListener('click', () => zoom(svgRoot, 1.18));
    }
    if (zoomOutBtn && zoomOutBtn.dataset.bound !== '1') {
      zoomOutBtn.dataset.bound = '1';
      zoomOutBtn.addEventListener('click', () => zoom(svgRoot, 1/1.18));
    }
    if (zoomResetBtn && zoomResetBtn.dataset.bound !== '1') {
      zoomResetBtn.dataset.bound = '1';
      zoomResetBtn.addEventListener('click', () => resetZoom(svgRoot));
    }
  }

  function setActive(svgRoot, regionId) {
    const prev = activeId ? getSvgElById(svgRoot, activeId) : null;
    if (prev) prev.classList.remove(ACTIVE_CLASS);

    const el = getSvgElById(svgRoot, regionId);
    if (el) el.classList.add(ACTIVE_CLASS);

    activeId = regionId;
  }

  function addHover(svgRoot, regionId) {
    const el = getSvgElById(svgRoot, regionId);
    if (el && regionId !== activeId) el.classList.add(HOVER_CLASS);
  }
  function removeHover(svgRoot, regionId) {
    const el = getSvgElById(svgRoot, regionId);
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

  async function loadInlineSvg() {
    const res = await fetch(SVG_URL, { cache: 'no-store' });
    if (!res.ok) throw new Error('SVG load failed: ' + res.status);

    const text = await res.text();

    // 컨테이너에 inline 삽입 (class는 기존 그대로 japan-map-object 유지)
    containerEl.innerHTML = text;

    const svgRoot = containerEl.querySelector('svg');
    if (!svgRoot) throw new Error('SVG root <svg> not found in response');

    return svgRoot;
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

  function bindSvg(svgRoot) {
    if (!svgRoot) return;

    injectSvgStyle(svgRoot);

    // center + scale fix
    fitViewBoxToContent(svgRoot);

    // zoom controls
    bindZoomControls(svgRoot);

    const regionPaths = Array.from(svgRoot.querySelectorAll('path[id^="JP-"]'));
    if (!regionPaths.length) {
      console.warn('[japan-map] No path[id^="JP-"] found in SVG');
    }

    regionPaths.forEach(p => {
      const id = p.getAttribute('id');
      if (!id) return;

      p.addEventListener('mouseenter', () => { addHover(svgRoot, id); setCard(id); });
      p.addEventListener('mouseleave', () => { removeHover(svgRoot, id); if (activeId) setCard(activeId); });
      p.addEventListener('click', () => { setActive(svgRoot, id); setCard(id); });
    });

    // default Tokyo
    const defaultId = 'JP-13';
    if (getSvgElById(svgRoot, defaultId)) {
      setActive(svgRoot, defaultId);
      setCard(defaultId);
    } else if (regionPaths[0]) {
      // 혹시 JP-13이 없다면 첫 번째 지역을 기본 선택
      const firstId = regionPaths[0].getAttribute('id');
      if (firstId) {
        setActive(svgRoot, firstId);
        setCard(firstId);
      }
    }
  }

  try {
    await loadData();
    const svgRoot = await loadInlineSvg();
    bindSvg(svgRoot);

  } catch (e) {
    console.error(e);
    if (titleEl) titleEl.textContent = 'Map data error';
    if (descEl) descEl.textContent = '지도 데이터를 불러오지 못했습니다.';
  }
})();
