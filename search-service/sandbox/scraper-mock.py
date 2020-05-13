import json
import redis
from random import random
from time import sleep

SEARCH_TASKS_CHANNEL = "pt-scraper-search-tasks"
SEARCH_RESULTS_CHANNEL = "pt-scraper-results"
r = redis.Redis(host='jrie.eu', port=6379)


def mock():
    p = r.pubsub()
    p.subscribe(SEARCH_TASKS_CHANNEL)
    for msg in p.listen():
        if msg['type'] != 'subscribe':
            task = json.loads(msg['data'])
            print(f'received {task}')
            process_task(task)


def publish_result(id, n, last=False):
    msg = {
        'taskId': id,
        'offerId': int(random() * 1000000),
        'title': f'good car {n}',
        'subtitle': f'buy it',
        'price': int(random() * 500_000),
        'currency': 'PLN',
        'url': f'www.abc.moto/some_{n}',
        'imgUrl': f'https://i.picsum.photos/id/{int(random()*1000)}/250/150.jpg',
        'params': {
            'fuel': 'petrol',
            'mileage': int(random() * 500000),
            'engineSize': int((random() + .3) * 3000)
        },
        'last': last
    }
    data = json.dumps(msg)
    sleep(random() * .5)
    print(f'sending {data}')
    r.publish(SEARCH_RESULTS_CHANNEL, data)


def process_task(task):
    sleep(random() * 5)
    for i in range(int(random() * 20)):
        publish_result(task['taskId'], i)
    publish_result(task['taskId'], 'LAST', True)


if __name__ == '__main__':
    mock()
