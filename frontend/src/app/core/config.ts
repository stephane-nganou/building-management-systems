export interface BmsConfig {
  apiUrl: string;
  keycloakUrl: string;
  realm: string;
  clientId: string;
}

declare global {
  interface Window {
    __BMS_CONFIG__?: Partial<BmsConfig>;
  }
}

const defaults: BmsConfig = {
  apiUrl: 'http://localhost:8080',
  keycloakUrl: 'http://localhost:8081',
  realm: 'bms',
  clientId: 'bms-frontend',
};

export const config: BmsConfig = { ...defaults, ...(window.__BMS_CONFIG__ ?? {}) };

export const api = (path: string) => `${config.apiUrl}${path}`;
