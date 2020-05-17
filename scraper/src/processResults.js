export const processResults = ($, elements, taskId, publisher, parsedResults, last = false) => {
    elements.map((_, el) => {
        const offerId = $(el).attr('data-ad-id');
        const title = $(el).find('.offer-item__title h2 a').text().trim();
        const subtitle = $(el).find('.offer-item__subtitle').text();
        const price = $(el).find('.offer-price__number span').first().text().replace(/\s/g, '');
        const currency = $(el).find('.offer-price__currency').first().text();
        const url = $(el).find('.offer-item__title h2 a').attr('href');
        const year = $(el).find('li[data-code=year] span').text();
        const mileage = $(el).find('li[data-code=mileage] span').text();
        const engine_capacity = $(el).find('li[data-code=engine_capacity] span').text();
        const fuel_type = $(el).find('li[data-code=fuel_type] span').text();
        const town = $(el).find('.ds-location-city').text();
        const image = $(el).find('.offer-item__photo a img').attr('data-src');
        const region = $(el).find('.ds-location-region').text().match(/\(([^)]+)\)/)[1];
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
                }
            },
            last
        };
        parsedResults.push(metadata);
        publisher.publish('pt-scraper-results', JSON.stringify(metadata));
    });
};