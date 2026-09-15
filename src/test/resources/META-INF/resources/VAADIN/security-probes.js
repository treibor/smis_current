// Test-classpath only: exercise real browser enforcement without endorsing script strings.
document.addEventListener('click', event => {
  if (!event.composedPath().some(node => node.id === 'run-csp-probes')) return;
  const results = [];
  const check = (name, condition) => results.push((condition ? 'PASS ' : 'FAIL ') + name);
  const denied = action => { try { action(); return false; } catch { return true; } };
  check('Trusted Types available', !!window.trustedTypes);
  check('native string compilation blocked', denied(() => (function () {}).constructor('return 7')()));
  check('unmapped Function blocked', denied(() => new Function('return 987654321')));
  check('string eval blocked', denied(() => window.eval('987654321')));
  check('unapproved policy blocked', denied(() => trustedTypes.createPolicy('unapproved-test', {createHTML: value => value})));
  const script = document.createElement('script');
  check('external script URL blocked', denied(() => { script.src = 'https://example.invalid/test.js'; }));
  check('inline script sink blocked', denied(() => { script.text = 'void 0'; }));
  const html = document.createElement('div');
  html.innerHTML = '<p>Safe <strong>format</strong></p><img src="x" onerror="alert(1)"><a href="javascript:alert(1)">link</a><script>alert(1)</script>';
  check('safe formatting preserved', html.querySelector('strong')?.textContent === 'format');
  check('unsafe HTML removed', !html.querySelector('script,[onerror],[href^="javascript:"]'));
  document.getElementById('csp-probe-results').textContent = results.join(' | ');
});
