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
cp /home/build/build/suripu-app.staging.yml /etc/suripu-app.prod.yml

echo "restarting..."
restart suripuapp

echo "Sleeping 30s to let suripu-app restart properly"
sleep 30

echo "deploying service on dev is currently disabled"


#echo "Updating symlink for suripu-service"
#rm /home/build/build/suripu-service.jar
#ln -s /home/build/build/suripu-service-$1.jar /home/build/build/suripu-service.jar

#echo "Moving configs to /etc/"
#cp /home/build/build/suripu-service.staging.yml /etc/suripu-service.prod.yml

#echo "restarting..."
#restart suripuservice
#sleep 1


# !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
# Note: not all workers need to run in dev,
#       uncomment if you need to run it.
# !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!

echo "Updating symlink for suripu-workers"
rm /home/build/build/suripu-workers.jar
ln -s /home/build/build/suripu-workers-$1.jar /home/build/build/suripu-workers.jar


#### Pill Postgres worker
echo "Moving pilldata configs to /etc/"
cp /home/build/build/pill.staging.yml /etc/pill.prod.yml
echo "restarting..."
restart suripuworkers-pilldata
sleep 1


#### Pill DynamoDB worker
echo "Moving pill save ddb configs to /etc/"
cp /home/build/build/pill_save_ddb.staging.yml /etc/
#echo "restarting..."
#restart suripuworkers-pill-ddb
#sleep 1


#### Alarm worker
echo "Moving smart alarm configs to /etc/"
cp /home/build/build/alarm_worker.staging.yml /etc/alarm_worker.prod.yml
#echo "restarting..."
#restart suripuworkers-smartalarm
#sleep 1


#### Sense data Postgres
echo "Moving sense save configs to /etc/"
cp /home/build/build/sense_save.staging.yml /etc/sense_save.prod.yml
echo "restarting"
restart suripuworkers-sense
sleep 1


#### Sense data DynamoDB
echo "Moving sense save ddb configs to /etc/"
cp /home/build/build/sense_save_ddb.staging.yml /etc/
#echo "restarting"
#restart suripuworkers-sense-ddb
#sleep 1


#### Index log
echo "Moving index log worker configs to /etc/"
cp /home/build/build/index_logs_worker.staging.yml /etc/index_logs_worker.prod.yml
#echo "restarting"
#restart suripuworkers-index-logs
#sleep 1


#### Timeline Analytics logs
echo "Moving timeline log worker configs to /etc/"
cp /home/build/build/timeline_logs.staging.yml /etc/
#echo "restarting"
#restart suripuworkers-timeline-logs
#sleep 1


#### Insights generation
echo "Moving insights generator worker configs to /etc/"
cp /home/build/build/insights_generator.staging.yml /etc/insights_generator.prod.yml
#echo "restarting"
#restart suripuworkers-insights
#sleep 1


#### Timeline
echo "Moving timeline worker configs to /etc/"
cp /home/build/build/timeline_worker.staging.yml /etc/timeline_worker.prod.yml
#echo "restarting"
#restart suripuworkers-timeline
#sleep 1


#### Push notifications
echo "Moving push notifications worker configs to /etc/"
cp /home/build/build/push-notifications.staging.yml /etc/push-notifications.prod.yml
#echo "restarting"
#restart suripuworkers-push
#sleep 1


#### Sense last seen
echo "Moving sense last seen worker configs to /etc/"
cp /home/build/build/sense_last_seen.staging.yml /etc/sense_last_seen.prod.yml
echo "restarting last seen"
restart suripuworkers-last-seen
sleep 1

