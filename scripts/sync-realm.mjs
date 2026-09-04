// Apply docker/keycloak/realm-bms.json to a Keycloak that already has the realm.
//
// Keycloak imports a realm only when it does not already exist, so every change
// to the export is invisible to a stack that has been started once. The usual
// answer is to wipe the volume, but that Postgres holds the application
// database too, so it throws away every building, tenant and invoice to pick up
// a client or a theme.
//
// Usage: node scripts/sync-realm.mjs
//
//   KEYCLOAK_URL             default http://localhost:8081
//   KEYCLOAK_ADMIN           default admin
//   KEYCLOAK_ADMIN_PASSWORD  default admin
//
// Ordinary users are deliberately left alone: recreating one would give it a
// new id, and the application keys its own records on that id, so every
// building the demo owner has would be orphaned.

import { readFile } from 'node:fs/promises';
import { dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';

const root = join(dirname(fileURLToPath(import.meta.url)), '..');
const keycloakUrl = (process.env.KEYCLOAK_URL ?? 'http://localhost:8081').replace(/\/$/, '');
const adminUser = process.env.KEYCLOAK_ADMIN ?? 'admin';
const adminPassword = process.env.KEYCLOAK_ADMIN_PASSWORD ?? 'admin';

async function call(token, method, path, body) {
  const response = await fetch(`${keycloakUrl}${path}`, {
    method,
    headers: {
      Authorization: `Bearer ${token}`,
      ...(body === undefined ? {} : { 'Content-Type': 'application/json' }),
    },
    body: body === undefined ? undefined : JSON.stringify(body),
  });
  if (!response.ok) {
    throw new Error(`${method} ${path} failed: ${response.status} ${await response.text()}`);
  }
  const text = await response.text();
  return text === '' ? null : JSON.parse(text);
}

async function adminToken() {
  const response = await fetch(`${keycloakUrl}/realms/master/protocol/openid-connect/token`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
    body: new URLSearchParams({
      grant_type: 'password',
      client_id: 'admin-cli',
      username: adminUser,
      password: adminPassword,
    }),
  });
  if (!response.ok) {
    throw new Error(
      `Could not sign in to Keycloak at ${keycloakUrl} as "${adminUser}": ${response.status}. ` +
        'Is the stack running, and are KEYCLOAK_ADMIN and KEYCLOAK_ADMIN_PASSWORD right?',
    );
  }
  return (await response.json()).access_token;
}

/** Everything that is not a role, a client or a user is a realm setting. */
async function syncRealmSettings(token, realm, exported) {
  const { roles, clients, users, ...settings } = exported;
  await call(token, 'PUT', `/admin/realms/${realm}`, settings);
  return Object.keys(settings).filter((key) => key !== 'realm');
}

/**
 * Keycloak's own merge, in two passes because the two halves need opposite
 * policies.
 *
 * <p>Roles are only ever added. Overwriting one deletes and recreates it, and
 * every user who held it loses it: the demo owner stops being an owner, and an
 * assistant who loses their marker silently becomes one. A role whose
 * definition drifted is worth far less than that.
 *
 * <p>Clients are overwritten, since redirect URIs and secrets are exactly what
 * drifts and no user holds a client by mapping.
 */
async function syncRolesAndClients(token, realm, exported) {
  const results = [];
  for (const [ifResourceExists, payload] of [
    ['SKIP', { roles: exported.roles ?? {} }],
    ['OVERWRITE', { clients: exported.clients ?? [] }],
  ]) {
    const result = await call(token, 'POST', `/admin/realms/${realm}/partialImport`, {
      ifResourceExists,
      ...payload,
    });
    results.push(...(result?.results ?? []));
  }
  return results;
}

/**
 * A service account's permissions live in the export's users list, which is the
 * one part of it we must not import wholesale.
 */
async function syncServiceAccountRoles(token, realm, exported) {
  const granted = [];
  for (const user of exported.users ?? []) {
    if (!user.serviceAccountClientId) {
      continue;
    }
    const [client] = await call(
      token,
      'GET',
      `/admin/realms/${realm}/clients?clientId=${encodeURIComponent(user.serviceAccountClientId)}`,
    );
    if (!client) {
      throw new Error(`The client ${user.serviceAccountClientId} is missing after the import`);
    }
    const account = await call(
      token,
      'GET',
      `/admin/realms/${realm}/clients/${client.id}/service-account-user`,
    );

    for (const [containerClientId, roleNames] of Object.entries(user.clientRoles ?? {})) {
      const [container] = await call(
        token,
        'GET',
        `/admin/realms/${realm}/clients?clientId=${encodeURIComponent(containerClientId)}`,
      );
      const available = await call(
        token,
        'GET',
        `/admin/realms/${realm}/clients/${container.id}/roles`,
      );
      const wanted = available.filter((role) => roleNames.includes(role.name));
      const missing = roleNames.filter((name) => !wanted.some((role) => role.name === name));
      if (missing.length > 0) {
        throw new Error(`${containerClientId} has no role named ${missing.join(', ')}`);
      }
      await call(
        token,
        'POST',
        `/admin/realms/${realm}/users/${account.id}/role-mappings/clients/${container.id}`,
        wanted.map((role) => ({ id: role.id, name: role.name })),
      );
      granted.push(`${user.serviceAccountClientId}: ${roleNames.join(', ')} on ${containerClientId}`);
    }
  }
  return granted;
}

/** Proves the thing that actually breaks: the backend signing in as its client. */
async function checkConfidentialClients(realm, exported) {
  const checked = [];
  for (const client of exported.clients ?? []) {
    if (client.publicClient !== false || !client.secret) {
      continue;
    }
    const response = await fetch(`${keycloakUrl}/realms/${realm}/protocol/openid-connect/token`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
      body: new URLSearchParams({
        grant_type: 'client_credentials',
        client_id: client.clientId,
        client_secret: client.secret,
      }),
    });
    if (!response.ok) {
      throw new Error(
        `${client.clientId} still cannot get a token: ${response.status} ${await response.text()}`,
      );
    }
    checked.push(client.clientId);
  }
  return checked;
}

const exported = JSON.parse(await readFile(join(root, 'docker/keycloak/realm-bms.json'), 'utf8'));
const realm = exported.realm;

const token = await adminToken();
const settings = await syncRealmSettings(token, realm, exported);
console.log(`Realm settings applied: ${settings.join(', ')}`);

const imported = await syncRolesAndClients(token, realm, exported);
const counts = imported.reduce((totals, item) => {
  totals[item.action] = (totals[item.action] ?? 0) + 1;
  return totals;
}, {});
const summary = Object.entries(counts).map(([action, count]) => `${count} ${action.toLowerCase()}`);
console.log(`Roles and clients: ${summary.length > 0 ? summary.join(', ') : 'nothing to do'}`);

for (const grant of await syncServiceAccountRoles(token, realm, exported)) {
  console.log(`Service account roles: ${grant}`);
}

for (const clientId of await checkConfidentialClients(realm, exported)) {
  console.log(`Verified: ${clientId} can get a token`);
}

console.log(`\nRealm "${realm}" matches the export. Users were left untouched.`);
