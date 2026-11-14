// Placeholder illustrations for the three starter lessons (1080x1920, portrait).
// They stand in until real images are added through Google Drive.
// Usage: node tools/starter/art.mjs <output-dir> [--check]
//   --check also writes *-spots.png with every spotlight region drawn on top,
//   to verify that the coordinates in book.json point at the right things.
import { createRequire } from 'node:module';
import { mkdirSync, readFileSync, writeFileSync } from 'node:fs';
import { dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';

const require = createRequire(import.meta.url);
const { chromium } = require('playwright');
const here = dirname(fileURLToPath(import.meta.url));
const out = process.argv[2] ?? join(here, 'out');
const check = process.argv.includes('--check');
mkdirSync(out, { recursive: true });

const W = 1080, H = 1920;
const svg = (body) => `<svg xmlns="http://www.w3.org/2000/svg" width="${W}" height="${H}" viewBox="0 0 ${W} ${H}">${body}</svg>`;
const stars = (n, seed, maxY) => {
  let s = seed, o = '';
  const rnd = () => (s = (s * 16807) % 2147483647) / 2147483647;
  for (let i = 0; i < n; i++) o += `<circle cx="${(rnd() * W).toFixed(0)}" cy="${(rnd() * maxY).toFixed(0)}" r="${(1 + rnd() * 2.5).toFixed(1)}" fill="#fff" opacity="${(0.3 + rnd() * 0.6).toFixed(2)}"/>`;
  return o;
};

const light = svg(`
<defs>
  <linearGradient id="sky" x1="0" y1="0" x2="0" y2="1"><stop offset="0" stop-color="#17306e"/><stop offset="0.62" stop-color="#5fa8f5"/><stop offset="0.63" stop-color="#4c9a5f"/><stop offset="1" stop-color="#1e5a3a"/></linearGradient>
  <radialGradient id="glow"><stop offset="0" stop-color="#fff6c2" stop-opacity="0.9"/><stop offset="1" stop-color="#fff6c2" stop-opacity="0"/></radialGradient>
  <marker id="arrow" viewBox="0 0 10 10" refX="6" refY="5" markerWidth="5" markerHeight="5" orient="auto"><path d="M0 0L10 5L0 10z" fill="#fff3b0"/></marker>
</defs>
<rect width="${W}" height="${H}" fill="url(#sky)"/>
<circle cx="230" cy="280" r="250" fill="url(#glow)"/>
<g stroke="#ffd54a" stroke-width="14" stroke-linecap="round">${Array.from({ length: 12 }, (_, i) => { const a = i * Math.PI / 6; return `<line x1="${230 + Math.cos(a) * 140}" y1="${280 + Math.sin(a) * 140}" x2="${230 + Math.cos(a) * 185}" y2="${280 + Math.sin(a) * 185}"/>`; }).join('')}</g>
<circle cx="230" cy="280" r="110" fill="#ffd54a"/>
<line x1="345" y1="345" x2="800" y2="600" stroke="#fff3b0" stroke-width="20" stroke-linecap="round" opacity="0.9" marker-end="url(#arrow)"/>
<line x1="815" y1="625" x2="440" y2="890" stroke="#fff3b0" stroke-width="20" stroke-linecap="round" opacity="0.9" marker-end="url(#arrow)"/>
<g transform="rotate(8 860 620)"><rect x="838" y="500" width="44" height="240" rx="8" fill="#d8ecff" stroke="#5b6b85" stroke-width="8"/><rect x="882" y="500" width="14" height="240" rx="4" fill="#5b6b85"/><line x1="848" y1="530" x2="848" y2="640" stroke="#fff" stroke-width="6" stroke-linecap="round"/></g>
<g><polygon points="430,950 900,930 900,970" fill="#ff4d4d"/><polygon points="430,950 900,970 900,1010" fill="#ff9f1c"/><polygon points="430,950 900,1010 900,1050" fill="#ffe14d"/><polygon points="430,950 900,1050 900,1090" fill="#3ddc84"/><polygon points="430,950 900,1090 900,1130" fill="#3d9bff"/><polygon points="430,950 900,1130 900,1170" fill="#9b5de5"/></g>
<polygon points="340,830 235,1020 445,1020" fill="#ffffff" fill-opacity="0.45" stroke="#ffffff" stroke-width="8" stroke-linejoin="round"/>
<ellipse cx="500" cy="1560" rx="210" ry="48" fill="#000" opacity="0.38"/>
<rect x="228" y="1300" width="56" height="250" rx="10" fill="#6b4226"/>
<g fill="#1f7a45"><circle cx="256" cy="1290" r="120"/><circle cx="170" cy="1340" r="85"/><circle cx="345" cy="1340" r="85"/><circle cx="256" cy="1215" r="85"/></g>
<line x1="872" y1="1200" x2="838" y2="1345" stroke="#ff5d73" stroke-width="16" stroke-linecap="round"/>
<line x1="812" y1="1345" x2="790" y2="1505" stroke="#ff5d73" stroke-width="18" stroke-linecap="round" opacity="0.85"/>
<polygon points="748,1345 892,1345 878,1528 762,1528" fill="#7fd4ff" fill-opacity="0.55"/>
<polygon points="740,1270 900,1270 880,1535 760,1535" fill="#ffffff" fill-opacity="0.16" stroke="#ffffff" stroke-width="8" stroke-linejoin="round"/>
<line x1="748" y1="1345" x2="892" y2="1345" stroke="#fff" stroke-width="5" opacity="0.9"/>
`);

const memory = svg(`
<defs>
  <linearGradient id="night" x1="0" y1="0" x2="0" y2="1"><stop offset="0" stop-color="#150d2e"/><stop offset="0.6" stop-color="#3a1c5e"/><stop offset="1" stop-color="#0c0819"/></linearGradient>
  <radialGradient id="fire"><stop offset="0" stop-color="#ffb347" stop-opacity="0.75"/><stop offset="1" stop-color="#ffb347" stop-opacity="0"/></radialGradient>
  <radialGradient id="gold"><stop offset="0" stop-color="#ffd166" stop-opacity="0.9"/><stop offset="1" stop-color="#ffd166" stop-opacity="0"/></radialGradient>
  <radialGradient id="red"><stop offset="0" stop-color="#ff5d73" stop-opacity="0.6"/><stop offset="1" stop-color="#ff5d73" stop-opacity="0"/></radialGradient>
  <marker id="tip" viewBox="0 0 10 10" refX="7" refY="5" markerWidth="5" markerHeight="5" orient="auto"><path d="M0 0L10 5L0 10z" fill="#fff"/></marker>
</defs>
<rect width="${W}" height="${H}" fill="url(#night)"/>
${stars(70, 7, 1400)}
<g>
  <ellipse cx="540" cy="400" rx="255" ry="190" fill="#f4a6c0"/>
  <ellipse cx="600" cy="590" rx="60" ry="42" fill="#e58aa9"/>
  <g fill="none" stroke="#d76c94" stroke-width="10" stroke-linecap="round">
    <path d="M330 380q60 -70 120 -10t110 -20"/><path d="M380 300q70 -50 130 0t120 -10"/><path d="M340 470q50 40 110 0"/><path d="M620 290q60 -30 110 30"/><path d="M690 400q40 40 80 0"/><path d="M420 540q60 30 110 -10"/>
  </g>
  <circle cx="600" cy="470" r="95" fill="url(#gold)"/>
  <path d="M555 470q45 -55 90 0q-10 55 -55 40q-25 -8 -35 -40z" fill="#ffd166" stroke="#b8860b" stroke-width="5"/>
</g>
<g transform="rotate(-6 250 970)">
  <rect x="130" y="820" width="240" height="300" rx="18" fill="#dcd6ea"/>
  <g stroke="#9a93ad" stroke-width="12" stroke-linecap="round"><line x1="165" y1="880" x2="335" y2="880"/><line x1="165" y1="935" x2="310" y2="935"/><line x1="165" y1="990" x2="335" y2="990"/><line x1="165" y1="1045" x2="290" y2="1045"/></g>
</g>
<g stroke="#fff" stroke-width="8" fill="none" marker-end="url(#tip)"><line x1="680" y1="893" x2="752" y2="917"/><line x1="772" y1="962" x2="728" y2="1006"/><line x1="742" y1="1053" x2="812" y2="1076"/></g>
<circle cx="640" cy="880" r="44" fill="#ffd166"/><circle cx="800" cy="932" r="44" fill="#ff9f1c"/><circle cx="700" cy="1040" r="44" fill="#ff5d73"/><circle cx="860" cy="1092" r="44" fill="#9b5de5"/>
<circle cx="540" cy="1250" r="170" fill="url(#red)"/>
<path d="M540 1330c-130 -85 -120 -190 -50 -190c30 0 45 20 50 40c5 -20 20 -40 50 -40c70 0 80 105 -50 190z" fill="#ff5d73"/>
<path d="M0 1700q540 -90 1080 0v220h-1080z" fill="#0a0614"/>
<circle cx="540" cy="1580" r="330" fill="url(#fire)"/>
<g fill="#120a22"><circle cx="290" cy="1560" r="42"/><path d="M225 1720q0 -115 65 -115t65 115z"/><circle cx="790" cy="1560" r="42"/><path d="M725 1720q0 -115 65 -115t65 115z"/></g>
<g stroke="#5a3418" stroke-width="26" stroke-linecap="round"><line x1="460" y1="1700" x2="620" y2="1660"/><line x1="460" y1="1660" x2="620" y2="1700"/></g>
<path d="M540 1470c70 80 95 130 60 185c-20 30 -100 30 -120 0c-35 -55 20 -85 60 -185z" fill="#ff7b1c"/>
<path d="M540 1560c35 40 45 65 28 92c-10 15 -46 15 -56 0c-17 -27 8 -42 28 -92z" fill="#ffd54a"/>
`);

const water = svg(`
<defs>
  <linearGradient id="air" x1="0" y1="0" x2="0" y2="1"><stop offset="0" stop-color="#4aa3e8"/><stop offset="1" stop-color="#d4efff"/></linearGradient>
  <linearGradient id="sea" x1="0" y1="0" x2="0" y2="1"><stop offset="0" stop-color="#1f86d1"/><stop offset="1" stop-color="#0a3f78"/></linearGradient>
  <radialGradient id="sun"><stop offset="0" stop-color="#fff6c2" stop-opacity="0.9"/><stop offset="1" stop-color="#fff6c2" stop-opacity="0"/></radialGradient>
  <marker id="up" viewBox="0 0 10 10" refX="6" refY="5" markerWidth="4.5" markerHeight="4.5" orient="auto"><path d="M0 0L10 5L0 10z" fill="#fff"/></marker>
</defs>
<rect width="${W}" height="${H}" fill="url(#air)"/>
<circle cx="860" cy="240" r="230" fill="url(#sun)"/><circle cx="860" cy="240" r="100" fill="#ffd54a"/>
<g fill="#fff"><circle cx="520" cy="450" r="80"/><circle cx="610" cy="400" r="100"/><circle cx="705" cy="455" r="75"/><rect x="470" y="450" width="285" height="80" rx="40"/></g>
<polygon points="10,1320 270,800 540,1320" fill="#66758c"/>
<polygon points="270,800 205,930 245,905 272,945 300,900 335,930" fill="#fff"/>
<g fill="#8794a6"><circle cx="190" cy="585" r="70"/><circle cx="270" cy="545" r="88"/><circle cx="350" cy="590" r="66"/><rect x="140" y="585" width="260" height="70" rx="35"/></g>
<g stroke="#2f7fd0" stroke-width="10" stroke-linecap="round"><line x1="180" y1="690" x2="165" y2="740"/><line x1="250" y1="690" x2="235" y2="740"/><line x1="320" y1="690" x2="305" y2="740"/><line x1="215" y1="760" x2="200" y2="810"/><line x1="290" y1="760" x2="275" y2="810"/><line x1="360" y1="750" x2="345" y2="800"/></g>
<path d="M0 1270q270 -70 540 20t540 -10v260h-1080z" fill="#58a868"/>
<path d="M300 1150q-30 90 90 140t60 160t-60 110" fill="none" stroke="#3fa9f5" stroke-width="44" stroke-linecap="round"/>
<rect y="1500" width="${W}" height="420" fill="url(#sea)"/>
<path d="M0 1500q45 -26 90 0t90 0t90 0t90 0t90 0t90 0t90 0t90 0t90 0t90 0t90 0t90 0v30h-1080z" fill="#1f86d1"/>
<g fill="none" stroke="#fff" stroke-width="12" stroke-linecap="round" opacity="0.85" marker-end="url(#up)"><path d="M760 1450q-40 -60 0 -120t0 -120t0 -120"/><path d="M860 1450q-40 -60 0 -120t0 -120t0 -120"/><path d="M960 1450q-40 -60 0 -120t0 -120t0 -120"/></g>
<g><path d="M700 1540c70 90 95 130 95 165a95 95 0 0 1 -190 0c0 -35 25 -75 95 -165z" fill="#8fdcff" stroke="#fff" stroke-width="8"/><circle cx="668" cy="1700" r="11" fill="#12355b"/><circle cx="732" cy="1700" r="11" fill="#12355b"/><path d="M670 1735q30 26 60 0" fill="none" stroke="#12355b" stroke-width="8" stroke-linecap="round"/></g>
`);

const scenes = { light, memory, water };
const book = JSON.parse(readFileSync(join(here, 'book.json'), 'utf8'));
const spotsFor = (name) => {
  const lesson = book.chapters.flatMap((c) => c.lessons).find((l) => l.image.includes(name));
  const seen = new Map();
  for (const t of lesson.tracks) for (const s of t.segments) if (s.spot) seen.set(JSON.stringify(s.spot), s.spot);
  return [...seen.values()];
};

const browser = await chromium.launch();
const page = await browser.newPage({ viewport: { width: W, height: H }, deviceScaleFactor: 1 });
for (const [name, markup] of Object.entries(scenes)) {
  writeFileSync(join(out, `${name}.svg`), markup);
  await page.setContent(`<body style="margin:0">${markup}</body>`);
  await page.screenshot({ path: join(out, `${name}.png`) });
  if (check) {
    const rings = spotsFor(name).map((s, i) => `<circle cx="${s.x * W}" cy="${s.y * H}" r="${s.r * W}" fill="none" stroke="#ff00ff" stroke-width="6"/><text x="${s.x * W}" y="${s.y * H}" fill="#ff00ff" font-size="40" font-family="sans-serif">${i + 1}</text>`).join('');
    await page.setContent(`<body style="margin:0">${markup.replace('</svg>', rings + '</svg>')}</body>`);
    await page.screenshot({ path: join(out, `${name}-spots.png`) });
  }
}
await browser.close();
