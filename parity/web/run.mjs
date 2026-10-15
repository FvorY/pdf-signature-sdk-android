// Runs parity/scenarios.json with the real web SDK in headless Chrome and writes
// parity/expected/<pdf>.json. Usage: npm install && npm run expected
// Set CHROME_PATH if Chrome is not in the default macOS location.
import { readFile, writeFile, mkdir } from 'node:fs/promises';
import { createServer } from 'node:http';
import { extname, join, normalize, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';
import puppeteer from 'puppeteer-core';

const parityDir = resolve(fileURLToPath(new URL('..', import.meta.url)));
const types = { '.html': 'text/html', '.js': 'text/javascript', '.mjs': 'text/javascript', '.pdf': 'application/pdf', '.json': 'application/json' };

const server = createServer(async (request, response) => {
  const path = normalize(decodeURIComponent(new URL(request.url, 'http://localhost').pathname));
  const file = join(parityDir, path);
  if (!file.startsWith(parityDir)) {
    response.writeHead(403).end();
    return;
  }
  try {
    const body = await readFile(file);
    response.writeHead(200, { 'Content-Type': types[extname(file)] ?? 'application/octet-stream' }).end(body);
  } catch {
    response.writeHead(404).end();
  }
});
await new Promise((done) => server.listen(0, '127.0.0.1', done));
const base = `http://127.0.0.1:${server.address().port}`;

const config = JSON.parse(await readFile(join(parityDir, 'scenarios.json'), 'utf8'));
const browser = await puppeteer.launch({
  executablePath: process.env.CHROME_PATH ?? '/Applications/Google Chrome.app/Contents/MacOS/Google Chrome',
  headless: true,
  args: ['--no-sandbox'],
});

try {
  const page = await browser.newPage();
  page.on('pageerror', (error) => console.error('page error:', error.message));
  await page.goto(`${base}/web/index.html`);
  await page.waitForFunction('window.parityReady === true');
  await mkdir(join(parityDir, 'expected'), { recursive: true });

  for (const pdf of config.pdfs) {
    const forPdf = { ...config, scenarios: config.scenarios.filter((s) => !s.only || s.only.includes(pdf)) };
    const result = await page.evaluate((url, cfg) => window.runParity(url, cfg), `${base}/pdfs/${pdf}`, forPdf);
    result.pdf = pdf;
    result.webSdkVersion = '1.1.2';
    const target = join(parityDir, 'expected', pdf.replace(/\.pdf$/, '.json'));
    await writeFile(target, JSON.stringify(result, null, 2) + '\n');
    console.log(`wrote ${target}: ${Object.keys(result.scenarios).length} scenarios, ${result.pageSizes.length} pages`);
  }
} finally {
  await browser.close();
  server.close();
}
