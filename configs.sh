#!/bin/sh

if [ -z "$1" ]
  then
    echo "[ERROR] \xE2\x9A\xA0 Missing version number"
    exit 1
fi

VERSION=$1

s3cmd put suripu-app/suripu-app.prod.yml s3://hello-deploy/configs/com/hello/suripu/suripu-app/$VERSION/suripu-app.prod.yml
s3cmd put suripu-service/suripu-service.prod.yml s3://hello-deploy/configs/com/hello/suripu/suripu-service/$VERSION/suripu-service.prod.yml
s3cmd put suripu-workers/configs/pill/pillscorer.prod.yml s3://hello-deploy/configs/com/hello/suripu/suripu-workers/$VERSION/pillscorer.prod.yml
s3cmd put suripu-workers/configs/pill/pill.prod.yml s3://hello-deploy/configs/com/hello/suripu/suripu-workers/$VERSION/pill.prod.yml
s3cmd put suripu-workers/configs/alarm/alarm_worker.prod.yml s3://hello-deploy/configs/com/hello/suripu/suripu-workers/$VERSION/alarm_worker.prod.yml
s3cmd put suripu-workers/configs/sense/sense_save.prod.yml s3://hello-deploy/configs/com/hello/suripu/suripu-workers/$VERSION/sense_save.prod.yml
s3cmd put suripu-workers/configs/logs/index_logs_worker.prod.yml s3://hello-deploy/configs/com/hello/suripu/suripu-workers/$VERSION/index_logs_worker.prod.yml
s3cmd put suripu-workers/configs/insights/insights_generator.prod.yml s3://hello-deploy/configs/com/hello/suripu/suripu-workers/$VERSION/insights_generator.prod.yml
s3cmd put suripu-workers/configs/timeline/timeline_worker.prod.yml s3://hello-deploy/configs/com/hello/suripu/suripu-workers/$VERSION/timeline_worker.prod.yml