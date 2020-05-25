FROM arm32v7/node:13.13-slim as setup-stage

WORKDIR /frontend

COPY package*.json /frontend/
RUN npm install
RUN npm install -g @vue/cli@4.3.1
RUN npm install -g @vue/cli-service@4.3.0
RUN npm install -g @vue/cli-plugin-babel@4.3.0
RUN npm install -g @vue/cli-plugin-eslint@4.3.0
RUN npm install -g vue-cli-plugin-vuetify@2.0.5
RUN npm install -g vue-template-compiler@2.6.11
RUN npm install -g vuetify-loader@1.4.2
RUN npm install -g webpack@4.43.0
COPY . /frontend/
RUN npm run build

FROM arm32v7/nginx:1.15.7 as production-stage
COPY --from=setup-stage /frontend/dist /usr/share/nginx/html
EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]


# DEVELOPMENT STAGE
FROM arm32v7/node:13.13-slim as develop-stage
WORKDIR /frontend
COPY package*.json /frontend/
COPY . /frontend/
CMD ["npm", "run", "serve"]