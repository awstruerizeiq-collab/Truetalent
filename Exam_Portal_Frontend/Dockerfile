# Stage 1: build React app
FROM node:18 AS build

WORKDIR /app

COPY package.json package-lock.json ./
RUN npm ci

COPY . .

ENV REACT_APP_API_BASE_URL=http://localhost:8080/api

RUN npm run build

# Stage 2: serve with Nginx
FROM nginx:alpine

COPY --from=build /app/build /usr/share/nginx/html

EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]
