# flatparser

Flat feature extractor for irr.by. Mostly as proof of concept. 

## Usage

    java -jar flatparser.jar http://<search-url> <path-to-output-file>

Example: 

    java -jar flatparser.jar "http://irr.by/realestate/sale-flats/incity/search/offertype=%D0%BF%D1%80%D0%B5%D0%B4%D0%BB%D0%BE%D0%B6%D0%B5%D0%BD%D0%B8%D0%B5/price=%D0%BE%D1%82%2040000%20%D0%B4%D0%BE%2060000/currency=USD/" /home/user/dataset.csv

## License

Distributed under the Eclipse Public License, the same as Clojure.
