/**
 * GoCrawl Academic Harvester — Frontend Application (Sidebar Single-Purpose UI)
 * Strict adherence to:
 * - NO EMOJIS (100% handcrafted SVG icons)
 * - Single-purpose pages (Mulai Crawl, Live Monitor, Data Artikel, Kelola Jurnal, Statistik)
 * - Left sidebar with mobile drawer support
 */

// Custom Handcrafted SVG Icon Set
const ICONS = {
  logo: `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
    <circle cx="12" cy="12" r="3"></circle>
    <path d="M12 3v6m0 6v6"></path>
    <path d="M3 12h6m6 0h6"></path>
    <circle cx="12" cy="3" r="1.5"></circle>
    <circle cx="12" cy="21" r="1.5"></circle>
    <circle cx="3" cy="12" r="1.5"></circle>
    <circle cx="21" cy="12" r="1.5"></circle>
  </svg>`,
  terminal: `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
    <polyline points="4 17 10 11 4 5"></polyline>
    <line x1="12" y1="19" x2="20" y2="19"></line>
  </svg>`,
  database: `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
    <ellipse cx="12" cy="5" rx="9" ry="3"></ellipse>
    <path d="M21 12c0 1.66-4 3-9 3s-9-1.34-9-3"></path>
    <path d="M3 5v14c0 1.66 4 3 9 3s9-1.34 9-3V5"></path>
  </svg>`,
  'book-open': `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
    <path d="M2 3h6a4 4 0 0 1 4 4v14a3 3 0 0 0-3-3H2z"></path>
    <path d="M22 3h-6a4 4 0 0 0-4 4v14a3 3 0 0 1 3-3h7z"></path>
  </svg>`,
  'shield-check': `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
    <path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z"></path>
    <polyline points="9 12 11 14 15 10"></polyline>
  </svg>`,
  'file-text': `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
    <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"></path>
    <polyline points="14 2 14 8 20 8"></polyline>
    <line x1="16" y1="13" x2="8" y2="13"></line>
    <line x1="16" y1="17" x2="8" y2="17"></line>
    <polyline points="10 9 9 9 8 9"></polyline>
  </svg>`,
  cpu: `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
    <rect x="4" y="4" width="16" height="16" rx="2"></rect>
    <rect x="9" y="9" width="6" height="6"></rect>
    <line x1="9" y1="1" x2="9" y2="4"></line>
    <line x1="15" y1="1" x2="15" y2="4"></line>
    <line x1="9" y1="20" x2="9" y2="23"></line>
    <line x1="15" y1="20" x2="15" y2="23"></line>
    <line x1="20" y1="9" x2="23" y2="9"></line>
    <line x1="20" y1="15" x2="23" y2="15"></line>
    <line x1="1" y1="9" x2="4" y2="9"></line>
    <line x1="1" y1="15" x2="4" y2="15"></line>
  </svg>`,
  search: `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
    <circle cx="11" cy="11" r="8"></circle>
    <line x1="21" y1="21" x2="16.65" y2="16.65"></line>
  </svg>`,
  calendar: `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
    <rect x="3" y="4" width="18" height="18" rx="2" ry="2"></rect>
    <line x1="16" y1="2" x2="16" y2="6"></line>
    <line x1="8" y1="2" x2="8" y2="6"></line>
    <line x1="3" y1="10" x2="21" y2="10"></line>
  </svg>`,
  play: `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
    <polygon points="5 3 19 12 5 21 5 3"></polygon>
  </svg>`,
  stop: `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
    <rect x="5" y="5" width="14" height="14" rx="2"></rect>
  </svg>`,
  trash: `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
    <polyline points="3 6 5 6 21 6"></polyline>
    <path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"></path>
  </svg>`,
  copy: `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
    <rect x="9" y="9" width="13" height="13" rx="2" ry="2"></rect>
    <path d="M5 15H4a2 2 0 0 1-2-2V4a2 2 0 0 1 2-2h9a2 2 0 0 1 2 2v1"></path>
  </svg>`,
  download: `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
    <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"></path>
    <polyline points="7 10 12 15 17 10"></polyline>
    <line x1="12" y1="15" x2="12" y2="3"></line>
  </svg>`,
  refresh: `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
    <polyline points="23 4 23 10 17 10"></polyline>
    <polyline points="1 20 1 14 7 14"></polyline>
    <path d="M3.51 9a9 9 0 0 1 14.85-3.36L23 10M1 14l4.64 4.36A9 9 0 0 0 20.49 15"></path>
  </svg>`,
  plus: `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
    <line x1="12" y1="5" x2="12" y2="19"></line>
    <line x1="5" y1="12" x2="19" y2="12"></line>
  </svg>`,
  x: `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
    <line x1="18" y1="6" x2="6" y2="18"></line>
    <line x1="6" y1="6" x2="18" y2="18"></line>
  </svg>`,
  menu: `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
    <line x1="3" y1="12" x2="21" y2="12"></line>
    <line x1="3" y1="6" x2="21" y2="6"></line>
    <line x1="3" y1="18" x2="21" y2="18"></line>
  </svg>`,
  'chevron-left': `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
    <polyline points="15 18 9 12 15 6"></polyline>
  </svg>`,
  'chevron-right': `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
    <polyline points="9 18 15 12 9 6"></polyline>
  </svg>`,
  'check-circle': `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
    <path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"></path>
    <polyline points="22 4 12 14.01 9 11.01"></polyline>
  </svg>`,
  'alert-circle': `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
    <circle cx="12" cy="12" r="10"></circle>
    <line x1="12" y1="8" x2="12" y2="12"></line>
    <line x1="12" y1="16" x2="12.01" y2="16"></line>
  </svg>`,
  'external-link': `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
    <path d="M18 13v6a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V8a2 2 0 0 1 2-2h6"></path>
    <polyline points="15 3 21 3 21 9"></polyline>
    <line x1="10" y1="14" x2="21" y2="3"></line>
  </svg>`,
  info: `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
    <circle cx="12" cy="12" r="10"></circle>
    <line x1="12" y1="16" x2="12" y2="12"></line>
    <line x1="12" y1="8" x2="12.01" y2="8"></line>
  </svg>`,
  quote: `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
    <path d="M3 21c3 0 7-1 7-8V5c0-1.25-.756-2.017-2-2H4c-1.25 0-2 .75-2 1.972V11c0 1.25.75 2 2 2 1 0 1 0 1 1v1c0 1-1 2-2 2s-1 .008-1 1.031V20c0 1 0 1 1 1z"></path>
    <path d="M15 21c3 0 7-1 7-8V5c0-1.25-.757-2.017-2-2h-4c-1.25 0-2 .75-2 1.972V11c0 1.25.75 2 2 2 1 0 1 0 1 1v1c0 1-1 2-2 2s-1 .008-1 1.031V20c0 1 0 1 1 1z"></path>
  </svg>`,
  tag: `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
    <path d="M20.59 13.41l-7.17 7.17a2 2 0 0 1-2.83 0L2 12V2h10l8.59 8.59a2 2 0 0 1 0 2.82z"></path>
    <line x1="7" y1="7" x2="7.01" y2="7"></line>
  </svg>`,
  globe: `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
    <circle cx="12" cy="12" r="10"></circle>
    <line x1="2" y1="12" x2="22" y2="12"></line>
    <path d="M12 2a15.3 15.3 0 0 1 4 10 15.3 15.3 0 0 1-4 10 15.3 15.3 0 0 1-4-10 15.3 15.3 0 0 1 4-10z"></path>
  </svg>`,
  sun: `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
    <circle cx="12" cy="12" r="5"></circle>
    <line x1="12" y1="1" x2="12" y2="3"></line>
    <line x1="12" y1="21" x2="12" y2="23"></line>
    <line x1="4.22" y1="4.22" x2="5.64" y2="5.64"></line>
    <line x1="18.36" y1="18.36" x2="19.78" y2="19.78"></line>
    <line x1="1" y1="12" x2="3" y2="12"></line>
    <line x1="21" y1="12" x2="23" y2="12"></line>
    <line x1="4.22" y1="19.78" x2="5.64" y2="18.36"></line>
    <line x1="18.36" y1="5.64" x2="19.78" y2="4.22"></line>
  </svg>`,
  moon: `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
    <path d="M21 12.79A9 9 0 1 1 11.21 3 7 7 0 0 0 21 12.79z"></path>
  </svg>`,
};

