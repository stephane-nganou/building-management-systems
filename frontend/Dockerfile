FROM node:24-alpine AS build
WORKDIR /build
COPY package.json package-lock.json ./
RUN npm ci
COPY . .

# The browser talks to these hosts directly, so they are baked in at image build.
ARG API_URL=http://localhost:8080
ARG KEYCLOAK_URL=http://localhost:8081
RUN printf 'window.__BMS_CONFIG__ = { apiUrl: "%s", keycloakUrl: "%s", realm: "bms", clientId: "bms-frontend" };\n' \
    "$API_URL" "$KEYCLOAK_URL" > public/config.js

RUN npm run build

FROM nginx:1.27-alpine
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /build/dist/frontend/browser /usr/share/nginx/html
EXPOSE 80
