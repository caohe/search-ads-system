# Search Ads System
Implement a search ads system.

## Development environment
- **Memcached** is used to store rewritten queries, reverse indexes... etc.
- **MySQL DB** is used to store the ads and campaigns information.
- **spymemcached** is used in Java to connect to memcached server.
- **Python** is used to implement offline query rewrite pipline.
- **libmc** is used in Python to connect to memcached server.
- **Spark** and **word2vec** model are used to generate synonyms data based on click log.
## Features
- Ad server receives query from UI, performs query understanding.
- AdSelector sends queries to index servers through gRPC to retrieve level 0 ads.
- AdSelector calculates relevance score of level 0 ads. 
- AdRanker ranks ads by relevance score, pClick and bid price, return level 1 ads.
- AdFilter selects top K ads from level 1 ads.
- AdCampaignManger picks one ad per campaignId from top K ads.
- AdPricing sets cost-per-click of each ads.
- AdCampaignManager choose final ads and deduct budget for each campaign.
- Ad server returns the final list to UI.
- Offline query rewrite
    - For each query in historical click log, construct rewritten query offline
and store original query -> {rewritten query} to key value store.
    - On web server side, look up this key value store for each query to get rewritten query list,
then send to ads selector.
- Online query rewrite
    - For each query received, web server looks for rewritten queries in key value store.
    - If found, use the rewritten queries to retrieve more matching ads.
    - If not found, look up synonym table for each query term and construct rewritten query online, 
      then send to ads selector.
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
### Setup Spark
Download Spark 2.1.0, pre-built for Apache Hadoop 2.7 and later
```bash
url: https://spark.apache.org/downloads.html
```
Unzip to /spark-2.1.0-bin-hadoop2.7
### Setup Python for Spark
Modify .bash_profile
```bash
export SPARK_HOME=/spark-2.1.0-bin-hadoop2.7
export PYTHONPATH=$SPARK_HOME/python/:$PYTHONPATH
export PYTHONPATH=$SPARK_HOME/python/lib/py4j-0.10.4-src.zip:$PYTHONPATH
```
```bash
source .bash_profile
```
Both the following command will work:
```bash
> $SPARK_HOME/bin/spark-submit <python file>
```
or
```bash
> python <python file>
```
### Offline flow
Create training data.
```bash
> python generate_word2vec_training_data.py ads_0502.txt training_data_0502.txt
```
Generate synonymous data.
```bash
> python word2vec.py training_data_0502.txt synonyms_0502.txt
```
Generate rewritten queries for queries in click log.
```bash
> python query_rewrite.py click_log.txt synonyms_0502.txt
```
Rewritten queries will be stored in memcached in key value pair format:
```bash
key: REWRITE_<original queries connected with "_">
value: list of rewritten queries
```
ex:
```bash
key: REWRITE_body_care
value: ["moisturizer aging", "moisturizer skin", ...]
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
localhost:9001/ad-server/ads/bluetooth speaker
```
Should see a page of ads displayed.
![Searched Ads](/SearchedAds.png)
### Test offline query rewrite
```
> cd src/main/python
> sh test_query_rewrite.sh
```
Should see the list of original_query [rewrite_queries_list]
```
bluetooth_speaker ['zenbre wireless', 'zenbre rugged', 'zenbre speaker', 'dustproof wireless', 
'dustproof rugged', 'dustproof speaker', 'dustproof speaker', 'bluetooth wireless', 'bluetooth rugged',
 'bluetooth speaker', 'bluetooth speaker', 'bluetooth speaker']
```
Check the key value pair in memcached with key = "REWRITE_" + <original_query>
```bash
> telnet 127.0.0.1 11211
> get REWRITE_bluetooth_speaker
VALUE REWRITE_bluetooth_speaker 32 260
[
zenbre ruggedszenbre speakersdustproof wirelesssdustproof ruggedsdustproof speakersdustproof 
speakersbluetooth wirelesssbluetooth ruggedsbluetooth speakersbluetooth speakersbluetooth speaker
END
```
## LICENSE

[MIT](./License.txt)