function injectIcons() {
  const sidebarLogo = document.getElementById('sidebar-logo');
  if (sidebarLogo) sidebarLogo.innerHTML = ICONS.logo;

  const mobileLogo = document.getElementById('mobile-logo');
  if (mobileLogo) mobileLogo.innerHTML = ICONS.logo;

  document.querySelectorAll('[data-icon]').forEach(el => {
    const iconName = el.getAttribute('data-icon');
    if (ICONS[iconName]) {
      el.innerHTML = ICONS[iconName];
    }
  });
}

// Global App State
const state = {
  theme: localStorage.getItem('gocrawl_theme') || 'light',
  activeView: 'view-crawl',
  crawler: {
    running: false,
    keyword: '',
    fromYear: null,
    toYear: null,
    pagesFetched: 0,
    articlesFound: 0,
    currentJournal: '',
  },
  logs: [],
  logFilter: 'all',
  articles: [],
  totalArticles: 0,
  articlesPage: 1,
  articlesLimit: 15,
  articlesTotalPages: 1,
  journals: [],
  selectedArticle: null,
  citationFormat: 'apa',
};

document.addEventListener('DOMContentLoaded', () => {
  initTheme();
  injectIcons();
  setupSidebarNavigation();
  setupCrawlerForm();
  setupLiveStream();
  setupConsoleControls();
  setupArticleExplorer();
  setupJournalManager();
  setupModal();
  loadInitialData();
});

