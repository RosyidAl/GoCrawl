import http from 'node:http';
import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';
import { spawn } from 'node:child_process';

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);
const ROOT_DIR = path.resolve(__dirname, '..');
const PORT = process.env.PORT || 3000;
const OUTPUT_DIR = path.join(ROOT_DIR, 'output');
const JOURNAL_FILE = path.join(ROOT_DIR, 'journal.txt');
const JAR_PATH = path.join(ROOT_DIR, 'gocrawl.jar');

// SSE clients and crawl process state
const sseClients = new Set();
let activeProcess = null;
let activeCrawlState = {
  running: false,
  keyword: '',
  fromYear: null,
  toYear: null,
  startedAt: null,
  pagesFetched: 0,
  articlesFound: 0,
  currentJournal: '',
  exitCode: null,
  error: null,
};
const logBuffer = [];
const MAX_LOG_BUFFER = 500;

function appendLog(type, message) {
  const entry = {
    id: Date.now() + '-' + Math.random().toString(36).substring(2, 7),
    timestamp: new Date().toISOString(),
    type, // 'info' | 'progress' | 'match' | 'success' | 'error' | 'start'
    message: message.trimEnd(),
  };
  logBuffer.push(entry);
  if (logBuffer.length > MAX_LOG_BUFFER) {
    logBuffer.shift();
  }
  broadcastSse('log', entry);
}

function broadcastSse(event, data) {
  const payload = `event: ${event}\ndata: ${JSON.stringify(data)}\n\n`;
  for (const client of sseClients) {
    try {
      client.write(payload);
    } catch {
      sseClients.delete(client);
    }
  }
}

// Helper to read targets from journal.txt
async function getJournalTargets() {
  if (!fs.existsSync(JOURNAL_FILE)) {
    return [];
  }
  const content = await fs.promises.readFile(JOURNAL_FILE, 'utf-8');
  const lines = content.split(/\r?\n/).map(l => l.trim()).filter(l => l && !l.startsWith('#'));
  
  // Also count articles in output
  return lines.map((url, idx) => {
    const name = deriveJournalName(url);
    const jsonPath = path.join(OUTPUT_DIR, name, 'articles.json');
    let articleCount = 0;
    if (fs.existsSync(jsonPath)) {
      try {
        const raw = fs.readFileSync(jsonPath, 'utf-8');
        const parsed = JSON.parse(raw);
        articleCount = Array.isArray(parsed) ? parsed.length : 0;
      } catch {
        // ignore parse error
      }
    }
    return { id: idx + 1, url, name, articleCount };
  });
}

function deriveJournalName(urlStr) {
  try {
    const u = new URL(urlStr);
    const parts = (u.pathname || '').split('/').filter(Boolean);
    let name = '';
    for (let i = 0; i < parts.length; i++) {
      if (parts[i].toLowerCase() === 'index.php' && i + 1 < parts.length) {
        name = parts[i + 1];
        break;
      }
    }
    if (!name) {
      for (let i = parts.length - 1; i >= 0; i--) {
        if (parts[i]) {
          name = parts[i];
          break;
        }
      }
    }
    if (name.toLowerCase() === 'index') name = '';
    if (!name) name = u.hostname.split('.')[0];
    return name.replace(/[^A-Za-z0-9_-]/g, '_');
  } catch {
    return 'unknown_journal';
  }
}

