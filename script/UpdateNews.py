import os
import boto3
import datetime
import requests
from warcio.archiveiterator import ArchiveIterator
from bs4 import BeautifulSoup
from requests_aws4auth import AWS4Auth

CRAWL_DATA_REGION = 'us-east-1'

AWS_ACCESS_KEY_ID = os.environ.get('AWS_ACCESS_KEY_ID')
AWS_SECRET_ACCESS_KEY = os.environ.get('AWS_SECRET_ACCESS_KEY')
ELASTIC_SEARCH_HOST = os.environ.get('ELASTIC_SEARCH_HOST')
ELASTIC_SEARCH_INDEX = os.environ.get('ELASTIC_SEARCH_INDEX')
ELASTIC_SEARCH_REGION = os.environ.get('ELASTIC_SEARCH_REGION')

s3 = boto3.resource('s3', region_name=CRAWL_DATA_REGION,
                    aws_access_key_id=AWS_ACCESS_KEY_ID,
                    aws_secret_access_key=AWS_SECRET_ACCESS_KEY)

my_bucket = s3.Bucket('commoncrawl')
file_name = ''

today = datetime.datetime.today()
year = today.year
month = today.month

for file in my_bucket.objects.filter(Prefix='crawl-data/CC-NEWS/' + str(year) + "/{:02d}".format(month)):
    if str(file.key) > file_name:
        file_name = str(file.key)

if os.path.exists("data"):
    os.remove('data')
my_bucket.download_file(file_name, "data")
print("download finish")


def get_text_bs(html):
    soup = BeautifulSoup(html, 'lxml')

    if soup.body is None or soup.html is None:
        return None, None, None

    body = soup.body
    lang = soup.html.get('lang')
    if lang is None:
        return None, None, None

    for tag in body.select('script'):
        tag.decompose()
    for tag in body.select('style'):
        tag.decompose()

    text = body.get_text(separator='\n')
    title = None
    if soup.title:
        title = soup.title.get_text(separator='\n')
    if title is None and body.title:
        title = body.title.get_text(separator='\n')
    return text, lang, title


def read_doc(record, parser=get_text_bs):
    url = record.rec_headers.get_header('WARC-Target-URI')
    text = None
    lang = None
    title = None

    if url:
        header = record.http_headers
        html = record.content_stream().read()
        html = html.strip()

        if len(html) > 0:
            text, lang, title = parser(html)

    return url, text, lang, title


def process_warc(f_name, parser, limit=10000):
    f = open(f_name, 'rb')
    n_documents = 0
    records = []
    for i, record in enumerate(ArchiveIterator(f)):
        if record.rec_type == 'response':
            url, doc, lang, title = read_doc(record, parser)
            if not doc or not url or not title or not lang or 'en' not in lang:
                continue
            records.append((url, title, doc))

        n_documents += 1
        if n_documents % 500 == 0:
            print("parsing:", n_documents, "/", limit)

        if i > limit:
            break

    f.close()
    return records


files = process_warc("data", get_text_bs, 10000)
print("parse finish:", len(files))

awsauth = AWS4Auth(AWS_ACCESS_KEY_ID, AWS_SECRET_ACCESS_KEY, ELASTIC_SEARCH_REGION, 'es')

es_url = ELASTIC_SEARCH_HOST + ELASTIC_SEARCH_INDEX

idx_start = (today.day % 3) * 3000
inserted = 0
for i, rec in enumerate(files):
    payload = {
        "doc": {
            "url": rec[0],
            "title": rec[1],
            "txt": rec[2]
        },
        "doc_as_upsert": True
    }

    r = requests.post(es_url + "/_update/" + str(i + idx_start), auth=awsauth, json=payload)
    if r.ok:
        inserted += 1

    if inserted >= 3000:
        break

f = open("log_update", 'a')
f.write(str(datetime.datetime.now()) + " inserted:" + str(inserted) + "\n")
f.close()

if os.path.exists('data'):
    os.remove('data')

