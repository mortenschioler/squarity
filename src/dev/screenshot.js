// Connect to the already-running headless browser via CDP and screenshot the live page.
// Usage: node src/dev/screenshot.js [output-path]
const { chromium } = require('playwright');

const out = process.argv[2] || '/tmp/squarity_screenshot.png';

(async () => {
  const browser = await chromium.connectOverCDP('http://localhost:9222');
  const page = browser.contexts()[0].pages()[0];
  await page.screenshot({ path: out, fullPage: false });
  await browser.close();
  process.stdout.write(out + '\n');
})();
