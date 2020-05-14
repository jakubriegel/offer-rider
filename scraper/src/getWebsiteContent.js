import axios from "axios";
import cheerio from "cheerio";
import chalk from "chalk";
import { processResults } from "./processResults.js";

const pageLimit = 5;
const resultsSelector = "div.offers.list article";

const popLastResult = ($) => {
    let last = $(resultsSelector).last();
    $(`article[data-ad-id="${last.attr("data-ad-id")}"]`).remove();
    let results = $(resultsSelector);
    return {
        results,
        last,
    };
};

const getWebsiteContent = async (
    url,
    taskId,
    publisher,
    parsedResults,
    pageCounter = 1
) => {
    try {
        await axios.get(url).then(async (response) => {
            const $ = cheerio.load(response.data);
            const nextPageLink = $(".om-pager")
                .find(".next")
                .find("a")
                .attr("href");

            console.log(chalk.cyan(`  Scraping: ${url}`));
            if (nextPageLink && pageCounter < pageLimit) {
                processResults(
                    $,
                    $(resultsSelector),
                    taskId,
                    publisher,
                    parsedResults
                );
                await getWebsiteContent(
                    nextPageLink,
                    taskId,
                    publisher,
                    parsedResults,
                    ++pageCounter
                );
            } else {
                let { results, last } = popLastResult($);
                processResults($, results, taskId, publisher, parsedResults);
                processResults($, last, taskId, publisher, parsedResults, true);
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

export default getWebsiteContent;