// Load all crawled articles
async function loadAllArticles() {
  if (!fs.existsSync(OUTPUT_DIR)) {
    return [];
  }
  const entries = await fs.promises.readdir(OUTPUT_DIR, { withFileTypes: true });
  const allArticles = [];

  for (const entry of entries) {
    if (entry.isDirectory()) {
      const jsonFile = path.join(OUTPUT_DIR, entry.name, 'articles.json');
      if (fs.existsSync(jsonFile)) {
        try {
          const raw = await fs.promises.readFile(jsonFile, 'utf-8');
          const list = JSON.parse(raw);
          if (Array.isArray(list)) {
            for (const item of list) {
              let keywords = item.keywords || '';
              if (!keywords && item.abstract) {
                const match = item.abstract.match(/(?:kata\s*kunci|keywords?)\s*:\s*([^.\n]+)/i);
                if (match) {
                  keywords = match[1].replace(/[."]+$/, '').trim();
                }
              }
              allArticles.push({
                ...item,
                keywords,
                journalFolder: entry.name,
              });
            }
          }
        } catch (err) {
          console.error(`Failed parsing ${jsonFile}:`, err.message);
        }
      }
    }
  }
  return allArticles;
}

// MIME types dictionary
const MIME_TYPES = {
  '.html': 'text/html; charset=utf-8',
  '.css': 'text/css; charset=utf-8',
  '.js': 'application/javascript; charset=utf-8',
  '.mjs': 'application/javascript; charset=utf-8',
  '.json': 'application/json; charset=utf-8',
  '.svg': 'image/svg+xml',
  '.png': 'image/png',
  '.ico': 'image/x-icon',
};

// Start Crawler Process
function startCrawl({ keyword, fromYear, toYear, customTargets }) {
  if (activeProcess && activeCrawlState.running) {
    throw new Error('Crawl job is already currently running.');
  }

  if (!fs.existsSync(JAR_PATH)) {
    throw new Error(`gocrawl.jar not found at ${JAR_PATH}. Please compile first.`);
  }

  if (!keyword || !keyword.trim()) {
    throw new Error('Keyword parameter is required.');
  }

  // If custom targets are specified, update journal.txt
  if (Array.isArray(customTargets) && customTargets.length > 0) {
    fs.writeFileSync(JOURNAL_FILE, customTargets.map(t => t.trim()).filter(Boolean).join('\n') + '\n', 'utf-8');
  }

  const args = [
    '-Djavax.net.ssl.trustStoreType=Windows-ROOT',
    '-jar',
    JAR_PATH,
    '--keyword',
    keyword.trim(),
  ];

  if (fromYear) args.push('--from-year', String(fromYear));
  if (toYear) args.push('--to-year', String(toYear));
  args.push('--output', 'output');

  activeCrawlState = {
    running: true,
    keyword: keyword.trim(),
    fromYear: fromYear || null,
    toYear: toYear || null,
    startedAt: new Date().toISOString(),
    pagesFetched: 0,
    articlesFound: 0,
    currentJournal: '',
    exitCode: null,
    error: null,
  };

  appendLog('start', `Memulai crawl: keyword="${keyword.trim()}" ${fromYear ? `from=${fromYear} ` : ''}${toYear ? `to=${toYear}` : ''}`);
  broadcastSse('state', activeCrawlState);

  try {
    activeProcess = spawn('java', args, {
      cwd: ROOT_DIR,
      stdio: ['ignore', 'pipe', 'pipe'],
    });

    activeProcess.stdout.on('data', (chunk) => {
      const text = chunk.toString('utf-8');
      const lines = text.split(/\r?\n/).filter(l => l.trim());
      for (const line of lines) {
        let type = 'info';
        if (line.includes('[MULAI]')) {
          type = 'start';
          const match = line.match(/\[MULAI\]\s+([^\s—]+)/);
          if (match) {
            activeCrawlState.currentJournal = match[1];
          }
        } else if (line.includes('[COCOK]')) {
          type = 'match';
          activeCrawlState.articlesFound++;
        } else if (line.includes('[PROGRES]')) {
          type = 'progress';
          const match = line.match(/(\d+)\s+halaman/);
          if (match) {
            activeCrawlState.pagesFetched = parseInt(match[1], 10);
          }
        } else if (line.includes('[SELESAI]')) {
          type = 'success';
        }
        appendLog(type, line);
      }
      broadcastSse('state', activeCrawlState);
    });

    activeProcess.stderr.on('data', (chunk) => {
      const text = chunk.toString('utf-8');
      const lines = text.split(/\r?\n/).filter(l => l.trim());
      for (const line of lines) {
        appendLog('error', line);
      }
    });

    activeProcess.on('close', (code) => {
      activeCrawlState.running = false;
      activeCrawlState.exitCode = code;
      activeProcess = null;
      appendLog(code === 0 ? 'success' : 'error', `Proses crawler berakhir dengan kode: ${code}`);
      broadcastSse('state', activeCrawlState);
      broadcastSse('done', { exitCode: code });
    });

    activeProcess.on('error', (err) => {
      activeCrawlState.running = false;
      activeCrawlState.error = err.message;
      activeProcess = null;
      appendLog('error', `Gagal menjalankan Java crawler: ${err.message}`);
      broadcastSse('state', activeCrawlState);
    });

    return { success: true, pid: activeProcess.pid };
  } catch (err) {
    activeCrawlState.running = false;
    activeCrawlState.error = err.message;
    throw err;
  }
}

// Stop Crawler Process
function stopCrawl() {
  if (!activeProcess) {
    return { success: false, message: 'Tidak ada proses crawler yang sedang berjalan.' };
  }

  const pid = activeProcess.pid;
  try {
    if (process.platform === 'win32') {
      spawn('taskkill', ['/pid', String(pid), '/f', '/t']);
    } else {
      activeProcess.kill('SIGTERM');
    }
    appendLog('error', `Proses crawler dihentikan manual oleh pengguna (PID: ${pid}).`);
    activeCrawlState.running = false;
    broadcastSse('state', activeCrawlState);
    return { success: true, message: `Proses ${pid} berhasil dihentikan.` };
  } catch (err) {
    return { success: false, error: err.message };
  }
}

// Request dispatcher
const server = http.createServer(async (req, res) => {
  const parsedUrl = new URL(req.url, `http://${req.headers.host || 'localhost'}`);
  const pathname = parsedUrl.pathname;
  const method = req.method;

  // Global CORS headers
  res.setHeader('Access-Control-Allow-Origin', '*');
  res.setHeader('Access-Control-Allow-Methods', 'GET, POST, PUT, DELETE, OPTIONS');
  res.setHeader('Access-Control-Allow-Headers', 'Content-Type, Authorization');

  if (method === 'OPTIONS') {
    res.writeHead(204);
    res.end();
    return;
  }

  // --- API ROUTES ---

  // SSE Stream
  if (pathname === '/api/crawl/events' && method === 'GET') {
    res.writeHead(200, {
      'Content-Type': 'text/event-stream',
      'Cache-Control': 'no-cache, no-transform',
      'Connection': 'keep-alive',
      'X-Accel-Buffering': 'no',
    });
    res.write(`event: init\ndata: ${JSON.stringify({ state: activeCrawlState, logs: logBuffer.slice(-50) })}\n\n`);
    sseClients.add(res);

    const heartbeat = setInterval(() => {
      try {
        res.write(': heartbeat\n\n');
      } catch {
        clearInterval(heartbeat);
      }
    }, 15000);

    req.on('close', () => {
      clearInterval(heartbeat);
      sseClients.delete(res);
    });
    return;
  }

  // Status API
  if (pathname === '/api/status' && method === 'GET') {
    const jarExists = fs.existsSync(JAR_PATH);
    const targets = await getJournalTargets();
    const articles = await loadAllArticles();
    
    res.writeHead(200, { 'Content-Type': 'application/json' });
    res.end(JSON.stringify({
      ready: jarExists,
      activeState: activeCrawlState,
      totalTargets: targets.length,
      totalArticlesCrawled: articles.length,
      uniqueJournalsWithData: new Set(articles.map(a => a.journal || a.journalFolder)).size,
    }));
    return;
  }

  // Journal targets list
  if (pathname === '/api/journals' && method === 'GET') {
    const targets = await getJournalTargets();
    res.writeHead(200, { 'Content-Type': 'application/json' });
    res.end(JSON.stringify(targets));
    return;
  }

  // Add / replace journal targets
  if (pathname === '/api/journals' && method === 'POST') {
    try {
      const body = await parseJsonBody(req);
      if (!body.url || typeof body.url !== 'string') {
        res.writeHead(400, { 'Content-Type': 'application/json' });
        res.end(JSON.stringify({ error: 'Field "url" is required and must be a string.' }));
        return;
      }
      const rawUrl = body.url.trim();
      try {
        new URL(rawUrl);
      } catch {
        res.writeHead(400, { 'Content-Type': 'application/json' });
        res.end(JSON.stringify({ error: 'Format URL tidak valid.' }));
        return;
      }

      let current = '';
      if (fs.existsSync(JOURNAL_FILE)) {
        current = await fs.promises.readFile(JOURNAL_FILE, 'utf-8');
      }
      const lines = current.split(/\r?\n/).map(l => l.trim()).filter(Boolean);
      if (!lines.includes(rawUrl)) {
        lines.push(rawUrl);
        await fs.promises.writeFile(JOURNAL_FILE, lines.join('\n') + '\n', 'utf-8');
      }

      const updated = await getJournalTargets();
      res.writeHead(200, { 'Content-Type': 'application/json' });
      res.end(JSON.stringify({ success: true, targets: updated }));
    } catch (e) {
      res.writeHead(500, { 'Content-Type': 'application/json' });
      res.end(JSON.stringify({ error: e.message }));
    }
    return;
  }

  // Delete journal target
  if (pathname === '/api/journals' && method === 'DELETE') {
    try {
      const body = await parseJsonBody(req);
      if (!body.url) {
        res.writeHead(400, { 'Content-Type': 'application/json' });
        res.end(JSON.stringify({ error: 'URL required for removal.' }));
        return;
      }
      const targetUrl = body.url.trim();
      if (fs.existsSync(JOURNAL_FILE)) {
        const current = await fs.promises.readFile(JOURNAL_FILE, 'utf-8');
        const lines = current.split(/\r?\n/).map(l => l.trim()).filter(l => l && l !== targetUrl);
        await fs.promises.writeFile(JOURNAL_FILE, lines.join('\n') + '\n', 'utf-8');
      }
      const updated = await getJournalTargets();
      res.writeHead(200, { 'Content-Type': 'application/json' });
      res.end(JSON.stringify({ success: true, targets: updated }));
    } catch (e) {
      res.writeHead(500, { 'Content-Type': 'application/json' });
      res.end(JSON.stringify({ error: e.message }));
    }
    return;
  }

  // Start Crawl
  if (pathname === '/api/crawl/start' && method === 'POST') {
    try {
      const body = await parseJsonBody(req);
      const result = startCrawl({
        keyword: body.keyword,
        fromYear: body.fromYear ? parseInt(body.fromYear, 10) : null,
        toYear: body.toYear ? parseInt(body.toYear, 10) : null,
        customTargets: body.customTargets,
      });
      res.writeHead(200, { 'Content-Type': 'application/json' });
      res.end(JSON.stringify(result));
    } catch (e) {
      res.writeHead(400, { 'Content-Type': 'application/json' });
      res.end(JSON.stringify({ error: e.message }));
    }
    return;
  }

  // Stop Crawl
  if (pathname === '/api/crawl/stop' && method === 'POST') {
    const result = stopCrawl();
    res.writeHead(200, { 'Content-Type': 'application/json' });
    res.end(JSON.stringify(result));
    return;
  }

  // Get Articles with search and filters
  if (pathname === '/api/articles' && method === 'GET') {
    const articles = await loadAllArticles();
    const q = (parsedUrl.searchParams.get('q') || '').toLowerCase().trim();
    const journalFilter = parsedUrl.searchParams.get('journal') || '';
    const fromYear = parseInt(parsedUrl.searchParams.get('fromYear') || '0', 10);
    const toYear = parseInt(parsedUrl.searchParams.get('toYear') || '9999', 10);
    const hasDoi = parsedUrl.searchParams.get('hasDoi');
    const sort = parsedUrl.searchParams.get('sort') || 'year_desc';
    const page = Math.max(1, parseInt(parsedUrl.searchParams.get('page') || '1', 10));
    const limit = Math.max(1, Math.min(100, parseInt(parsedUrl.searchParams.get('limit') || '15', 10)));

    let filtered = articles.filter(a => {
      if (journalFilter && a.journalFolder !== journalFilter && a.journal !== journalFilter) {
        return false;
      }
      if (fromYear > 0) {
        const y = parseInt(a.year, 10);
        if (isNaN(y) || y < fromYear) return false;
      }
      if (toYear < 9999) {
        const y = parseInt(a.year, 10);
        if (isNaN(y) || y > toYear) return false;
      }
      if (hasDoi === 'true' && (!a.doi || a.doi.trim() === '')) {
        return false;
      }
      if (q) {
        const inTitle = (a.title || '').toLowerCase().includes(q);
        const inAuthors = (a.authors || '').toLowerCase().includes(q);
        const inAbstract = (a.abstract || '').toLowerCase().includes(q);
        const inDoi = (a.doi || '').toLowerCase().includes(q);
        const inKeywords = (a.keywords || '').toLowerCase().includes(q);
        if (!inTitle && !inAuthors && !inAbstract && !inDoi && !inKeywords) return false;
      }
      return true;
    });

    // Sorting
    filtered.sort((a, b) => {
      const yearA = parseInt(a.year, 10) || 0;
      const yearB = parseInt(b.year, 10) || 0;
      if (sort === 'year_desc') return yearB - yearA;
      if (sort === 'year_asc') return yearA - yearB;
      if (sort === 'title_asc') return (a.title || '').localeCompare(b.title || '');
      return 0;
    });

    const total = filtered.length;
    const totalPages = Math.ceil(total / limit) || 1;
    const paginated = filtered.slice((page - 1) * limit, page * limit);

    res.writeHead(200, { 'Content-Type': 'application/json' });
    res.end(JSON.stringify({
      articles: paginated,
      total,
      page,
      limit,
      totalPages,
    }));
    return;
  }

  // Dashboard Stats & Analytics
  if (pathname === '/api/stats' && method === 'GET') {
    const articles = await loadAllArticles();
    const targets = await getJournalTargets();

    const byYear = {};
    const byJournal = {};
    let doiCount = 0;
    let authorMentions = {};

    for (const a of articles) {
      // Year distribution
      const y = a.year || 'Tidak Teridentifikasi';
      byYear[y] = (byYear[y] || 0) + 1;

      // Journal distribution
      const jKey = a.journal || a.journalFolder || 'Unknown';
      byJournal[jKey] = (byJournal[jKey] || 0) + 1;

      // DOI count
      if (a.doi && a.doi.trim()) {
        doiCount++;
      }

      // Authors count
      if (a.authors) {
        const split = a.authors.split(/;|,/).map(s => s.trim()).filter(Boolean);
        for (const auth of split) {
          authorMentions[auth] = (authorMentions[auth] || 0) + 1;
        }
      }
    }

    const topAuthors = Object.entries(authorMentions)
      .sort((a, b) => b[1] - a[1])
      .slice(0, 8)
      .map(([name, count]) => ({ name, count }));

    res.writeHead(200, { 'Content-Type': 'application/json' });
    res.end(JSON.stringify({
      totalArticles: articles.length,
      totalJournalsConfigured: targets.length,
      journalsWithData: Object.keys(byJournal).length,
      doiCount,
      doiPercentage: articles.length ? Math.round((doiCount / articles.length) * 100) : 0,
      byYear,
      byJournal,
      topAuthors,
    }));
    return;
  }

  // Export Combined CSV or JSON
  if (pathname === '/api/export/csv' && method === 'GET') {
    const articles = await loadAllArticles();
    const header = 'title,authors,year,journal,url,abstract,doi,published,keywords\n';
    const rows = articles.map(a => [
      escapeCsv(a.title),
      escapeCsv(a.authors),
      escapeCsv(a.year),
      escapeCsv(a.journal),
      escapeCsv(a.url),
      escapeCsv(a.abstract),
      escapeCsv(a.doi),
      escapeCsv(a.published),
      escapeCsv(a.keywords),
    ].join(','));

    const csvContent = '\uFEFF' + header + rows.join('\n'); // UTF-8 BOM for Excel
    res.writeHead(200, {
      'Content-Type': 'text/csv; charset=utf-8',
      'Content-Disposition': 'attachment; filename="gocrawl_all_articles.csv"',
    });
    res.end(csvContent);
    return;
  }

  if (pathname === '/api/export/json' && method === 'GET') {
    const articles = await loadAllArticles();
    res.writeHead(200, {
      'Content-Type': 'application/json; charset=utf-8',
      'Content-Disposition': 'attachment; filename="gocrawl_all_articles.json"',
    });
    res.end(JSON.stringify(articles, null, 2));
    return;
  }

  // --- STATIC FILE SERVING ---
  let filePath = path.join(__dirname, 'public', pathname === '/' ? 'index.html' : pathname);
  
  // Security check: ensure within public folder
  const publicDir = path.join(__dirname, 'public');
  if (!filePath.startsWith(publicDir)) {
    res.writeHead(403, { 'Content-Type': 'text/plain' });
    res.end('Forbidden');
    return;
  }

  // If path doesn't have an extension, try adding .html
  if (!path.extname(filePath)) {
    if (fs.existsSync(filePath + '.html')) {
      filePath = filePath + '.html';
    }
  }

  try {
    const stat = await fs.promises.stat(filePath);
    if (stat.isDirectory()) {
      filePath = path.join(filePath, 'index.html');
    }
    const ext = path.extname(filePath).toLowerCase();
    const contentType = MIME_TYPES[ext] || 'application/octet-stream';
    const data = await fs.promises.readFile(filePath);
    res.writeHead(200, { 'Content-Type': contentType });
    res.end(data);
  } catch {
    res.writeHead(404, { 'Content-Type': 'text/plain; charset=utf-8' });
    res.end('Halaman tidak ditemukan (404)');
  }
});

function parseJsonBody(req) {
  return new Promise((resolve, reject) => {
    let body = '';
    req.on('data', chunk => {
      body += chunk;
      if (body.length > 1e6) {
        req.destroy();
        reject(new Error('Payload too large'));
      }
    });
    req.on('end', () => {
      try {
        resolve(body ? JSON.parse(body) : {});
      } catch (err) {
        reject(new Error('Invalid JSON: ' + err.message));
      }
    });
    req.on('error', reject);
  });
}

function escapeCsv(str) {
  return '"' + (str || '').replace(/"/g, '""').replace(/\r?\n/g, ' ') + '"';
}

server.listen(PORT, '0.0.0.0', () => {
  console.log(`[GoCrawl Web Dashboard] Running at:`);
  console.log(`  - Local:   http://localhost:${PORT}`);
  console.log(`  - Network: http://0.0.0.0:${PORT}`);
});
