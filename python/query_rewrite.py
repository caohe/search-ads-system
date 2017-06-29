import sys
import time
import json
import libmc
from sets import Set
from libmc import (
    MC_HASH_MD5, MC_POLL_TIMEOUT, MC_CONNECT_TIMEOUT, MC_RETRY_TIMEOUT
)

def getRewrittenQueries(tokens, dict):
    queue = ['']
    for token in tokens:
        new_queue = []
        for y in queue:
            if not dict.has_key(token):
                continue

            all_tokens = dict[token]
            all_tokens.append(token)
            for a in all_tokens:
                if y == '':
                    new_queue.append(a)
                else:
                    new_queue.append(y + " " + a)
        queue = new_queue
    return queue

if __name__ == "__main__":
    # For testing in Jupyter notebook
    # click_log_file = "./test_click_log.txt"
    # synonyms_file = "./test_synonyms.txt"
    click_log_file = sys.argv[1] #raw query file
    synonyms_file = sys.argv[2]
    REWRITE_PREFIX = "REWRITE_"
    dict = {}
    with open(synonyms_file) as f:
        for line in f:
            data = json.loads(line)
            values = data['synonyms']
            list = []
            for value in values:
                list.append(value.encode('utf-8'))
            dict[data['word'].encode('utf-8')] = list

    mc = libmc.Client(
        ["127.0.0.1:11219"], comp_threshold=0, noreply=False, prefix=None, hash_fn=MC_HASH_MD5, failover=False
    )
    mc.config(MC_POLL_TIMEOUT, 100)
    mc.config(MC_CONNECT_TIMEOUT, 300)
    mc.config(MC_RETRY_TIMEOUT, 5)

    query_set = Set()
    with open(click_log_file, "r") as lines:
        for line in lines:
            tokens = line.strip().split(',')[3].strip().split(' ')
            if " ".join(tokens) in query_set:
                continue
            query_set.add(" ".join(tokens)) # filter out duplicated queries
            rewrittenQueries = getRewrittenQueries(tokens, dict);
            mc.set(REWRITE_PREFIX + "_".join(tokens), rewrittenQueries)
            print "_".join(tokens), mc.get(REWRITE_PREFIX + "_".join(tokens))

