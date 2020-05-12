# offer-rider

## about
The system for automatic searching advertisements on e-commerce web sites

## componets
The system composes of three independent modules:
* [scraper](./scraper/readme.md)
* [search-service](./search-service/readme.md)
* [frontend](./frontend/readme.md)

See their readmes for more documentation

## schema
![](./docs/schema/offer-rider-schema.png)

## deployment - Docker

1. Set varaibles in `.env `:
    - `OFFER_RIDER_ARCHITECTURE` accordingly to your machine - `arm` or `x86`
    - `TARGET` accordingly to purpose - `develop-stage` or `production-stage`
2. Run `docker-compose up --build`

If you want clean Docker after shuting down all containers type `docker-compose down -v --rmi all --remove-orphans`.

## credits
tba
