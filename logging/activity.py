import boto
import time
import Logging_pb2
import base64

kinesis = boto.connect_kinesis()

class TimeoutError(Exception):
  pass

stream_name = 'activity_stream'

# Wait for the stream to be ready
tries = 0
while tries < 10:
  tries += 1
  response = kinesis.describe_stream(stream_name)
  if response['StreamDescription']['StreamStatus'] == 'ACTIVE':
    shard_id = response['StreamDescription']['Shards'][0]['ShardId']
    break
  else:
    time.sleep(15)
    #raise TimeoutError('Stream is still not active, aborting...')
# Get ready to process some data from the stream

print "Get shard iterator"
response = kinesis.get_shard_iterator(stream_name, shard_id, 'LATEST')
shard_iterator = response['ShardIterator']
print shard_iterator

# Wait for the data to show up
tries = 0
while True:
  tries += 1
  response = kinesis.get_records(shard_iterator, limit=5000, b64_decode=False)
  shard_iterator = response['NextShardIterator']
  for record in response['Records']:    
    req = Logging_pb2.HttpRequest()
    try:
      data = record['Data']
      decoded = base64.b64decode(data)
      req.ParseFromString(decoded)
      print req.path
      for header in req.headers:
        print "\t", header.name, header.value
      print
    except Exception, e:
      print e
  else:
    print "no record, sleeping for 1s..."
    time.sleep(1)

