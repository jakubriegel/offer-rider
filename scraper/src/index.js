// External dependencies
import axios from "axios";
import cheerio from "cheerio";
import chalk from "chalk";
import redis from "redis";
import redisConfig from "../configs/redisConfig.js";
import exportResults from "./exportResults.js";
import {
    buildUrlFromParams
} from "./urlBuilder.js";

const parsedResults = [];
const pageLimit = 100;
let pageCounter = 0;
let resultCount = 0;

const getWebsiteContent = (url, taskId, publisher) => {
    try {
        axios.get(url).then(response => {
            const $ = cheerio.load(response.data);
            // New Lists
            $("div.offers.list article").map((i, el) => {
                const count = resultCount++;
                const offerId = $(el).attr("data-ad-id");
                const title = $(el).find(".offer-item__title h2 a").text().trim();
                const subtitle = $(el).find(".offer-item__subtitle").text();
                const price = $(el)
                    .find(".offer-price__number span")
                    .first()
                    .text()
                    .replace(/\s/g, "");
                const currency = $(el)
                    .find(".offer-price__currency")
                    .first()
                    .text();
                const url = $(el).find(".offer-item__title h2 a").attr("href");
                const year = $(el).find("li[data-code=year] span").text();
                const mileage = $(el).find("li[data-code=mileage] span").text();
                const engine_capacity = $(el)
                    .find("li[data-code=engine_capacity] span")
                    .text();
                const fuel_type = $(el).find("li[data-code=fuel_type] span").text();
                const town = $(el).find(".ds-location-city").text();
                const image = $(el)
                    .find(".offer-item__photo a img")
                    .attr("data-src");
                const region = $(el)
                    .find(".ds-location-region")
                    .text()
                    .match(/\(([^)]+)\)/)[1];
                const metadata = {
                    taskId,
                    offerId,
                    title,
                    subtitle,
                    price: parseInt(price),
                    currency,
                    url,
                    imgUrl: image,
                    params: {
                        year: parseFloat(year),
                        mileage: mileage,
                        engine_capacity,
                        fuel_type,
                        town,
                        region
                    }
                };
                parsedResults.push(metadata);
                publisher.publish('pt-scraper-results', JSON.stringify(metadata));
            });

            // Pagination Elements Link
            const nextPageLink = $(".om-pager")
                .find(".next")
                .find("a")
                .attr("href");
            pageCounter++;

            if (pageCounter === pageLimit) {
                exportResults(parsedResults);
                return false;
            }

            if (nextPageLink) {
                console.log(chalk.cyan(`  Scraping: ${nextPageLink}`));
                getWebsiteContent(nextPageLink, taskId, publisher);
            } else {
                console.log(
                    chalk.black.bgGreenBright(
                        `\n  End of scrapping. Scrapped ${parsedResults.length} items!\n`
                    )
                );
                exportResults(parsedResults);
            }
        });
    } catch (error) {
        exportResults(parsedResults);
        console.error(error);
    }
};

const app = () => {
    const subscriber = redis.createClient(redisConfig);
    const publisher = redis.createClient(redisConfig);

    subscriber.on("message", (channel, job) => {
        /// console.log(channel, job);

        const taskData = JSON.parse(job);

        const url = buildUrlFromParams(taskData.params);
        console.log(
            chalk.yellow.bgBlue(
                `\n  Scraping of ${chalk.underline.bold(url)} initiated...\n`
            )
        );

        getWebsiteContent(url, taskData.taskId, publisher);
    });

    subscriber.subscribe("pt-scraper-search-tasks");
};

app();