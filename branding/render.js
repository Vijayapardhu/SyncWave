// Rasterize SyncWave SVGs.
//
// Strategy:
//   1. Use puppeteer-core + Chrome to render each SVG at its intrinsic
//      viewBox size. (This is the only pattern that produced visible
//      output reliably on this machine.)
//   2. Use sharp to resize to the target output dimensions.
//   3. For the app icon, composite the foreground over a solid white
//      plate to get a white-background launcher icon.

const fs = require('fs');
const path = require('path');
const puppeteer = require('puppeteer-core');
const sharp = require('sharp');

const ROOT = __dirname;
const OUT = path.join(ROOT, 'out');
fs.mkdirSync(OUT, { recursive: true });

const CHROME = 'C:\\Program Files\\Google\\Chrome\\Application\\chrome.exe';

const JOBS = [
  { svg: 'syncwave-icon.svg',            w: 432,  out: 'icon-foreground-432.png',  bg: null },
  { svg: 'syncwave-icon.svg',            w: 1024, out: 'icon-foreground-1024.png', bg: null },
  { svg: 'syncwave-icon.svg',            w: 512,  out: 'app-icon-512.png',         bg: '#FFFFFF' },
  { svg: 'syncwave-lockup.svg',          w: 1440, out: 'lockup-1440.png',          bg: '#FFFFFF' },
  { svg: 'syncwave-lockup-inverse.svg',  w: 1440, out: 'lockup-inverse-1440.png',  bg: null },
  { svg: 'syncwave-readme-header.svg',   w: 1280, out: 'readme-header-1280.png',   bg: null },
];

function readViewBox(svgText) {
  const m = svgText.match(/viewBox="([\d.\s-]+)"/);
  if (!m) return [720, 220];
  const [, , w, h] = m[1].split(/\s+/).map(Number);
  return [w, h];
}

async function renderSvgOnce(browser, svgPath) {
  const svgText = fs.readFileSync(svgPath, 'utf8');
  const [w, h] = readViewBox(svgText);
  const page = await browser.newPage();
  await page.setViewport({ width: w, height: h, deviceScaleFactor: 1 });
  await page.setContent(
    `<html><body style="margin:0;background:transparent">${svgText}</body></html>`,
    { waitUntil: 'networkidle0' }
  );
  const png = await page.screenshot({ omitBackground: true });
  await page.close();
  return png;
}

async function main() {
  const browser = await puppeteer.launch({
    executablePath: CHROME,
    headless: 'new',
    args: ['--no-sandbox', '--disable-setuid-sandbox', '--disable-gpu'],
  });

  const cache = new Map();

  for (const job of JOBS) {
    const svgPath = path.join(ROOT, job.svg);
    const svgText = fs.readFileSync(svgPath, 'utf8');
    const [, , vbW, vbH] = svgText.match(/viewBox="([\d.\s-]+)"/)[1].split(/\s+/).map(Number);
    const outH = Math.round((job.w * vbH) / vbW);

    let png = cache.get(svgPath);
    if (!png) {
      png = await renderSvgOnce(browser, svgPath);
      cache.set(svgPath, png);
    }

    let img = sharp(png).resize({ width: job.w, withoutEnlargement: false });

    if (job.bg) {
      // Composite over a solid background.
      img = img.flatten({ background: job.bg });
    }

    const out = path.join(OUT, job.out);
    await img.png().toFile(out);
    console.log(`wrote ${job.out}  (${job.w}x${outH})  bg=${job.bg || 'transparent'}`);
  }

  await browser.close();
  console.log('\nAll assets rendered to', OUT);
}

main().catch(e => { console.error(e); process.exit(1); });
