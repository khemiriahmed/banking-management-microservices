FROM node:18-alpine AS build

WORKDIR /app

COPY package.json package-lock.json ./
RUN npm ci

COPY angular.json tsconfig.json tsconfig.app.json tsconfig.spec.json .browserslistrc karma.conf.js proxy.conf.json ./
COPY src ./src

RUN npm run build

FROM nginx:1.27-alpine

COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist/banking-frontend /usr/share/nginx/html

EXPOSE 80
