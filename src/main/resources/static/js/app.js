(function () {
  // Since this file is served by the same Spring Boot app (from /static),
  // relative paths hit the same origin - no CORS/base-URL config needed.
  // If you ever host the frontend separately, change this to the full
  // backend URL, e.g. 'https://your-api-domain.com'.
  const API_BASE = '';

  const TOKEN_KEY = 'grocery_jwt';
  const USER_KEY = 'grocery_username';

  let items = []; // items for the currently selected date
  let currentDate = null;

  const loginView = document.getElementById('login-view');
  const appView = document.getElementById('app-view');
  const loginError = document.getElementById('login-error');
  const itemsError = document.getElementById('items-error');
  const sendError = document.getElementById('send-error');
  const toastEl = document.getElementById('toast');
  const itemsContainer = document.getElementById('items-container');
  const itemsLoading = document.getElementById('items-loading');
  const loginBtn = document.getElementById('login-btn');

  function showToast(msg) {
    toastEl.textContent = msg;
    toastEl.classList.add('show');
    setTimeout(() => toastEl.classList.remove('show'), 2500);
  }

  function todayStr() {
    const d = new Date();
    const off = d.getTimezoneOffset();
    const local = new Date(d.getTime() - off * 60000);
    return local.toISOString().slice(0, 10);
  }

  function escapeHtml(str) {
    const div = document.createElement('div');
    div.textContent = str == null ? '' : str;
    return div.innerHTML;
  }

  // ---------- Auth helpers ----------
  function getToken() {
    return localStorage.getItem(TOKEN_KEY);
  }

  function getUsername() {
    return localStorage.getItem(USER_KEY);
  }

  function setSession(token, username) {
    localStorage.setItem(TOKEN_KEY, token);
    localStorage.setItem(USER_KEY, username);
  }

  function clearSession() {
    localStorage.removeItem(TOKEN_KEY);
    localStorage.removeItem(USER_KEY);
  }

  // Wrapper around fetch that attaches the JWT and handles 401 by logging out.
  async function apiFetch(path, options = {}) {
    const token = getToken();
    const headers = Object.assign(
      { 'Content-Type': 'application/json' },
      options.headers || {},
      token ? { Authorization: 'Bearer ' + token } : {}
    );

    const response = await fetch(API_BASE + path, Object.assign({}, options, { headers }));

    if (response.status === 401) {
      clearSession();
      showLogin();
      showToast('Session expired - please sign in again');
      throw new Error('Unauthorized');
    }

    if (!response.ok) {
      let message = 'Request failed (' + response.status + ')';
      try {
        const body = await response.json();
        if (body && body.error) message = body.error;
      } catch (e) { /* ignore parse errors */ }
      throw new Error(message);
    }

    if (response.status === 204) return null;
    return response.json();
  }

  function showLogin() {
    appView.style.display = 'none';
    loginView.style.display = 'block';
  }

  function showApp(username) {
    loginView.style.display = 'none';
    appView.style.display = 'block';
    document.getElementById('current-user').textContent = username;
    const dateInput = document.getElementById('list-date');
    dateInput.value = todayStr();
    currentDate = dateInput.value;
    loadItems();
    loadTemplates();
    loadSchedule();
    document.getElementById('history-list').style.display = 'none';
    document.getElementById('toggle-history-btn').textContent = 'show';
  }

  // ---------- Login ----------
  loginBtn.addEventListener('click', doLogin);
  document.getElementById('login-pass').addEventListener('keydown', e => {
    if (e.key === 'Enter') doLogin();
  });

  async function doLogin() {
    const username = document.getElementById('login-user').value.trim();
    const password = document.getElementById('login-pass').value;

    if (!username || !password) {
      loginError.textContent = 'Enter both username and password.';
      return;
    }

    loginError.textContent = '';
    loginBtn.disabled = true;
    loginBtn.textContent = 'SIGNING IN...';

    try {
      const response = await fetch(API_BASE + '/api/auth/login', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ username, password })
      });

      if (!response.ok) {
        throw new Error(response.status === 401 ? 'Incorrect username or password.' : 'Login failed.');
      }

      const data = await response.json();
      setSession(data.token, data.username);
      showApp(data.username);
    } catch (err) {
      loginError.textContent = err.message || 'Could not sign in - is the backend running?';
    } finally {
      loginBtn.disabled = false;
      loginBtn.textContent = 'SIGN IN';
    }
  }

  document.getElementById('logout-btn').addEventListener('click', () => {
    clearSession();
    items = [];
    document.getElementById('login-user').value = '';
    document.getElementById('login-pass').value = '';
    showLogin();
  });

  document.getElementById('list-date').addEventListener('change', (e) => {
    currentDate = e.target.value;
    loadItems();
  });

  // ---------- Items ----------
  async function loadItems() {
    itemsLoading.style.display = 'block';
    itemsError.textContent = '';
    itemsContainer.innerHTML = '';
    try {
      items = await apiFetch('/api/groceries?date=' + encodeURIComponent(currentDate));
    } catch (err) {
      items = [];
      if (err.message !== 'Unauthorized') {
        itemsError.textContent = err.message || 'Could not load items.';
      }
    }
    itemsLoading.style.display = 'none';
    renderItems();
  }

  function renderItems() {
    itemsContainer.innerHTML = '';
    if (items.length === 0) {
      itemsContainer.innerHTML = '<div class="empty-state">No items yet. Add your first one below.</div>';
    } else {
      items.forEach(item => {
        const row = document.createElement('div');
        row.className = 'item-row' + (item.checked ? ' checked' : '');
        row.innerHTML = `
          <div class="checkbox" data-id="${item.id}">${item.checked ? '&#10003;' : ''}</div>
          <div class="item-name">${escapeHtml(item.name)}</div>
          <div class="item-qty">${escapeHtml(item.qty || '')}</div>
          <button class="remove-btn" data-id="${item.id}">&times;</button>
        `;
        itemsContainer.appendChild(row);
      });
    }
    document.getElementById('item-count').textContent = items.length;
    document.getElementById('checked-count').textContent = items.filter(i => i.checked).length;
  }

  itemsContainer.addEventListener('click', async (e) => {
    const cb = e.target.closest('.checkbox');
    const rm = e.target.closest('.remove-btn');

    if (cb) {
      const id = cb.dataset.id;
      const item = items.find(i => String(i.id) === id);
      if (!item) return;
      const newChecked = !item.checked;
      try {
        const updated = await apiFetch('/api/groceries/' + id, {
          method: 'PUT',
          body: JSON.stringify({ checked: newChecked })
        });
        item.checked = updated.checked;
        renderItems();
      } catch (err) {
        if (err.message !== 'Unauthorized') showToast(err.message || 'Could not update item');
      }
    } else if (rm) {
      const id = rm.dataset.id;
      try {
        await apiFetch('/api/groceries/' + id, { method: 'DELETE' });
        items = items.filter(i => String(i.id) !== id);
        renderItems();
      } catch (err) {
        if (err.message !== 'Unauthorized') showToast(err.message || 'Could not remove item');
      }
    }
  });

  const addItemBtn = document.getElementById('add-item-btn');
  addItemBtn.addEventListener('click', addItem);
  document.getElementById('new-item-qty').addEventListener('keydown', e => { if (e.key === 'Enter') addItem(); });
  document.getElementById('new-item-name').addEventListener('keydown', e => { if (e.key === 'Enter') addItem(); });

  async function addItem() {
    const nameInput = document.getElementById('new-item-name');
    const qtyInput = document.getElementById('new-item-qty');
    const name = nameInput.value.trim();
    const qty = qtyInput.value.trim();
    if (!name) { nameInput.focus(); return; }

    addItemBtn.disabled = true;
    try {
      const created = await apiFetch('/api/groceries', {
        method: 'POST',
        body: JSON.stringify({ date: currentDate, name, qty })
      });
      items.push(created);
      nameInput.value = '';
      qtyInput.value = '';
      nameInput.focus();
      renderItems();
    } catch (err) {
      if (err.message !== 'Unauthorized') showToast(err.message || 'Could not add item');
    } finally {
      addItemBtn.disabled = false;
    }
  }

  // ---------- Send to WhatsApp ----------
  const sendBtn = document.getElementById('send-btn');
  sendBtn.addEventListener('click', async () => {
    sendError.textContent = '';
    const numberRaw = document.getElementById('whatsapp-number').value.trim();
    const toNumber = numberRaw.replace(/[^0-9]/g, '');

    if (!toNumber || toNumber.length < 8) {
      sendError.textContent = 'Enter a valid number with country code (e.g. 919876543210).';
      return;
    }
    if (items.length === 0) {
      sendError.textContent = 'Add at least one item before sending.';
      return;
    }

    sendBtn.disabled = true;
    sendBtn.textContent = 'SENDING...';

    try {
      const result = await apiFetch('/api/whatsapp/send', {
        method: 'POST',
        body: JSON.stringify({ date: currentDate, toNumber })
      });

      if (result.sentDirectly) {
        showToast('Sent directly via WhatsApp!');
      } else {
        showToast(result.message || 'Opening WhatsApp...');
        if (result.waLink) {
          window.open(result.waLink, '_blank');
        }
      }
    } catch (err) {
      if (err.message !== 'Unauthorized') {
        sendError.textContent = err.message || 'Could not send the list.';
      }
    } finally {
      sendBtn.disabled = false;
      sendBtn.textContent = 'SEND LIST \u25B6';
    }
  });

  // ---------- Send by Gmail ----------
  const sendEmailBtn = document.getElementById('send-email-btn');
  sendEmailBtn.addEventListener('click', async () => {
    const emailError = document.getElementById('email-error');
    emailError.textContent = '';
    const toEmail = document.getElementById('email-address').value.trim();

    if (!toEmail || !toEmail.includes('@')) {
      emailError.textContent = 'Enter a valid email address.';
      return;
    }
    if (items.length === 0) {
      emailError.textContent = 'Add at least one item before sending.';
      return;
    }

    sendEmailBtn.disabled = true;
    sendEmailBtn.textContent = 'SENDING...';

    try {
      const result = await apiFetch('/api/email/send', {
        method: 'POST',
        body: JSON.stringify({ date: currentDate, toEmail })
      });
      showToast(result.message || (result.sent ? 'Email sent!' : 'Could not send email'));
    } catch (err) {
      if (err.message !== 'Unauthorized') {
        emailError.textContent = err.message || 'Could not send the email.';
      }
    } finally {
      sendEmailBtn.disabled = false;
      sendEmailBtn.textContent = 'EMAIL LIST \u25B6';
    }
  });

  // ---------- Usual items (templates) ----------
  let templates = [];
  const templatesChips = document.getElementById('templates-chips');

  async function loadTemplates() {
    try {
      templates = await apiFetch('/api/templates');
    } catch (err) {
      templates = [];
    }
    renderTemplates();
  }

  function renderTemplates() {
    templatesChips.innerHTML = '';
    if (templates.length === 0) {
      templatesChips.innerHTML = '<div class="empty-state">No usual items yet - add ones you buy every time below.</div>';
      return;
    }
    templates.forEach(t => {
      const chip = document.createElement('div');
      chip.className = 'chip';
      chip.innerHTML = `
        <span>${escapeHtml(t.name)}${t.qty ? ' (' + escapeHtml(t.qty) + ')' : ''}</span>
        <button class="chip-add" data-id="${t.id}" title="Add to today's list">+</button>
        <button class="chip-remove" data-id="${t.id}" title="Remove from usual items">&times;</button>
      `;
      templatesChips.appendChild(chip);
    });
  }

  templatesChips.addEventListener('click', async (e) => {
    const addBtn = e.target.closest('.chip-add');
    const removeBtn = e.target.closest('.chip-remove');

    if (addBtn) {
      const id = addBtn.dataset.id;
      try {
        const created = await apiFetch('/api/templates/' + id + '/add?date=' + encodeURIComponent(currentDate), {
          method: 'POST'
        });
        items.push(created);
        renderItems();
        showToast('Added to list');
      } catch (err) {
        if (err.message !== 'Unauthorized') showToast(err.message || 'Could not add item');
      }
    } else if (removeBtn) {
      const id = removeBtn.dataset.id;
      try {
        await apiFetch('/api/templates/' + id, { method: 'DELETE' });
        templates = templates.filter(t => String(t.id) !== id);
        renderTemplates();
      } catch (err) {
        if (err.message !== 'Unauthorized') showToast(err.message || 'Could not remove usual item');
      }
    }
  });

  document.getElementById('add-template-btn').addEventListener('click', addTemplate);
  document.getElementById('new-template-name').addEventListener('keydown', e => { if (e.key === 'Enter') addTemplate(); });
  document.getElementById('new-template-qty').addEventListener('keydown', e => { if (e.key === 'Enter') addTemplate(); });

  async function addTemplate() {
    const nameInput = document.getElementById('new-template-name');
    const qtyInput = document.getElementById('new-template-qty');
    const name = nameInput.value.trim();
    const qty = qtyInput.value.trim();
    if (!name) { nameInput.focus(); return; }

    try {
      const created = await apiFetch('/api/templates', {
        method: 'POST',
        body: JSON.stringify({ name, qty })
      });
      templates.push(created);
      nameInput.value = '';
      qtyInput.value = '';
      renderTemplates();
    } catch (err) {
      if (err.message !== 'Unauthorized') showToast(err.message || 'Could not add usual item');
    }
  }

  document.getElementById('add-all-templates-btn').addEventListener('click', async () => {
    if (templates.length === 0) { showToast('No usual items to add'); return; }
    try {
      const created = await apiFetch('/api/templates/add-all?date=' + encodeURIComponent(currentDate), {
        method: 'POST'
      });
      items = items.concat(created);
      renderItems();
      showToast('Added ' + created.length + ' usual items');
    } catch (err) {
      if (err.message !== 'Unauthorized') showToast(err.message || 'Could not add usual items');
    }
  });

  // ---------- Auto-send schedule ----------
  const dayOrder = ['MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY', 'SUNDAY'];
  const scheduleDaySelect = document.getElementById('schedule-day');
  dayOrder.forEach(d => {
    const opt = document.createElement('option');
    opt.value = d;
    opt.textContent = d.charAt(0) + d.slice(1).toLowerCase();
    scheduleDaySelect.appendChild(opt);
  });

  async function loadSchedule() {
    try {
      const schedule = await apiFetch('/api/whatsapp/schedule');
      if (schedule) {
        document.getElementById('schedule-enabled').checked = schedule.enabled;
        scheduleDaySelect.value = schedule.dayOfWeek || 'MONDAY';
        document.getElementById('schedule-hour').value = schedule.hour;
        document.getElementById('schedule-minute').value = schedule.minute;
        document.getElementById('schedule-channel').value = schedule.channel || 'WHATSAPP';
        document.getElementById('schedule-number').value = schedule.toNumber || '';
      }
    } catch (err) {
      // no schedule saved yet - leave fields at defaults
    }
  }

  document.getElementById('save-schedule-btn').addEventListener('click', async () => {
    const scheduleError = document.getElementById('schedule-error');
    scheduleError.textContent = '';

    const enabled = document.getElementById('schedule-enabled').checked;
    const hour = parseInt(document.getElementById('schedule-hour').value, 10);
    const minute = parseInt(document.getElementById('schedule-minute').value, 10);
    const channel = document.getElementById('schedule-channel').value;
    const rawRecipient = document.getElementById('schedule-number').value.trim();
    const toNumber = channel === 'EMAIL' ? rawRecipient : rawRecipient.replace(/[^0-9]/g, '');

    if (isNaN(hour) || hour < 0 || hour > 23 || isNaN(minute) || minute < 0 || minute > 59) {
      scheduleError.textContent = 'Enter a valid hour (0-23) and minute (0-59).';
      return;
    }
    if (channel === 'EMAIL' ? !toNumber.includes('@') : toNumber.length < 8) {
      scheduleError.textContent = channel === 'EMAIL'
        ? 'Enter a valid email address.'
        : 'Enter a valid number with country code.';
      return;
    }

    try {
      await apiFetch('/api/whatsapp/schedule', {
        method: 'PUT',
        body: JSON.stringify({ enabled, dayOfWeek: scheduleDaySelect.value, hour, minute, channel, toNumber })
      });
      showToast(enabled ? 'Auto-send saved' : 'Auto-send saved (off)');
    } catch (err) {
      if (err.message !== 'Unauthorized') scheduleError.textContent = err.message || 'Could not save schedule';
    }
  });

  // ---------- History ----------
  const historyList = document.getElementById('history-list');
  const toggleHistoryBtn = document.getElementById('toggle-history-btn');
  let historyLoaded = false;

  toggleHistoryBtn.addEventListener('click', async () => {
    const isHidden = historyList.style.display === 'none';
    if (isHidden) {
      if (!historyLoaded) {
        await loadHistory();
        historyLoaded = true;
      }
      historyList.style.display = 'block';
      toggleHistoryBtn.textContent = 'hide';
    } else {
      historyList.style.display = 'none';
      toggleHistoryBtn.textContent = 'show';
    }
  });

  async function loadHistory() {
    historyList.innerHTML = '<div class="loading">loading history...</div>';
    try {
      const history = await apiFetch('/api/whatsapp/history');
      renderHistory(history);
    } catch (err) {
      historyList.innerHTML = '<div class="error-msg">Could not load history.</div>';
    }
  }

  function renderHistory(history) {
    if (!history || history.length === 0) {
      historyList.innerHTML = '<div class="empty-state">Nothing sent yet.</div>';
      return;
    }
    historyList.innerHTML = history.map(h => {
      const when = new Date(h.sentAt).toLocaleString();
      const isEmail = h.channel === 'EMAIL';
      const channelTag = isEmail ? 'GMAIL' : 'WHATSAPP';
      const statusTag = h.sentDirectly ? 'SENT' : (isEmail ? 'FAILED' : 'LINK OPENED');
      const sourceTag = h.automatic ? 'AUTO' : 'MANUAL';
      return `
        <div class="history-row">
          <div class="history-meta">
            <span>${escapeHtml(h.date)} &middot; ${escapeHtml(when)}</span>
            <span><span class="history-tag">${channelTag}</span> <span class="history-tag">${statusTag}</span> <span class="history-tag">${sourceTag}</span></span>
          </div>
          <div>To ${escapeHtml(h.toNumber)}</div>
        </div>
      `;
    }).join('');
  }

  // ---------- Boot ----------
  const existingToken = getToken();
  const existingUser = getUsername();
  if (existingToken && existingUser) {
    // Optimistically show the app; apiFetch will bounce back to login if the token is expired/invalid.
    showApp(existingUser);
  } else {
    showLogin();
  }
})();
