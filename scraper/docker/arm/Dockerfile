FROM arm32v7/node:13.13-slim as develop-stage

RUN mkdir -p /scraper/
WORKDIR /scraper/

COPY ./package*.json /scraper/

RUN npm install
COPY . /scraper/

CMD ["npm", "start"]

FROM arm32v7/node:13.13-slim as production-stage

RUN mkdir -p /scraper/
WORKDIR /scraper/

COPY ./package*.json /scraper/

RUN npm install
COPY . /scraper/

CMD ["npm", "run" ,"start:prod"]
