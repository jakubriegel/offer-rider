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
const pageLimit = 5;
const resultsSelector = 'div.offers.list article'

const processResults = ($, elements, taskId, publisher, last = false) => {
    elements.map((_, el) => {
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
            result: {
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
                },
            },
            last
        };
        parsedResults.push(metadata);
        publisher.publish('pt-scraper-results', JSON.stringify(metadata));
    });
};

const popLastResult = ($) => {
    let last = $(resultsSelector).last();
    $(`article[data-ad-id="${last.attr('data-ad-id')}"]`).remove();
    let results = $(resultsSelector);
    return { results, last }
};

const getWebsiteContent = async (url, taskId, publisher, pageCounter = 0) => {
    try {
        await axios.get(url).then(async response => {
            const $ = cheerio.load(response.data);
            const nextPageLink = $(".om-pager")
                .find(".next")
                .find("a")
                .attr("href");

            console.log(chalk.cyan(`  Scraping: ${url}`));
            if (nextPageLink && pageCounter < pageLimit) {
                processResults($, $(resultsSelector), taskId, publisher);
                await getWebsiteContent(nextPageLink, taskId, publisher, ++pageCounter);
            } else {
                let { results, last } = popLastResult($)
                processResults($, results, taskId, publisher);
                processResults($, last, taskId, publisher, true);
                console.log(
                    chalk.black.bgGreenBright(
                        `\n  End of scrapping for ${taskId}. Scrapped items from ${pageCounter} pages!\n`
                    )
                );
            }
        });
    } catch (error) {
        console.error(error);
    }
};

const app = () => {
    const subscriber = redis.createClient(redisConfig);
    const publisher = redis.createClient(redisConfig);

    subscriber.on("message", async (_, job) => {
        /// console.log(channel, job);

        const taskData = JSON.parse(job);

        const url = buildUrlFromParams(taskData.params);
        console.log(
            chalk.yellow.bgBlue(
                `\n  Scraping of ${chalk.underline.bold(url)} initiated...\n`
            )
        );

        await getWebsiteContent(url, taskData.taskId, publisher);
        await exportResults(parsedResults);
    });

    subscriber.subscribe("pt-scraper-search-tasks");
};

app();