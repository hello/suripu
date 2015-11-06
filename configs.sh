#!/bin/sh

if [ -z "$1" ]
  then
    echo "[ERROR] \xE2\x9A\xA0 Missing version number"
    exit 1
fi

VERSION=$1

# pardon the copy pasta but I don't know shit about bash

# prod

s3cmd put suripu-app/suripu-app.prod.yml s3://hello-deploy/configs/com/hello/suripu/suripu-app/$VERSION/suripu-app.prod.yml
s3cmd put suripu-service/suripu-service.prod.yml s3://hello-deploy/configs/com/hello/suripu/suripu-service/$VERSION/suripu-service.prod.yml
s3cmd put suripu-workers/configs/pill/pillscorer.prod.yml s3://hello-deploy/configs/com/hello/suripu/suripu-workers/$VERSION/pillscorer.prod.yml
s3cmd put suripu-workers/configs/pill/pill.prod.yml s3://hello-deploy/configs/com/hello/suripu/suripu-workers/$VERSION/pill.prod.yml
s3cmd put suripu-workers/configs/alarm/alarm_worker.prod.yml s3://hello-deploy/configs/com/hello/suripu/suripu-workers/$VERSION/alarm_worker.prod.yml
s3cmd put suripu-workers/configs/sense/sense_save.prod.yml s3://hello-deploy/configs/com/hello/suripu/suripu-workers/$VERSION/sense_save.prod.yml
s3cmd put suripu-workers/configs/sense/sense_save_ddb.prod.yml s3://hello-deploy/configs/com/hello/suripu/suripu-workers/$VERSION/sense_save_ddb.prod.yml
s3cmd put suripu-workers/configs/sense/sense_last_seen.prod.yml s3://hello-deploy/configs/com/hello/suripu/suripu-workers/$VERSION/sense_last_seen.prod.yml
s3cmd put suripu-workers/configs/logs/index_logs_worker.prod.yml s3://hello-deploy/configs/com/hello/suripu/suripu-workers/$VERSION/index_logs_worker.prod.yml
s3cmd put suripu-workers/configs/logs/timeline_logs.prod.yml s3://hello-deploy/configs/com/hello/suripu/suripu-workers/$VERSION/timeline_logs.prod.yml
s3cmd put suripu-workers/configs/insights/insights_generator.prod.yml s3://hello-deploy/configs/com/hello/suripu/suripu-workers/$VERSION/insights_generator.prod.yml
s3cmd put suripu-workers/configs/timeline/timeline_worker.prod.yml s3://hello-deploy/configs/com/hello/suripu/suripu-workers/$VERSION/timeline_worker.prod.yml
s3cmd put suripu-workers/configs/push/push-notifications.prod.yml s3://hello-deploy/configs/com/hello/suripu/suripu-workers/$VERSION/push-notifications.prod.yml


# staging
s3cmd put suripu-app/suripu-app.staging.yml s3://hello-deploy/configs/com/hello/suripu/suripu-app/$VERSION/suripu-app.staging.yml
s3cmd put suripu-service/suripu-service.staging.yml s3://hello-deploy/configs/com/hello/suripu/suripu-service/$VERSION/suripu-service.staging.yml
s3cmd put suripu-workers/configs/pill/pillscorer.staging.yml s3://hello-deploy/configs/com/hello/suripu/suripu-workers/$VERSION/pillscorer.staging.yml
s3cmd put suripu-workers/configs/pill/pill.staging.yml s3://hello-deploy/configs/com/hello/suripu/suripu-workers/$VERSION/pill.staging.yml
s3cmd put suripu-workers/configs/alarm/alarm_worker.staging.yml s3://hello-deploy/configs/com/hello/suripu/suripu-workers/$VERSION/alarm_worker.staging.yml
s3cmd put suripu-workers/configs/sense/sense_save.staging.yml s3://hello-deploy/configs/com/hello/suripu/suripu-workers/$VERSION/sense_save.staging.yml
s3cmd put suripu-workers/configs/sense/sense_save_ddb.staging.yml s3://hello-deploy/configs/com/hello/suripu/suripu-workers/$VERSION/sense_save_ddb.staging.yml
s3cmd put suripu-workers/configs/sense/sense_last_seen.staging.yml s3://hello-deploy/configs/com/hello/suripu/suripu-workers/$VERSION/sense_last_seen.staging.yml
s3cmd put suripu-workers/configs/logs/index_logs_worker.staging.yml s3://hello-deploy/configs/com/hello/suripu/suripu-workers/$VERSION/index_logs_worker.staging.yml
s3cmd put suripu-workers/configs/logs/timeline_logs.staging.yml s3://hello-deploy/configs/com/hello/suripu/suripu-workers/$VERSION/timeline_logs.staging.yml
s3cmd put suripu-workers/configs/insights/insights_generator.staging.yml s3://hello-deploy/configs/com/hello/suripu/suripu-workers/$VERSION/insights_generator.staging.yml
s3cmd put suripu-workers/configs/timeline/timeline_worker.staging.yml s3://hello-deploy/configs/com/hello/suripu/suripu-workers/$VERSION/timeline_worker.staging.yml
s3cmd put suripu-workers/configs/push/push-notifications.staging.yml s3://hello-deploy/configs/com/hello/suripu/suripu-workers/$VERSION/push-notifications.staging.yml