// Theme Management
function initTheme() {
  document.documentElement.setAttribute('data-theme', state.theme);
  updateThemeUI();

  const themeBtn = document.getElementById('theme-toggle');
  themeBtn.addEventListener('click', () => {
    state.theme = state.theme === 'light' ? 'dark' : 'light';
    document.documentElement.setAttribute('data-theme', state.theme);
    localStorage.setItem('gocrawl_theme', state.theme);
    updateThemeUI();
  });
}

function updateThemeUI() {
  const slot = document.getElementById('theme-icon-slot');
  const label = document.getElementById('theme-label');
  if (slot) slot.innerHTML = state.theme === 'light' ? ICONS.moon : ICONS.sun;
  if (label) label.textContent = state.theme === 'light' ? 'Mode Gelap' : 'Mode Terang';
}

// Sidebar Navigation & Mobile Drawer
function setupSidebarNavigation() {
  const navItems = document.querySelectorAll('.nav-item');
  const sidebar = document.getElementById('app-sidebar');
  const backdrop = document.getElementById('sidebar-backdrop');
  const btnToggle = document.getElementById('btn-toggle-sidebar');
  const btnClose = document.getElementById('btn-close-sidebar');

  navItems.forEach(btn => {
    btn.addEventListener('click', () => {
      const targetView = btn.getAttribute('data-view');
      switchView(targetView);
      closeMobileSidebar();
    });
  });

  if (btnToggle) {
    btnToggle.addEventListener('click', () => {
      sidebar.classList.add('open');
      backdrop.classList.add('active');
    });
  }

  if (btnClose) {
    btnClose.addEventListener('click', closeMobileSidebar);
  }

  if (backdrop) {
    backdrop.addEventListener('click', closeMobileSidebar);
  }
}

function closeMobileSidebar() {
  const sidebar = document.getElementById('app-sidebar');
  const backdrop = document.getElementById('sidebar-backdrop');
  if (sidebar) sidebar.classList.remove('open');
  if (backdrop) backdrop.classList.remove('active');
}

function switchView(viewId) {
  state.activeView = viewId;

  // Update nav item active states
  document.querySelectorAll('.nav-item').forEach(item => {
    item.classList.toggle('active', item.getAttribute('data-view') === viewId);
  });

  // Update view containers
  document.querySelectorAll('.page-view').forEach(view => {
    view.classList.toggle('active', view.id === viewId);
  });

  // Lazy refresh based on view purpose
  if (viewId === 'view-articles') {
    fetchArticles();
  } else if (viewId === 'view-journals') {
    fetchJournals();
  } else if (viewId === 'view-stats') {
    fetchStatus();
  }
}

// Initial Data Load
async function loadInitialData() {
  await Promise.all([
    fetchStatus(),
    fetchJournals(),
    fetchArticles(),
  ]);
}

// Status & KPIs
async function fetchStatus() {
  try {
    const res = await fetch('/api/stats');
    if (!res.ok) return;
    const data = await res.json();

    document.getElementById('kpi-total-articles').textContent = data.totalArticles.toLocaleString('id-ID');
    document.getElementById('nav-count-articles').textContent = data.totalArticles;

    document.getElementById('kpi-total-journals').textContent = data.totalJournalsConfigured;
    document.getElementById('nav-count-journals').textContent = data.totalJournalsConfigured;
    document.getElementById('kpi-journals-sub').textContent = `${data.journalsWithData} jurnal memiliki data`;

    document.getElementById('kpi-doi-percentage').textContent = `${data.doiPercentage}%`;
    document.getElementById('kpi-doi-count').textContent = `${data.doiCount} dari ${data.totalArticles} terindeks DOI`;

    // Populate Year Filter
    const yearSelect = document.getElementById('filter-year');
    const existingYears = Object.keys(data.byYear).sort((a, b) => b.localeCompare(a));
    const currentVal = yearSelect.value;
    yearSelect.innerHTML = '<option value="">Semua Tahun</option>';
    for (const yr of existingYears) {
      if (yr && yr !== 'Tidak Teridentifikasi') {
        const opt = document.createElement('option');
        opt.value = yr;
        opt.textContent = `${yr} (${data.byYear[yr]})`;
        yearSelect.appendChild(opt);
      }
    }
    yearSelect.value = currentVal;
  } catch (err) {
    console.error('Failed fetching stats:', err);
  }
}

