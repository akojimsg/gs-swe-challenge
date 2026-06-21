# Multi-stage: build the SPA, then serve the static bundle via nginx with an
# /api -> Gateway proxy (same-origin in prod, mirrors the vite dev proxy).
FROM node:20-alpine AS build
WORKDIR /app
COPY package*.json ./
RUN npm ci
COPY . .
# Base URL is relative (/api/v1); nginx proxies /api to the gateway at runtime.
RUN npm run build

FROM nginx:1.27-alpine AS runtime
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist /usr/share/nginx/html
EXPOSE 80
# GATEWAY_URL is templated into nginx.conf at start (default: compose service name).
ENV GATEWAY_URL=http://gateway:8080
CMD ["/bin/sh", "-c", "envsubst '$GATEWAY_URL' < /etc/nginx/conf.d/default.conf > /tmp/default.conf && cp /tmp/default.conf /etc/nginx/conf.d/default.conf && nginx -g 'daemon off;'"]
