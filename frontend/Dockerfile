# --- build stage ---
FROM node:22-alpine AS build
# Enable pnpm via corepack, pinned by package.json "packageManager".
RUN corepack enable
WORKDIR /app
COPY package.json pnpm-lock.yaml ./
RUN pnpm install --frozen-lockfile
COPY . .
# Uses the committed generated API client (src/api/generated). CI regenerates it
# from the backend OpenAPI spec before building this image.
RUN pnpm run build

# --- runtime stage ---
FROM nginx:1.27-alpine
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist /usr/share/nginx/html
EXPOSE 80