// Crawler Form & Starting
function setupCrawlerForm() {
  const form = document.getElementById('crawl-form');
  const btnStart = document.getElementById('btn-start-crawl');
  const btnStop = document.getElementById('btn-stop-crawl');

  // Presets
  document.querySelectorAll('.preset-tag').forEach(tag => {
    tag.addEventListener('click', () => {
      const input = document.getElementById('keyword-input');
      input.value = tag.getAttribute('data-preset');
      input.focus();
    });
  });

  // Submit Start
  form.addEventListener('submit', async (e) => {
    e.preventDefault();
    const keyword = document.getElementById('keyword-input').value.trim();
    const fromYear = document.getElementById('from-year').value.trim();
    const toYear = document.getElementById('to-year').value.trim();

    if (!keyword) {
      showToast('Harap masukkan kata kunci pencarian.', 'error');
      return;
    }

    btnStart.disabled = true;
    try {
      const res = await fetch('/api/crawl/start', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ keyword, fromYear, toYear }),
      });
      const data = await res.json();
      if (!res.ok) throw new Error(data.error || 'Gagal memulai perayapan.');

      showToast('Perayapan berhasil dimulai! Beralih ke Live Monitor...', 'success');
      updateCrawlerStateUI({ running: true, keyword, fromYear, toYear });
      
      // Auto-switch to Live Monitor so user sees immediate feedback
      setTimeout(() => {
        switchView('view-monitor');
      }, 400);
    } catch (err) {
      showToast(err.message, 'error');
      btnStart.disabled = false;
    }
  });

  // Stop button (in Monitor view)
  btnStop.addEventListener('click', async () => {
    btnStop.disabled = true;
    try {
      const res = await fetch('/api/crawl/stop', { method: 'POST' });
      const data = await res.json();
      if (!res.ok) throw new Error(data.error || 'Gagal menghentikan.');
      showToast('Perayapan dihentikan pengguna.', 'info');
    } catch (err) {
      showToast(err.message, 'error');
    } finally {
      btnStop.disabled = false;
    }
  });
}

function updateCrawlerStateUI(stateData) {
  const isRunning = !!stateData.running;
  
  // Sidebar indicators
  const sidebarDot = document.getElementById('sidebar-status-dot');
  const sidebarText = document.getElementById('sidebar-status-text');
  const monitorPillDot = document.getElementById('monitor-pill-dot');
  const mobileDot = document.getElementById('mobile-status-dot');

  sidebarDot.className = 'status-indicator-dot ' + (isRunning ? 'running' : 'idle');
  sidebarText.textContent = isRunning ? 'Merayap...' : 'Siap';
  mobileDot.className = 'mobile-status-dot ' + (isRunning ? 'running' : 'idle');
  monitorPillDot.className = 'nav-pill-dot ' + (isRunning ? 'running' : '');

  // Monitor View indicators
  const monitorStatus = document.getElementById('monitor-status-text');
  const btnStart = document.getElementById('btn-start-crawl');
  const btnStop = document.getElementById('btn-stop-crawl');
  const progressBar = document.getElementById('crawl-progress-bar');

  if (monitorStatus) {
    monitorStatus.textContent = isRunning ? 'MERAYAP' : 'SIAP';
    monitorStatus.style.color = isRunning ? 'var(--warning)' : 'var(--accent)';
  }

  if (btnStart) btnStart.disabled = isRunning;
  if (btnStop) btnStop.disabled = !isRunning;

  if (progressBar) {
    if (isRunning) progressBar.classList.add('active');
    else progressBar.classList.remove('active');
  }

  if (stateData.currentJournal) {
    document.getElementById('monitor-journal-text').textContent = stateData.currentJournal;
  }
  if (stateData.pagesFetched !== undefined) {
    document.getElementById('monitor-pages-text').textContent = stateData.pagesFetched;
  }
  if (stateData.articlesFound !== undefined) {
    document.getElementById('monitor-matches-text').textContent = stateData.articlesFound;
  }
}

// Server-Sent Events (SSE) Live Monitor Streaming
function setupLiveStream() {
  const sse = new EventSource('/api/crawl/events');

  sse.addEventListener('init', (e) => {
    try {
      const data = JSON.parse(e.data);
      if (data.state) updateCrawlerStateUI(data.state);
      if (Array.isArray(data.logs)) {
        for (const log of data.logs) {
          addLogLine(log, false);
        }
        scrollConsoleBottom();
      }
    } catch (err) {
      console.error('SSE init error:', err);
    }
  });

  sse.addEventListener('log', (e) => {
    try {
      const entry = JSON.parse(e.data);
      addLogLine(entry, true);
    } catch (err) {
      console.error('SSE log error:', err);
    }
  });

  sse.addEventListener('state', (e) => {
    try {
      const crawlState = JSON.parse(e.data);
      updateCrawlerStateUI(crawlState);
    } catch (err) {
      console.error('SSE state error:', err);
    }
  });

  sse.addEventListener('done', (e) => {
    try {
      const payload = JSON.parse(e.data);
      showToast(`Proses berakhir dengan kode status ${payload.exitCode}`, payload.exitCode === 0 ? 'success' : 'error');
      fetchStatus();
      fetchArticles();
      fetchJournals();
    } catch (err) {
      console.error('SSE done error:', err);
    }
  });
}

