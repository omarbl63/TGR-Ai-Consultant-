# =============================================================================
# Multi-stage build for the AdminAI Angular dashboard.
#   Stage 1: build the production bundle with Node.
#   Stage 2: serve the static files with nginx (and reverse-proxy /api).
# =============================================================================

# ---- Stage 1: build --------------------------------------------------------
FROM node:22-alpine AS build
WORKDIR /app
COPY package.json package-lock.json ./
RUN npm ci
COPY . .
RUN npm run build

# ---- Stage 2: serve --------------------------------------------------------
FROM nginx:1.27-alpine AS runtime
RUN rm /etc/nginx/conf.d/default.conf
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist/dashboard/browser /usr/share/nginx/html
EXPOSE 80
HEALTHCHECK --interval=30s --timeout=5s --start-period=10s --retries=3 \
    CMD wget -qO- http://localhost/ >/dev/null 2>&1 || exit 1
