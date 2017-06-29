import os
import sys
import json
import random
import re
from sets import Set

from nltk.corpus import stopwords
from nltk.tokenize import wordpunct_tokenize
from nltk.stem.porter import PorterStemmer

stop_words = set(stopwords.words('english'))
stop_words.update(['.', ',', '"', "'", '?', '!', ':', ';', '(', ')', '[', ']', '{', '}', '&','/','...','-','+','*','|',"),"])
porter = PorterStemmer()
def cleanData(input) :
    #remove stop words
    list_of_tokens = [i.lower() for i in wordpunct_tokenize(input) if i.lower() not in stop_words ]
    return list_of_tokens

if __name__ == "__main__":
    input_file = sys.argv[1] #ads data
    word2vec_training_file = sys.argv[2]

    word2vec_training = open(word2vec_training_file, "w")

    with open(input_file, "r") as lines:
        for line in lines:
            entry = json.loads(line.strip())
            if  "title" in entry and "adId" in entry and "query" in entry:
                    title = entry["title"].lower().encode('utf-8')
                    query = entry["query"].lower().encode('utf-8')
                    query_tokens = cleanData(query)

                    #remove number from text
                    new_query_tokens = []
                    for q in query_tokens:
                        if q.isdigit() == False and len(q) > 1:
                            new_query_tokens.append(q)

                    new_title_tokens = []
                    title_tokens = cleanData(title)
                    for t in title_tokens:
                        if t.isdigit() == False and len(t) > 1:
                            new_title_tokens.append(t)
                    query = " ".join(new_query_tokens)
                    title = " ".join(new_title_tokens)
                    word2vec_training.write(query)
                    word2vec_training.write(" ")
                    word2vec_training.write(title)
                    word2vec_training.write('\n')

    word2vec_training.close()
