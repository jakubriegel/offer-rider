import fs from "fs";
import chalk from "chalk";
import appConfig from "../configs/appConfig.js";

const exportResults = (parsedResults) => {
    if (appConfig.isSavingData) {
        const outputFile = appConfig.outputFile;

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
    }
};

export default exportResults;