function addLogLine(entry, autoScroll = true) {
  state.logs.push(entry);
  const container = document.getElementById('console-lines');
  const empty = document.getElementById('console-empty');
  if (empty) empty.style.display = 'none';

  const lineEl = document.createElement('div');
  lineEl.className = `console-line log-line-${entry.type}`;
  lineEl.setAttribute('data-type', entry.type);

  if (state.logFilter !== 'all' && state.logFilter !== entry.type) {
    lineEl.style.display = 'none';
  }

  const timeStr = entry.timestamp ? new Date(entry.timestamp).toLocaleTimeString() : '';
  lineEl.innerHTML = `
    <span class="log-time">[${timeStr}]</span>
    <span class="log-tag">${formatLogTag(entry.type)}</span>
    <span class="log-text">${escapeHtml(entry.message)}</span>
  `;

  container.appendChild(lineEl);

  if (autoScroll) {
    scrollConsoleBottom();
  }
}

function formatLogTag(type) {
  switch (type) {
    case 'start': return 'START';
    case 'match': return 'COCOK';
    case 'progress': return 'PROGRES';
    case 'success': return 'SELESAI';
    case 'error': return 'GAGAL';
    default: return 'INFO';
  }
}

function scrollConsoleBottom() {
  const screen = document.getElementById('console-screen');
  if (screen) {
    screen.scrollTop = screen.scrollHeight;
  }
}

function setupConsoleControls() {
  document.getElementById('btn-clear-console').addEventListener('click', () => {
    document.getElementById('console-lines').innerHTML = '';
    const empty = document.getElementById('console-empty');
    if (empty) empty.style.display = 'flex';
    state.logs = [];
  });

  document.getElementById('btn-copy-console').addEventListener('click', () => {
    if (!state.logs.length) {
      showToast('Tidak ada log untuk disalin.', 'info');
      return;
    }
    const text = state.logs.map(l => `[${l.timestamp}] [${l.type.toUpperCase()}] ${l.message}`).join('\n');
    navigator.clipboard.writeText(text).then(() => {
      showToast('Seluruh log berhasil disalin!', 'success');
    });
  });

  document.querySelectorAll('.filter-pill').forEach(pill => {
    pill.addEventListener('click', () => {
      document.querySelectorAll('.filter-pill').forEach(p => p.classList.remove('active'));
      pill.classList.add('active');
      state.logFilter = pill.getAttribute('data-filter');

      document.querySelectorAll('.console-line').forEach(line => {
        const type = line.getAttribute('data-type');
        if (state.logFilter === 'all' || state.logFilter === type) {
          line.style.display = 'flex';
        } else {
          line.style.display = 'none';
        }
      });
      scrollConsoleBottom();
    });
  });
}

// Articles Explorer
function setupArticleExplorer() {
  const searchInput = document.getElementById('article-search-input');
  const clearBtn = document.getElementById('btn-clear-article-search');
  const journalSelect = document.getElementById('filter-journal');
  const yearSelect = document.getElementById('filter-year');
  const sortSelect = document.getElementById('filter-sort');
  const hasDoiCheck = document.getElementById('filter-has-doi');
  const btnRefresh = document.getElementById('btn-refresh-articles');
  const btnPrev = document.getElementById('btn-pag-prev');
  const btnNext = document.getElementById('btn-pag-next');

  let debounceTimer;
  searchInput.addEventListener('input', () => {
    clearBtn.style.display = searchInput.value ? 'block' : 'none';
    clearTimeout(debounceTimer);
    debounceTimer = setTimeout(() => {
      state.articlesPage = 1;
      fetchArticles();
    }, 280);
  });

  clearBtn.addEventListener('click', () => {
    searchInput.value = '';
    clearBtn.style.display = 'none';
    state.articlesPage = 1;
    fetchArticles();
  });

  journalSelect.addEventListener('change', () => {
    state.articlesPage = 1;
    fetchArticles();
  });

  yearSelect.addEventListener('change', () => {
    state.articlesPage = 1;
    fetchArticles();
  });

  sortSelect.addEventListener('change', () => {
    state.articlesPage = 1;
    fetchArticles();
  });

  hasDoiCheck.addEventListener('change', () => {
    state.articlesPage = 1;
    fetchArticles();
  });

  btnRefresh.addEventListener('click', () => {
    fetchArticles();
    fetchStatus();
    showToast('Data artikel dimuat ulang.', 'info');
  });

  btnPrev.addEventListener('click', () => {
    if (state.articlesPage > 1) {
      state.articlesPage--;
      fetchArticles();
    }
  });

  btnNext.addEventListener('click', () => {
    if (state.articlesPage < state.articlesTotalPages) {
      state.articlesPage++;
      fetchArticles();
    }
  });

  document.getElementById('btn-export-csv').addEventListener('click', () => {
    window.location.href = '/api/export/csv';
  });
  document.getElementById('btn-export-json').addEventListener('click', () => {
    window.location.href = '/api/export/json';
  });
}

