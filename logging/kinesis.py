import boto
import time
import Logging_pb2
import base64

kinesis = boto.connect_kinesis()

class TimeoutError(Exception):
  pass

stream_name = 'logs'

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
while tries < 100:
  tries += 1
  response = kinesis.get_records(shard_iterator, b64_decode=False)
  shard_iterator = response['NextShardIterator']
  for record in response['Records']:    
    logger = Logging_pb2.LogMessage()
    try:
      data = record['Data']
      decoded = base64.b64decode(data)
      logger.ParseFromString(decoded)
      print logger.message
    except Exception, e:
      print e
  else:
    print "no record, sleeping for 5s..."
    time.sleep(5)

