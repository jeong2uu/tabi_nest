// Login / Signup modals (Bootstrap 5) + session keep-alive checks
(function () {
  const $ = (sel) => document.querySelector(sel);

  const loginBtn = $('#navLoginBtn');
  const signupBtn = $('#navSignupBtn');
  const userBox = $('#navUserBox');
  const userNameEl = $('#navUserName');
  const logoutBtn = $('#navLogoutBtn');

  const loginForm = $('#loginForm');
  const signupForm = $('#signupForm');
  const loginError = $('#loginError');
  const signupError = $('#signupError');

  function show(el) { if (el) el.hidden = false; }
  function hide(el) { if (el) el.hidden = true; }
  function setText(el, t) { if (el) el.textContent = t; }

  function setAuthedUI(user) {
    // if user exists -> hide login/signup, show user box
    if (user) {
      if (loginBtn) loginBtn.classList.add('is-hidden');
      if (signupBtn) signupBtn.classList.add('is-hidden');
      if (userBox) userBox.classList.remove('is-hidden');
      setText(userNameEl, user.name || user.email);
    } else {
      if (loginBtn) loginBtn.classList.remove('is-hidden');
      if (signupBtn) signupBtn.classList.remove('is-hidden');
      if (userBox) userBox.classList.add('is-hidden');
      setText(userNameEl, '');
    }
  }

  async function api(url, method, body) {
    const res = await fetch(url, {
      method,
      headers: { 'Content-Type': 'application/json' },
      credentials: 'same-origin',
      body: body ? JSON.stringify(body) : undefined
    });
    const ct = res.headers.get('content-type') || '';
    const data = ct.includes('application/json') ? await res.json() : null;
    return { ok: res.ok, status: res.status, data };
  }

  async function refreshMe() {
    const r = await api('/api/auth/me', 'GET');
    if (r.ok) setAuthedUI(r.data);
    else setAuthedUI(null);
  }

  // --- Login ---
  if (loginForm) {
    loginForm.addEventListener('submit', async (e) => {
      e.preventDefault();
      hide(loginError);

      const email = $('#loginEmail')?.value?.trim();
      const password = $('#loginPassword')?.value;

      const r = await api('/api/auth/login', 'POST', { email, password });
      if (!r.ok) {
        setText(loginError, (r.data && r.data.message) ? r.data.message : 'Login failed');
        show(loginError);
        return;
      }

      setAuthedUI(r.data);

      // close modal
      const modalEl = $('#loginModal');
      if (modalEl && window.bootstrap) {
        const inst = window.bootstrap.Modal.getInstance(modalEl) || new window.bootstrap.Modal(modalEl);
        inst.hide();
      }
    });
  }

  // --- Signup ---
  if (signupForm) {
    signupForm.addEventListener('submit', async (e) => {
      e.preventDefault();
      hide(signupError);

      const name = $('#signupName')?.value?.trim();
      const email = $('#signupEmail')?.value?.trim();
      const phone = $('#signupPhone')?.value?.trim();
      const password = $('#signupPassword')?.value;

      const r = await api('/api/auth/signup', 'POST', { name, email, phone, password });
      if (!r.ok) {
        setText(signupError, (r.data && r.data.message) ? r.data.message : 'Signup failed');
        show(signupError);
        return;
      }

      setAuthedUI(r.data);

      const modalEl = $('#signupModal');
      if (modalEl && window.bootstrap) {
        const inst = window.bootstrap.Modal.getInstance(modalEl) || new window.bootstrap.Modal(modalEl);
        inst.hide();
      }
    });
  }

  // --- Logout ---
  if (logoutBtn) {
    logoutBtn.addEventListener('click', async (e) => {
      e.preventDefault();
      await api('/api/auth/logout', 'POST');
      setAuthedUI(null);
    });
  }

  // --- 30-minute session checks ---
  // Server timeout is configured to 30m. On client, we ping regularly so UI can react
  // when the session expires (e.g., show Login button again).
  const CHECK_EVERY_MS = 60_000; // 1 minute
  setInterval(async () => {
    const r = await api('/api/auth/ping', 'GET');
    if (!r.ok) setAuthedUI(null);
  }, CHECK_EVERY_MS);

  // initial
  document.addEventListener('DOMContentLoaded', refreshMe);
})();
