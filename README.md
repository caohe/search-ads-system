# Search Ads System
Implement a search Ads system which returns a list of recommended Ads for a search query.

## Development environment
- **Memcached** is used to store rewritten queries, reverse indexes... etc.
- **MySQL DB** is used to store the ads and campaigns information.
- **spymemcached** is used in Java to connect to memcached server.
## Features
- Ad server receives a query from UI, performs query understanding.
- AdSelector sends queries to index servers through gRPC to retrieve level 0 Ads.
- Index server gets a list of Ad candidates from cache by query terms, and calculates relevance score of each Ad, returns the level 0 Ads with relevance score larger than a threshold.
- AdRanker ranks Ads by relevance score, pClick and bid price, returns level 1 Ads.
- AdFilter selects top K ads from level 1 Ads.
- AdCampaignManger picks one Ad per campaignId from top K ads.
- AdPricing sets cost-per-click for each Ad.
- AdCampaignManager choose final Ads and deducts budget for each campaign.
- Ad server returns the final list to UI.
- Online query rewrite
    - For each query received, web server looks for rewritten queries in key value store.
    - If found, use the rewritten queries to retrieve more matching Ads.
    - If not found, look up synonym table for each query term and construct rewritten query online, 
      then send to Ads selector.
- For example:
    ```bash
    query = "body care"
    synonyms for "body" = {"moisturizer", "shaving"}
    synonyms for "care' = {"aging", "skin"}
    ``` 
    Generated rewritten queries will be
    ```bash
    {"moisturizer aging", "moisturizer skin", "moisturizer care", "shaving aging", "shaving skin",
    "shaving care", "body aging", "body skin", "body care"}
    ```
## Getting started
### Install memcached Server on mac
```
> brew install memcached
```
### Start memcached server
```bash
> /usr/local/bin/memcached -d -p 11211    // index cache 1
> /usr/local/bin/memcached -d -p 11212    // index cache 2
> /usr/local/bin/memcached -d -p 11219    // synonym cache
> /usr/local/bin/memcached -d -p 11220    // tf cache
> /usr/local/bin/memcached -d -p 11221    // df cache

```
### Prepare DB
Create new user 'testuser' with password 'testpass' on MySQL server.
```
CREATE USER 'testuser'@'localhost' IDENTIFIED BY 'testpass';
GRANT ALL PRIVILEGES ON *.* TO 'testuser'@'localhost';
```
Test login with testuser.
```
> mysql -u testuser -p
Enter password: testpass
```
### Build server
```bash
mvn clean package
```
### Start server
```bash
sh start-ad-server.sh
sh start-index-server.sh 9002 50051
sh start-index-server.sh 9003 50052
```
### Test search ads
Open Chrome, enter the test url:
```bash
http://localhost:9001/ad-server/ads/bluetooth%20speaker
```
Should see a list of ads displayed.
```aidl
[
    {
        "id": 21957,
        "adId": 5276,
        "campaignId": 8061,
        "keyWords": "nakamichi,shockwafe,pro,7.1ch,400w,45,sound,bar,8,wireless,subwoofer,rear,satellite,speaker",
        "relevanceScore": 0,
        "pClick": 0,
        "bidPrice": 2.5,
        "rankScore": 0,
        "qualityScore": 0,
        "costPerClick": 0.8223724194557837,
        "position": 0,
        "title": "Nakamichi Shockwafe Pro 7.1Ch 400W 45\" Sound Bar with 8” Wireless Subwoofer & Rear Satellite Speakers",
        "price": 0,
        "thumbnail": "https://images-na.ssl-images-amazon.com/images/I/41Cbat45+hL._AC_US218_.jpg",
        "description": "",
        "brand": "Nakamichi",
        "detailUrl": "",
        "query": null,
        "category": "Electronics",
        "pclick": 0
    }
]
```
## LICENSE

[MIT](./License.txt)

