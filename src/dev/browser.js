// Headless browser that serves as the shadow-cljs REPL runtime.
// Launched by bin/dev-start; killed by bin/dev-stop.
// Exposes CDP on port 9222 so bin/screenshot can connect to the live page.
const { chromium } = require('playwright');

(async () => {
  const browser = await chromium.launch({
    headless: true,
    args: ['--remote-debugging-port=9222'],
  });
  const page = await browser.newPage();
  await page.goto('http://localhost:3000');
  process.stdout.write('Browser ready\n');

  const shutdown = () => browser.close().then(() => process.exit(0));
  process.on('SIGTERM', shutdown);
  process.on('SIGINT', shutdown);
})();
