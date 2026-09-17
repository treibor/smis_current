/* Flow 24.2 exact precompiled expressions; never compile request data. */
(() => {
  'use strict';
  const NativeFunction = window.Function;
  const functions = window.__smisCspRegistry;
  const methods = window.__smisCspMethods;
  const events = window.__smisCspEvents;
  delete window.__smisCspRegistry;
  delete window.__smisCspMethods;
  delete window.__smisCspEvents;
  if (!(functions instanceof Map) || !(methods instanceof Set) || !(events instanceof Map)) throw new Error('Missing CSP registry');
  function blocked(code) {
    let hash = 2166136261;
    for (let i=0; i<code.length; i++) hash = Math.imul(hash ^ code.charCodeAt(i), 16777619);
    const message = 'Unmapped CSP expression ' + (hash >>> 0).toString(16);
    console.error(message);
    throw new Error(message);
  }
  function resolve(code) {
    const known = functions.get(code);
    if (known) return known;
    const element = /^return \(function\(\) \{ ([\s\S]*)\}\)\.apply\(\$(\d+)\)$/.exec(code);
    if (element) {
      const inner = resolve(element[1]), index = Number(element[2]);
      return function (...args) { return inner.apply(args[index], args); };
    }
    const promise = /^try\{Promise\.resolve\(\(function\(\)\{([\s\S]*)\}\)\(\)\)\.then\(\$(\d+),function\(error\)\{\$(\d+)\(''\+error\)\}\)\}catch\(error\)\{\$\3\(''\+error\)\}$/.exec(code);
    if (promise) {
      const inner = resolve(promise[1]), success = Number(promise[2]), failure = Number(promise[3]);
      if (failure !== success + 1) return blocked(code);
      return function (...args) {
        try { Promise.resolve(inner.apply(this, args)).then(args[success], error => args[failure](''+error)); }
        catch (error) { args[failure](''+error); }
      };
    }
    const chunk = /^return window\.Vaadin\.Flow\.loadOnDemand\('([A-Za-z0-9_.-]+)'\);?$/.exec(code);
    if (chunk) return function () { return window.Vaadin.Flow.loadOnDemand(chunk[1]); };
    const call = /^return \$0\.([\w.$]+)\(((?:\$\d+(?:,\$\d+)*)?)\)$/.exec(code);
    if (call && methods.has(call[1])) {
      const path = call[1].split('.');
      if (path.some(part => !part || ['constructor','prototype','__proto__'].includes(part))) return blocked(code);
      const indices = call[2] ? call[2].split(',').map(value => Number(value.slice(1))) : [];
      return function (...args) {
        let owner = args[0];
        for (const key of path.slice(0,-1)) owner = owner[key];
        return owner[path[path.length-1]].apply(owner, indices.map(index => args[index]));
      };
    }
    return blocked(code);
  }
  window.Function = function (...args) {
    if (!args.length) return function () {};
    if (args.length === 3 && args[0] === 'event' && args[1] === 'element') {
      const match = /^return \(([\s\S]*)\)$/.exec(args[2]);
      const known = match && events.get(match[1]);
      if (known) return known;
      return blocked(args[2]);
    }
    if (args.length === 2 && args[0] === 'callback' && args[1] === 'callback();') return callback => callback();
    if (args.length === 2 && args[0] === 'element' && args[1] === "if ( element.shadowRoot ) { return element.shadowRoot; } else { return element.attachShadow({'mode' : 'open'});}") {
      return element => element.shadowRoot || element.attachShadow({mode:'open'});
    }
    if (args.slice(0,-1).some((name,index) => name !== '$'+index)) return blocked('parameters');
    if (typeof args[args.length-1] !== 'string') return blocked('type');
    return resolve(args[args.length-1]);
  };
  window.Function.prototype = NativeFunction.prototype;
  window.eval = function (script) {
    if (typeof script !== 'string') return script;
    return blocked(script);
  };
  if (window.trustedTypes) {
    const root = new URL(document.baseURI);
    const resources = new URL('./VAADIN/', root);
    const serviceWorker = new URL('./sw.js', root);
    const purify = window.DOMPurify;
    if (!purify || !purify.isSupported) throw new Error('Missing HTML sanitizer');
    window.trustedTypes.createPolicy('default', {
      createHTML: input => purify.sanitize(input, {
        RETURN_TRUSTED_TYPE: false,
        ADD_TAGS: ['slot'],
        FORBID_TAGS: ['script','iframe','object','embed','base','meta','link','foreignObject'],
        CUSTOM_ELEMENT_HANDLING: {tagNameCheck: /^(?:vaadin|flow|vcf)-[a-z0-9-]+$/, attributeNameCheck: null},
        ALLOW_UNKNOWN_PROTOCOLS: false
      }),
      createScriptURL: input => {
        const url = new URL(input, document.baseURI);
        if (url.origin !== root.origin || !(url.pathname.startsWith(resources.pathname) || url.pathname === serviceWorker.pathname)
            || !url.pathname.endsWith('.js') || url.search || url.hash || /%2f|%5c|%2e/i.test(url.pathname)) {
          throw new TypeError('Unapproved script URL');
        }
        return url.href;
      }
      // No createScript: arbitrary script strings are never endorsed.
    });
  }
})();
