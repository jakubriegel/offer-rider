FROM node:13.13.0-alpine as develop-stage

RUN mkdir -p /scraper/
WORKDIR /scraper/

COPY ./package*.json /scraper/

RUN npm install
COPY . /scraper/

CMD ["npm", "start"]

FROM node:13.13.0-alpine as production-stage

RUN mkdir -p /scraper/
WORKDIR /scraper/

COPY ./package*.json /scraper/

RUN npm install
COPY . /scraper/

CMD ["npm", "run" ,"start:prod"]