#!/bin/sh

VERSION=$1

s3cmd put suripu-app/suripu-app.prod.yml s3://hello-deploy/configs/com/hello/suripu/suripu-app/$VERSION/suripu-app.prod.yml
s3cmd put suripu-service/suripu-service.prod.yml s3://hello-deploy/configs/com/hello/suripu/suripu-service/$VERSION/suripu-service.prod.yml
s3cmd put suripu-workers/pillscorer.prod.yml s3://hello-deploy/configs/com/hello/suripu/suripu-workers/$VERSION/pillscorer.prod.yml
s3cmd put suripu-workers/pill.prod.yml s3://hello-deploy/configs/com/hello/suripu/suripu-workers/$VERSION/pill.prod.yml
s3cmd put suripu-workers/alarm_worker.prod.yml s3://hello-deploy/configs/com/hello/suripu/suripu-workers/$VERSION/alarm_worker.prod.yml
s3cmd put suripu-workers/sense_save.prod.yml s3://hello-deploy/configs/com/hello/suripu/suripu-workers/$VERSION/sense_save.prod.yml
