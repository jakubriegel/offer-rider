FROM arm32v7/node:13.13-slim as develop-stage

RUN mkdir -p /frontend/
WORKDIR /frontend/

COPY package.json /frontend/
COPY package-lock.json /frontend/

RUN yarn install
COPY . /frontend/

FROM develop-stage as build-stage
RUN yarn build

FROM nginx:1.15.7-alpine as production-stage
COPY --from=build-stage /app/dist /usr/share/nginx/html
EXPOSE 8080
CMD ["nginx", "-g", "daemon off;"]