// External dependencies
import chalk from "chalk";
import redis from "redis";
import redisConfig from "../configs/redisConfig.js";
import exportResults from "./exportResults.js";
import appConfig from "../configs/appConfig.js";
import getWebsiteContent from "./getWebsiteContent.js";
import {
    buildUrlFromParams
} from "./urlBuilder.js";

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

        const parsedResults = [];

        if (appConfig.isSavingData) {
            await getWebsiteContent(url, taskData.taskId, publisher, parsedResults);
            await exportResults(parsedResults);
        } else getWebsiteContent(url, taskData.taskId, publisher, parsedResults);
    });

    subscriber.subscribe("pt-scraper-search-tasks");
};

app();