#!/bin/sh
if [ -z "$1" ]
  then
    echo "[ERROR] \xE2\x9A\xA0 Missing version number"
    exit 1
fi

echo "Updating symlink for suripu-app"
rm /home/build/build/suripu-app.jar 
ln -s /home/build/build/suripu-app-$1.jar /home/build/build/suripu-app.jar

echo "Copying configs to /etc/"
cp /home/build/build/suripu-app.staging.yml /etc/

echo "restarting..."
restart suripuapp

echo "Sleeping 30s to let suripu-app restart properly"
sleep 30

echo "Updating symlink for suripu-service"
rm /home/build/build/suripu-service.jar
ln -s /home/build/build/suripu-service-$1.jar /home/build/build/suripu-service.jar

echo "Moving configs to /etc/"
cp /home/build/build/suripu-service.staging.yml /etc/

echo "restarting..."
restart suripuservice

sleep 1

echo "Updating symlink for suripu-workers"
rm /home/build/build/suripu-workers.jar
ln -s /home/build/build/suripu-workers-$1.jar /home/build/build/suripu-workers.jar

echo "Moving pillscorer configs to /etc/"
cp /home/build/build/pillscorer.staging.yml /etc/

echo "restarting..."
restart suripuworkers-pillscorer

sleep 1

echo "Moving pilldata configs to /etc/"
cp /home/build/build/pill.staging.yml /etc/

echo "restarting..."
restart suripuworkers-pilldata

sleep 1

echo "Moving smart alarm configs to /etc/"
cp /home/build/build/alarm_worker.staging.yml /etc/

echo "restarting..."
restart suripuworkers-smartalarm

sleep 1

echo "Moving sense save configs to /etc/"
cp /home/build/build/sense_save.staging.yml /etc/

echo "restarting"
restart suripuworkers-sense

sleep 1

echo "Moving index log worker configs to /etc/"
cp /home/build/build/index_logs_worker.staging.yml /etc/

echo "restarting"
restart suripuworkers-index-logs

sleep 1

echo "Moving timeline log worker configs to /etc/"
cp /home/build/build/timeline_logs.staging.yml /etc/

echo "restarting"
restart suripuworkers-timeline-logs

sleep 1

echo "Moving insights generator worker configs to /etc/"
cp /home/build/build/insights_generator.staging.yml /etc/

echo "restarting"
restart suripuworkers-insights

sleep 1

echo "Moving insights generator worker configs to /etc/"
cp /home/build/build/timeline_worker.staging.yml /etc/

echo "restarting"
restart suripuworkers-timeline

sleep 1

echo "Moving push notifications worker configs to /etc/"
cp /home/build/build/push-notifications.staging.yml /etc/
echo "restarting"

restart suripuworkers-push
