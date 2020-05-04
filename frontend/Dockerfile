FROM node:13.13.0-alpine

RUN npm install -g http-server

RUN mkdir -p /frontend/
WORKDIR /frontend/

COPY package.json /frontend/
COPY package-lock.json /frontend/

RUN npm install
COPY . /frontend/

RUN npm run build
EXPOSE 8080
CMD [ "http-server", "dist" ]