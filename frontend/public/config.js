// Runtime configuration. The Docker image rewrites this file at build time
// from the API_URL and KEYCLOAK_URL build arguments.
window.__BMS_CONFIG__ = {
  apiUrl: 'http://localhost:8080',
  keycloakUrl: 'http://localhost:8081',
  realm: 'bms',
  clientId: 'bms-frontend',
};
