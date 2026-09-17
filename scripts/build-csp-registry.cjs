// Build-time compilation only. Input is compiled dependency constants, never browser/request data.
const fs = require('node:fs');
const entries = JSON.parse(fs.readFileSync('target/csp-constants.json', 'utf8'));
const sources = JSON.parse(fs.readFileSync('target/csp-source-inventory.json', 'utf8').replace(/^\uFEFF/, ''));
const params = Array.from({length:16}, (_, i) => '$'+i);
const accepted = [];
const events = [];
for (const {code, origin} of entries) {
  if (code.length > 20000 || /[\x00-\x08]/.test(code)) continue;
  if (!/(\$\d|\bthis\.|\bwindow\.|\bdocument\.|\bevent\.|\bVaadin\.)/.test(code)) continue;
  try { new Function(...params, code); } catch { continue; }
  accepted.push({code, origin});
}
for (const {code, origin} of entries) {
  if (code.length > 10000 || /[\x00-\x08]/.test(code) || !/\b(event|element|this)\./.test(code)) continue;
  try { new Function('event', 'element', 'return ('+code+')'); } catch { continue; }
  events.push({code, origin});
}
const methods = new Set();
for (const {source} of sources) {
  for (const m of source.matchAll(/(?:callJsFunction|executeJS|enqueue)\(\s*"([\w.$]+)"/g)) methods.add(m[1]);
}
// SOChart's command + "Data" dispatch and component methods passed through helper methods.
for (const method of ['initData','appendData','updateData','resetData','clearData','updateChart','setThemeAndLocale',
  '$connector.set','loadScript','focus','blur','click','validate']) methods.add(method);
methods.delete('$connector.');
const code = '/* Generated from resolved dependency constants. Run scripts/CollectCspConstants.java then scripts/build-csp-registry.cjs. */\n' +
  'window.__smisCspRegistry = new Map([\n' + accepted.map(({code,origin}) =>
    '// '+origin+'\n['+JSON.stringify(code)+', function('+params.join(',')+') {\n'+code+'\n}]').join(',\n') + '\n]);\n'+
  'window.__smisCspMethods = new Set('+JSON.stringify([...methods].sort())+');\n';
const eventCode = 'window.__smisCspEvents = new Map([\n' + events.map(({code,origin}) =>
  '// '+origin+'\n['+JSON.stringify(code)+', function(event,element) {return ('+code+');}]').join(',\n') + '\n]);\n';
fs.writeFileSync('src/main/resources/META-INF/resources/security/csp-registry.js', code+eventCode);
fs.writeFileSync('docs/security/csp-expression-inventory.json', JSON.stringify({functions:accepted, events, methods:[...methods].sort()},null,2)+'\n');
console.log(`Compiled ${accepted.length} static expressions; ${events.length} event expressions; ${methods.size} component methods`);
