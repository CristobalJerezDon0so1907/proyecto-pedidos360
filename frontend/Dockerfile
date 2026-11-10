# ---- Etapa 1: compilar Angular ----
FROM node:24-alpine AS build
WORKDIR /app
COPY package*.json ./
RUN npm ci
COPY . .
RUN npx ng build --configuration production

# ---- Etapa 2: servir con Nginx ----
FROM nginx:alpine
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist/pedidos360-frontend/browser /usr/share/nginx/html
EXPOSE 80