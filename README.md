# Flat Parser

Utility to create dataset from some real estate web sites (see list below) 

## Usage

**Graphical User Interface**

**Flat Parser** has very simple graphical interface with self-descripting parameters. To run it, just double-click on flatparser.jar or type in command line:

    java -jar flatparser.jar

Flat Parser also has command line interface. Usage for it is as follows:

    java -jar flatparser.jar http://<search-url> <#-of-pages-to-fetch> <address-of-key-point> <path-to-output-file>

Example: 

    java -jar flatparser.jar 'http://rent-and-sale.ru/moscow/rent-flat/results/roomsfrom=1&roomsto=2&apartmenttype=flat&searcharea=city&pricefrom=30000&priceto=70000&currency=rub&pricespecification=monthly&pagesize=10' 5 'Balchug, 7' /home/user/dataset.csv


## Available web sites

**Flat Parser** automatically determines what parser to use given URL of search result page. Following web sites and options are available: 

**IRR.BY / IRR.TUT.BY**

Both sale and rent of appartements in Minsk.

**RENT-AND-SALE.RU**

Only rent for Moscow.

**BN.RU**

Long-term rent for Saint Petersburg

## License

Copyright (c) Andrei Zhabinski. Distributed under the [MIT Licese](http://opensource.org/licenses/mit-license.php).
