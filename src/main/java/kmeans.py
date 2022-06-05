import re
import string
import json
from sklearn.feature_extraction.text import TfidfVectorizer
from sklearn.cluster import KMeans
from elasticsearch import Elasticsearch

def connect_elasticsearch():
    _es = None
    _es = Elasticsearch('http://127.0.0.1:9200')
    if _es.ping():
        print('Yay Connect')
    else:
        print('Awww it could not connect!')
    return _es

es = connect_elasticsearch()
res = es.search(index="posts", body={"query":{"match_all":{}},"size":150})

all_text = []
for text in res['hits']['hits']:
    text_json = json.dumps(text, indent=3, ensure_ascii=False)
    text = json.loads(text_json)
    text = text['_source']['text']
    text_str = str(text)
    all_text.append(text_str)
#print(all_text)

def preprocessing(line):
    line = line.lower()
    line = re.sub(r"[{}]".format(string.punctuation), " ", line)
    return line

tfidf_vectorizer = TfidfVectorizer(preprocessor=preprocessing)
tfidf = tfidf_vectorizer.fit_transform(all_text)

kmeans = KMeans(n_clusters=2).fit(tfidf)

lines_for_predicting = ["России", "oaaoaooaoa"]
prediction = kmeans.predict(tfidf_vectorizer.transform(lines_for_predicting))
print(prediction)
