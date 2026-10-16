import {test} from 'node:test';
import assert from 'node:assert/strict';
import {generateKeyPairSync, privateDecrypt, constants, webcrypto} from 'node:crypto';
import {encryptLoginPassword, installEncryptedLogin} from '../frontend/login-password-crypto.js';

const {publicKey, privateKey} = generateKeyPairSync('rsa', {modulusLength: 2048});
const spki = publicKey.export({type: 'spki', format: 'der'}).toString('base64');
const challenge = 'single-use-server-challenge';
const password = 'staging-\u20ac\u{1f512}';
const httpCrypto = {getRandomValues: bytes => webcrypto.getRandomValues(bytes)};

for (const [name, provider] of [['HTTP without SubtleCrypto', httpCrypto], ['HTTPS Web Crypto', webcrypto]]) {
  test(`${name} encrypts compatible UTF-8 RSA-OAEP/SHA-256 ciphertext`, async () => {
    const encrypted = await encryptLoginPassword(spki, challenge, password, provider);
    const again = await encryptLoginPassword(spki, challenge, password, provider);
    assert.notEqual(encrypted, again);
    assert.equal(Buffer.from(encrypted, 'base64').length, 256);
    assert.equal(privateDecrypt({key: privateKey, padding: constants.RSA_PKCS1_OAEP_PADDING,
      oaepHash: 'sha256', oaepLabel: Buffer.from(challenge)},
      Buffer.from(encrypted, 'base64')).toString('utf8'), password);
    assert.throws(() => privateDecrypt({key: privateKey, padding: constants.RSA_PKCS1_OAEP_PADDING,
      oaepHash: 'sha256', oaepLabel: Buffer.from('different-challenge')},
      Buffer.from(encrypted, 'base64')));
  });
}

test('fails closed if secure random generation is unavailable', async () => {
  await assert.rejects(encryptLoginPassword(spki, challenge, password, {}));
});

function control(value = '') {
  return {value, disabled: false, listeners: {}, validate: () => true,
    addEventListener(name, listener) { this.listeners[name] = listener; }};
}

test('HTTP login click sends ciphertext, clears password and installs only once', async () => {
  const descriptor = Object.getOwnPropertyDescriptor(globalThis, 'crypto');
  Object.defineProperty(globalThis, 'crypto', {value: httpCrypto, configurable: true});
  try {
    const field = control(password), button = control(), username = control('tester'), error = {}, captcha = control('ABC123');
    let sent;
    const view = {loginPublicKey: spki, loginChallenge: challenge,
      $server: {submitEncryptedLogin: async (...args) => { sent = args; }}};
    installEncryptedLogin(view, field, button, username, error, captcha);
    const handler = button.listeners.click;
    installEncryptedLogin(view, field, button, username, error, captcha);
    assert.equal(button.listeners.click, handler);
    await handler();
    assert.equal(sent.length, 3);
    for (const [index, name, expected] of [[0, 'username', 'tester'], [1, 'password', password], [2, 'captcha', 'ABC123']]) {
      assert.equal(privateDecrypt({key: privateKey, oaepHash: 'sha256',
        oaepLabel: Buffer.from(`${challenge}:${name}`)}, Buffer.from(sent[index], 'base64')).toString(), expected);
    }
    assert.equal(captcha.value, '');
    assert.equal(field.value, '');
    assert.equal(button.disabled, false);
    assert.equal(error.textContent, '');
  } finally { Object.defineProperty(globalThis, 'crypto', descriptor); }
});

test('validation and RPC failures display an error and allow another attempt', async () => {
  const field = control(password), button = control(), username = control('tester'), error = {}, captcha = control('ABC123');
  const view = {loginPublicKey: spki, loginChallenge: challenge,
    $server: {submitEncryptedLogin: async () => { throw new Error('network failure'); }}};
  installEncryptedLogin(view, field, button, username, error, captcha);
  field.validate = () => false;
  await button.listeners.click();
  assert.match(error.textContent, /valid user name and password/);
  field.validate = () => true;
  await button.listeners.click();
  assert.match(error.textContent, /Unable to sign in securely/);
  assert.equal(button.disabled, false);
  assert.equal(field.value, '');
});
