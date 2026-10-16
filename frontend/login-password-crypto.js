import forge from 'node-forge';

// getRandomValues is available on HTTP origins; SubtleCrypto is HTTPS-only.
export async function encryptLoginPassword(publicKey, challenge, password, cryptoProvider = globalThis.crypto) {
  if (!cryptoProvider?.getRandomValues) {
    throw new Error('Secure random number generation is unavailable');
  }
  if (cryptoProvider.subtle) {
    const key = await cryptoProvider.subtle.importKey('spki',
      Uint8Array.from(atob(publicKey), c => c.charCodeAt(0)),
      {name: 'RSA-OAEP', hash: 'SHA-256'}, false, ['encrypt']);
    const encrypted = await cryptoProvider.subtle.encrypt(
      {name: 'RSA-OAEP', label: new TextEncoder().encode(challenge)},
      key, new TextEncoder().encode(password));
    return btoa(String.fromCharCode(...new Uint8Array(encrypted)));
  }
  const key = forge.pki.publicKeyFromAsn1(forge.asn1.fromDer(forge.util.decode64(publicKey)));
  const seed = cryptoProvider.getRandomValues(new Uint8Array(32));
  return forge.util.encode64(key.encrypt(forge.util.encodeUtf8(password), 'RSA-OAEP', {
    md: forge.md.sha256.create(),
    mgf1: {md: forge.md.sha256.create()},
    label: forge.util.encodeUtf8(challenge),
    seed: String.fromCharCode(...seed)
  }));
}

export function installEncryptedLogin(view, field, button, username, errorMessage, captcha) {
  if (button.encryptedLoginInstalled) return;
  button.encryptedLoginInstalled = true;
  const submit = async () => {
    if (button.disabled) return;
    errorMessage.textContent = '';
    field.errorMessage = 'Enter a password between 5 and 40 characters.';
    username.errorMessage = 'Enter a user name between 5 and 40 characters.';
    captcha.errorMessage = 'Enter the six-character CAPTCHA.';
    if (!field.validate() || !username.validate()) {
      errorMessage.textContent = 'Enter a valid user name and password.';
      return;
    }
    if (!captcha.validate()) {
      errorMessage.textContent = 'Enter the six-character CAPTCHA.';
      return;
    }
    button.disabled = true;
    try {
      const encrypted = await Promise.all([
        ['username', username.value], ['password', field.value], ['captcha', captcha.value]
      ].map(([name, value]) => encryptLoginPassword(view.loginPublicKey,
        `${view.loginChallenge}:${name}`, value)));
      field.value = '';
      captcha.value = '';
      await view.$server.submitEncryptedLogin(...encrypted);
    } catch (error) {
      errorMessage.textContent = 'Unable to sign in securely. Please retry or reload the page.';
    } finally {
      field.value = '';
      button.disabled = false;
    }
  };
  button.addEventListener('click', submit);
  for (const input of [username, field, captcha]) {
    input.addEventListener('keydown', event => {
      if (event.key === 'Enter') { event.preventDefault(); submit(); }
    });
  }
}

if (typeof window !== 'undefined') {
  window.smisEncryptLoginPassword = encryptLoginPassword;
  window.smisInstallEncryptedLogin = installEncryptedLogin;
}