async function fetchArticles() {
  const search = document.getElementById('article-search-input').value.trim();
  const journal = document.getElementById('filter-journal').value;
  const year = document.getElementById('filter-year').value;
  const sort = document.getElementById('filter-sort').value;
  const hasDoi = document.getElementById('filter-has-doi').checked;

  const params = new URLSearchParams({
    page: state.articlesPage,
    limit: state.articlesLimit,
    sort,
  });
  if (search) params.append('q', search);
  if (journal) params.append('journal', journal);
  if (year) {
    params.append('fromYear', year);
    params.append('toYear', year);
  }
  if (hasDoi) params.append('hasDoi', 'true');

  try {
    const res = await fetch(`/api/articles?${params.toString()}`);
    if (!res.ok) return;
    const data = await res.json();

    state.articles = data.articles || [];
    state.totalArticles = data.total || 0;
    state.articlesTotalPages = data.totalPages || 1;

    renderArticles();
  } catch (err) {
    console.error('Failed fetching articles:', err);
  }
}

function renderArticles() {
  const tbody = document.getElementById('articles-tbody');
  const cardsContainer = document.getElementById('articles-cards');
  const empty = document.getElementById('articles-empty');
  const countEl = document.getElementById('articles-result-count');
  const pagCurrent = document.getElementById('pag-current');
  const pagTotal = document.getElementById('pag-total');
  const btnPrev = document.getElementById('btn-pag-prev');
  const btnNext = document.getElementById('btn-pag-next');

  countEl.textContent = state.totalArticles.toLocaleString('id-ID');
  pagCurrent.textContent = state.articlesPage;
  pagTotal.textContent = state.articlesTotalPages;
  btnPrev.disabled = state.articlesPage <= 1;
  btnNext.disabled = state.articlesPage >= state.articlesTotalPages;

  if (state.articles.length === 0) {
    tbody.innerHTML = '';
    cardsContainer.innerHTML = '';
    empty.style.display = 'flex';
    return;
  }

  empty.style.display = 'none';

  // Desktop Table
  tbody.innerHTML = state.articles.map((a, idx) => `
    <tr>
      <td><span class="badge">${escapeHtml(a.year || '—')}</span></td>
      <td>
        <div class="article-title-text" data-article-idx="${idx}">${escapeHtml(a.title)}</div>
        <div class="article-authors-text">${escapeHtml(a.authors || 'Penulis Tidak Dicantumkan')}</div>
      </td>
      <td>
        <span class="badge badge-subtle">${escapeHtml(a.journal || a.journalFolder || 'OJS Journal')}</span>
      </td>
      <td>
        ${a.doi ? `
          <a href="https://doi.org/${encodeURIComponent(a.doi)}" target="_blank" rel="noopener noreferrer" class="article-doi-link">
            <span>${escapeHtml(a.doi)}</span>
            <span class="icon-slot">${ICONS['external-link']}</span>
          </a>
        ` : '<span class="text-muted">—</span>'}
      </td>
      <td style="text-align: center;">
        <button type="button" class="btn btn-secondary btn-sm btn-inspect-article" data-article-idx="${idx}" title="Detail Lengkap & Sitasi">
          <span class="icon-slot">${ICONS['book-open']}</span>
        </button>
      </td>
    </tr>
  `).join('');

  // Mobile Cards
  cardsContainer.innerHTML = state.articles.map((a, idx) => `
    <div class="article-mobile-card" data-article-idx="${idx}">
      <div class="card-top-meta">
        <span class="badge">${escapeHtml(a.year || '—')}</span>
        <span class="badge badge-subtle">${escapeHtml(a.journalFolder || 'OJS')}</span>
      </div>
      <h3 class="card-mobile-title">${escapeHtml(a.title)}</h3>
      <div class="card-mobile-authors">${escapeHtml(a.authors || 'Penulis Tidak Dicantumkan')}</div>
      ${a.doi ? `
        <div>
          <a href="https://doi.org/${encodeURIComponent(a.doi)}" target="_blank" rel="noopener noreferrer" class="article-doi-link">
            <span>DOI: ${escapeHtml(a.doi)}</span>
            <span class="icon-slot">${ICONS['external-link']}</span>
          </a>
        </div>
      ` : ''}
      <button type="button" class="btn btn-secondary btn-sm" style="margin-top:0.25rem;">
        <span class="icon-slot">${ICONS['book-open']}</span>
        <span>Lihat Abstrak & Sitasi</span>
      </button>
    </div>
  `).join('');

  const openModalHandler = (e) => {
    const target = e.target.closest('[data-article-idx]');
    if (!target) return;
    const idx = parseInt(target.getAttribute('data-article-idx'), 10);
    const article = state.articles[idx];
    if (article) openArticleModal(article);
  };

  tbody.querySelectorAll('.article-title-text, .btn-inspect-article').forEach(el => {
    el.addEventListener('click', openModalHandler);
  });
  cardsContainer.querySelectorAll('.article-mobile-card').forEach(el => {
    el.addEventListener('click', openModalHandler);
  });
}

