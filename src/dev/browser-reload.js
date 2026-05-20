// Reload the live page in the headless browser via CDP.
const { chromium } = require('playwright');

(async () => {
  const browser = await chromium.connectOverCDP('http://localhost:9222');
  const page = browser.contexts()[0].pages()[0];
  await page.reload({ waitUntil: 'networkidle' });
  await browser.close();
  process.stdout.write('Reloaded\n');
})();
