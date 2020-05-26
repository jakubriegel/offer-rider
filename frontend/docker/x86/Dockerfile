FROM node:13.13.0-alpine as setup-stage

WORKDIR /frontend
COPY package.json /frontend/

FROM setup-stage as develop-stage
COPY . /frontend/
CMD ["npm", "run", "serve"]

FROM setup-stage as install-stage
WORKDIR /frontend
RUN pwd
RUN ls
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
RUN ls
RUN ls /frontend/
RUN npm run build

FROM nginx:1.15.7-alpine as production-stage
COPY --from=install-stage /frontend/dist /usr/share/nginx/html
COPY nginx/nginx.conf /etc/nginx/conf.d
EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
