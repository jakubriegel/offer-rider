import chalk from "chalk";
import util from "util";

class urlBuilder {
    constructor(origin, params = {}) {
        this.origin = origin;
        this.params = params;
    }

    build() {
        return (
            this.origin +
            "/osobowe/" +
            (this.params.brand ? `${this.params.brand}/` : "") +
            (this.params.model ? `${this.params.model}/` : "") +
            (this.params.year_from ? `od-${this.params.year_from}/` : "") +
            "?" +
            (this.params.price_from
                ? `&search%5Bfilter_float_price%3Afrom%5D=${this.params.price_from}`
                : "") +
            (this.params.price_to
                ? `&search%5Bfilter_float_price%3Ato%5D=${this.params.price_to}`
                : "") +
            (this.params.year_to
                ? `&search%5Bfilter_float_year%3Ato%5D=${this.params.year_to}`
                : "") +
            (this.params.mileage_from
                ? `&search%5Bfilter_float_mileage%3Afrom%5D=${this.params.mileage_from}`
                : "") +
            (this.params.mileage_to
                ? `&search%5Bfilter_float_mileage%3Ato%5D=${this.params.mileage_to}`
                : "")
        );
    }
}

export const buildUrlFromParams = (params) => {
    console.log(
        chalk.yellow.bgGrey(
            `\n  Building url for params: ${chalk.underline.bold(
                util.inspect(params)
            )} \n`
        )
    );

    const url = new urlBuilder("https://www.otomoto.pl", params).build();

    return url;
};

// Testing script

console.log(
    buildUrlFromParams({
        brand: "chevrolet",
        model: "corvette",
        enginge: "V8",
        year_from: 1993,
        year_to: 2017,
        price_from: 10000,
        price_to: 200000,
        mileage_from: 10,
        mileage_to: 300000
    })
);

// *************************************
// Example decomposed otomoto search URL
// *************************************
// https://www.otomoto.pl
// /osobowe/chevrolet/corvette/
// od-1993/
// ?search%5Bfilter_float_price%3Afrom%5D=10000
// &search%5Bfilter_float_price%3Ato%5D=20000
// &search%5Bfilter_float_year%3Ato%5D=2019
// TODO: &search%5Bfilter_enum_fuel_type%5D%5B0%5D=diesel
// TODO: &search%5Bfilter_enum_fuel_type%5D%5B1%5D=petrol-lpg
// &search%5Border%5D=created_at%3Adesc
// &search%5Bbrand_program_id%5D%5B0%5D=
// &search%5Bcountry%5D=
// &search%5Bfilter_float_mileage%3Afrom%5D=20000
// &search%5Bfilter_float_mileage%3Ato%5D=250000
