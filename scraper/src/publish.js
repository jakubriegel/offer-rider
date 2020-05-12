import redis from "redis";

const redisConfig = {
    host: "jrie.eu",
    port: "6379"
};

var publisher = redis.createClient(redisConfig);
publisher.publish(
    "pt-scraper-search-tasks",
    `
    {
        "taskId": "123e4567-e89b-12d3-a456-426614174000",
        "params": {
            "brand": "chevrolet",
            "model": "corvette",
            "enginge": "V8",
            "year_from": 1990,
            "year_to": 2017,
            "price_from": 10000,
            "price_to": 300000,
            "mileage_from": 10,
            "mileage_to": 300000
          }
    }
    `,
    function () {
        console.log("Published!");
        process.exit(0);
    }
);