# flatparser

Utility to create dataset from some real estate web sites (see list below) 

## Usage

    java -jar flatparser.jar http://<search-url> <path-to-output-file>

Example: 

    java -jar flatparser.jar "http://irr.by/realestate/sale-flats/incity/search/offertype=%D0%BF%D1%80%D0%B5%D0%B4%D0%BB%D0%BE%D0%B6%D0%B5%D0%BD%D0%B8%D0%B5/price=%D0%BE%D1%82+40000+%D0%B4%D0%BE+60000/currency=USD/page_len100/" /home/user/dataset.csv


## Available web sites

**flatparser** automatically determines what parser to use given URL of search result page. Following web sites and options are available: 

**IRR.BY / IRR.TUT.BY**

Both sale and rent of appartements in Minsk.

**RENT-AND-SALE.RU**

Only rent for Moscow.

**BN.RU**

Long-term rent for Saint Petersburg

## License

Copyright (c) Andrei Zhabinski. Distributed under the [MIT Licese](http://opensource.org/licenses/mit-license.php).
