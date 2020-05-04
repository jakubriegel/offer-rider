FROM node:13.13.0-alpine

RUN mkdir -p /scraper/
WORKDIR /scraper/

COPY package.json /scraper/
COPY package-lock.json /scraper/

RUN npm install
COPY . /scraper/

CMD ["npm", "start"]