// External dependencies
const axios = require("axios");
const cheerio = require("cheerio");
const fs = require("fs");
const chalk = require("chalk");
const redis = require("redis");

const url =
    "https://www.otomoto.pl/osobowe/chevrolet/corvette/?search%5Bfilter_float_year%3Ato%5D=1991&search%5Border%5D=created_at%3Adesc&search%5Bbrand_program_id%5D%5B0%5D=&search%5Bcountry%5D=";
const outputFile = "data.json";
const parsedResults = [];
const pageLimit = 100;
let pageCounter = 0;
let resultCount = 0;

console.log(
    chalk.yellow.bgBlue(
        `\n  Scraping of ${chalk.underline.bold(url)} initiated...\n`
    )
);

const exportResults = (parsedResults) => {
    fs.writeFile(outputFile, JSON.stringify(parsedResults, null, 4), (err) => {
        if (err) {
            console.log(err);
        }
        console.log(
            chalk.yellow.bgBlue(
                `\n ${chalk.underline.bold(
                    parsedResults.length
                )} Results exported successfully to ${chalk.underline.bold(
                    outputFile
                )}\n`
            )
        );
    });
};

const getWebsiteContent = async (url) => {
    try {
        const response = await axios.get(url);
        const $ = cheerio.load(response.data);
        // New Lists
        $("div.listHandler article").map((i, el) => {
            const count = resultCount++;
            const title = $(el).find(".offer-item__title h2 a").text().trim();
            const subtitle = $(el).find(".offer-item__subtitle").text();
            const price = $(el).find('.offer-price__number span').first().text().replace(/\s/g, '');
            const currency = $(el).find('.offer-price__currency').first().text();
            const url = $(el).find(".offer-item__title h2 a").attr("href");
            const year = $(el).find("li[data-code=year] span").text();
            const mileage = $(el).find("li[data-code=mileage] span").text();
            const engine_capacity = $(el).find("li[data-code=engine_capacity] span").text();
            const fuel_type = $(el).find("li[data-code=fuel_type] span").text();
            const town = $(el).find('.ds-location-city').text();
            const image = $(el).find('.offer-item__photo a img').attr('data-src');
            const region = $(el).find('.ds-location-region').text().match(/\(([^)]+)\)/)[1];
            const metadata = {
                taskId: null,
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
                    region,
                }
            };
            parsedResults.push(metadata);
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
            getWebsiteContent(nextPageLink);
        } else {
            console.log(
                chalk.black.bgGreenBright(
                    `\n  End of scrapping. Scrapped ${resultCount} items!\n`
                )
            );
            exportResults(parsedResults);
        }
    } catch (error) {
        exportResults(parsedResults);
        console.error(error);
    }
};

getWebsiteContent(url);