// Target Journals Manager
function setupJournalManager() {
  const form = document.getElementById('add-journal-form');
  form.addEventListener('submit', async (e) => {
    e.preventDefault();
    const urlInput = document.getElementById('new-journal-url');
    const url = urlInput.value.trim();
    if (!url) return;

    try {
      const res = await fetch('/api/journals', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ url }),
      });
      const data = await res.json();
      if (!res.ok) throw new Error(data.error || 'Gagal menambahkan target.');

      urlInput.value = '';
      showToast('Jurnal sasaran berhasil ditambahkan!', 'success');
      await fetchJournals();
      await fetchStatus();
    } catch (err) {
      showToast(err.message, 'error');
    }
  });
}

async function fetchJournals() {
  try {
    const res = await fetch('/api/journals');
    if (!res.ok) return;
    state.journals = await res.json();
    renderJournalList();
    renderJournalSelect();
    renderTargetSummary();
  } catch (err) {
    console.error('Failed fetching journals:', err);
  }
}

function renderJournalList() {
  const container = document.getElementById('journal-list');
  const countBadge = document.getElementById('journal-count-badge');
  countBadge.textContent = `${state.journals.length} Jurnal`;

  if (state.journals.length === 0) {
    container.innerHTML = `
      <div class="empty-state" style="padding: 2rem;">
        <span class="icon-slot">${ICONS['book-open']}</span>
        <h3 class="empty-title">Belum Ada Target Jurnal</h3>
        <p class="empty-desc">Tambahkan URL portal OJS melalui form di samping.</p>
      </div>
    `;
    return;
  }

  container.innerHTML = state.journals.map((j) => `
    <div class="journal-list-item">
      <div class="journal-info">
        <span class="journal-name">${escapeHtml(j.name)}</span>
        <a href="${escapeHtml(j.url)}" target="_blank" rel="noopener noreferrer" class="journal-url">
          ${escapeHtml(j.url)}
        </a>
        <div class="journal-badge-row">
          <span class="badge ${j.articleCount > 0 ? 'badge-accent' : 'badge-subtle'}">
            ${j.articleCount} Artikel Terambil
          </span>
        </div>
      </div>
      <button type="button" class="btn-icon-sm btn-delete-journal" data-url="${escapeHtml(j.url)}" title="Hapus jurnal">
        <span class="icon-slot">${ICONS.trash}</span>
      </button>
    </div>
  `).join('');

  container.querySelectorAll('.btn-delete-journal').forEach(btn => {
    btn.addEventListener('click', async () => {
      const targetUrl = btn.getAttribute('data-url');
      if (!confirm(`Hapus jurnal ${targetUrl} dari journal.txt?`)) return;

      try {
        const res = await fetch('/api/journals', {
          method: 'DELETE',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ url: targetUrl }),
        });
        if (!res.ok) throw new Error('Gagal menghapus.');
        showToast('Jurnal sasaran berhasil dihapus.', 'info');
        await fetchJournals();
        await fetchStatus();
      } catch (err) {
        showToast(err.message, 'error');
      }
    });
  });
}

function renderJournalSelect() {
  const select = document.getElementById('filter-journal');
  const current = select.value;
  select.innerHTML = '<option value="">Semua Jurnal</option>';
  for (const j of state.journals) {
    const opt = document.createElement('option');
    opt.value = j.name;
    opt.textContent = `${j.name} (${j.articleCount})`;
    select.appendChild(opt);
  }
  select.value = current;
}

function renderTargetSummary() {
  const summaryBox = document.getElementById('active-targets-summary');
  if (!state.journals.length) {
    summaryBox.textContent = 'Tidak ada jurnal terkonfigurasi. Tambahkan jurnal terlebih dahulu.';
    return;
  }
  const names = state.journals.map(j => j.name).join(', ');
  summaryBox.textContent = `${state.journals.length} target aktif: ${names}`;
}

// Modal & Citation Generator
function setupModal() {
  const modal = document.getElementById('article-modal');
  const btnClose = document.getElementById('btn-close-modal');
  const btnCopyCite = document.getElementById('btn-copy-citation');

  btnClose.addEventListener('click', () => closeModal());

  modal.addEventListener('click', (e) => {
    if (e.target === modal) closeModal();
  });

  document.addEventListener('keydown', (e) => {
    if (e.key === 'Escape' && modal.classList.contains('open')) closeModal();
  });

  document.querySelectorAll('.cite-tab').forEach(tab => {
    tab.addEventListener('click', () => {
      document.querySelectorAll('.cite-tab').forEach(t => t.classList.remove('active'));
      tab.classList.add('active');
      state.citationFormat = tab.getAttribute('data-format');
      updateCitationDisplay();
    });
  });

  btnCopyCite.addEventListener('click', () => {
    const code = document.getElementById('citation-display-text').textContent;
    navigator.clipboard.writeText(code).then(() => {
      showToast('Sitasi berhasil disalin ke clipboard!', 'success');
    });
  });
}

