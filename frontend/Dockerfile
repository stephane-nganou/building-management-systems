FROM node:24-alpine AS build
WORKDIR /build
COPY package.json package-lock.json ./
RUN npm ci
COPY . .
RUN npm run build

FROM nginx:1.27-alpine
# nginx substitutes the environment into anything under templates/ at startup.
COPY nginx.conf.template /etc/nginx/templates/default.conf.template
ENV BACKEND_ORIGIN=http://backend:8080
COPY --from=build /build/dist/frontend/browser /usr/share/nginx/html
EXPOSE 80