function openArticleModal(article) {
  state.selectedArticle = article;
  const modal = document.getElementById('article-modal');

  document.getElementById('modal-year-badge').textContent = article.year || 'Tahun —';
  document.getElementById('modal-journal-badge').textContent = article.journal || article.journalFolder || 'OJS Journal';
  document.getElementById('modal-article-title').textContent = article.title;
  document.getElementById('modal-authors').textContent = article.authors || 'Tidak Disebutkan';
  document.getElementById('modal-published').textContent = article.published || article.year || '—';
  document.getElementById('modal-doi').textContent = article.doi || 'Tidak Ada';

  const linkOrig = document.getElementById('modal-link-original');
  if (article.url) {
    linkOrig.href = article.url;
    linkOrig.style.display = 'inline-flex';
  } else {
    linkOrig.style.display = 'none';
  }

  const linkDoi = document.getElementById('modal-link-doi');
  if (article.doi) {
    linkDoi.href = `https://doi.org/${encodeURIComponent(article.doi)}`;
    linkDoi.style.display = 'inline-flex';
  } else {
    linkDoi.style.display = 'none';
  }

  document.getElementById('modal-abstract-text').textContent = article.abstract || 'Abstrak tidak disediakan dalam metadata halaman OJS ini.';

  // Render Kata Kunci (Keywords)
  const kwContainer = document.getElementById('modal-keywords-list');
  let kwStr = article.keywords || '';
  if (!kwStr && article.abstract) {
    const m = article.abstract.match(/(?:kata\s*kunci|keywords?)\s*:\s*([^.\n]+)/i);
    if (m) kwStr = m[1].replace(/[."]+$/, '').trim();
  }

  if (kwContainer) {
    if (kwStr) {
      const tokens = kwStr.split(/[,;]+/).map(s => s.trim()).filter(Boolean);
      if (tokens.length > 0) {
        kwContainer.innerHTML = tokens.map(tok => `
          <span class="keyword-pill">
            <span class="icon-heading" style="display:inline-flex; align-items:center;">${ICONS.tag}</span>
            <span>${escapeHtml(tok)}</span>
          </span>
        `).join('');
      } else {
        kwContainer.innerHTML = `<span class="text-muted" style="font-size:0.75rem;">Tidak tercantum dalam metadata artikel.</span>`;
      }
    } else {
      kwContainer.innerHTML = `<span class="text-muted" style="font-size:0.75rem;">Tidak tercantum dalam metadata artikel.</span>`;
    }
  }

  updateCitationDisplay();

  modal.classList.add('open');
  modal.setAttribute('aria-hidden', 'false');
  document.body.style.overflow = 'hidden';
}

function closeModal() {
  const modal = document.getElementById('article-modal');
  modal.classList.remove('open');
  modal.setAttribute('aria-hidden', 'true');
  document.body.style.overflow = '';
}

function updateCitationDisplay() {
  const article = state.selectedArticle;
  if (!article) return;

  const pre = document.getElementById('citation-display-text');
  const format = state.citationFormat;

  if (format === 'apa') {
    const authors = article.authors ? article.authors.replace(/;/g, ',') : 'Anonim';
    const year = article.year ? `(${article.year}).` : '(n.d.).';
    const title = article.title ? `${article.title}.` : '';
    const journal = article.journal ? `${article.journal}.` : '';
    const link = article.doi ? `https://doi.org/${article.doi}` : (article.url || '');
    pre.textContent = `${authors} ${year} ${title} ${journal} ${link}`.trim();
  } else if (format === 'bibtex') {
    const key = (article.authors ? article.authors.split(/[\s,;]+/)[0] : 'Article') + (article.year || '2026');
    pre.textContent = `@article{${key},
  title = {${article.title || ''}},
  author = {${article.authors || ''}},
  journal = {${article.journal || article.journalFolder || ''}},
  year = {${article.year || ''}},
  doi = {${article.doi || ''}},
  url = {${article.url || ''}}
}`;
  } else if (format === 'ris') {
    pre.textContent = `TY  - JOUR
TI  - ${article.title || ''}
AU  - ${article.authors ? article.authors.replace(/;/g, '\nAU  - ') : ''}
JO  - ${article.journal || ''}
PY  - ${article.year || ''}
DO  - ${article.doi || ''}
UR  - ${article.url || ''}
ER  -`;
  }
}

// Toast
function showToast(message, type = 'info') {
  const container = document.getElementById('toast-container');
  const toast = document.createElement('div');
  toast.className = `toast toast-${type}`;

  let iconSvg = ICONS.info;
  if (type === 'success') iconSvg = ICONS['check-circle'];
  if (type === 'error') iconSvg = ICONS['alert-circle'];

  toast.innerHTML = `
    <span class="icon-slot">${iconSvg}</span>
    <span>${escapeHtml(message)}</span>
  `;

  container.appendChild(toast);

  setTimeout(() => {
    toast.style.opacity = '0';
    toast.style.transform = 'translateY(8px)';
    toast.style.transition = 'all 0.2s ease';
    setTimeout(() => toast.remove(), 200);
  }, 3200);
}

function escapeHtml(str) {
  if (str === null || str === undefined) return '';
  return String(str)
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#39;');
}
