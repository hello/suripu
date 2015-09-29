#!/bin/sh -x
#VERSION=$(mvn org.apache.maven.plugins:maven-help-plugin:2.1.1:evaluate -Dexpression=project.version|grep -Ev '(^\[|Download\w+:)')
VERSION=0.2.65

if [[ $version == *"SNAPSHOT"* ]]
then
  echo "Snapshot version, not building deb package";
  exit 0
fi

#echo "Downloading aws-cli"
#curl "https://s3.amazonaws.com/aws-cli/awscli-bundle.zip" -o "awscli-bundle.zip"
#unzip awscli-bundle.zip
#./awscli-bundle/install -b ~/bin/aws

TEMP_DIR="/tmp/suripu-app"
mkdir -p $TEMP_DIR/opt/hello
mkdir -p $TEMP_DIR/etc/hello
mkdir -p $TEMP_DIR/etc/init/

#/home/travis/bin/aws s3 get s3://hello-deploy/configs/com/hello/suripu/suripu-app/$VERSION/suripu-app.prod.yml $TEMP_DIR/etc/hello --force
#/home/travis/bin/aws s3get s3://hello-maven/release/com/hello/suripu/suripu-app/$VERSION/suripu-app-$VERSION.jar $TEMP_DIR/opt/hello --force

s3cmd get s3://hello-deploy/configs/com/hello/suripu/suripu-app/$VERSION/suripu-app.prod.yml $TEMP_DIR/etc/hello/suripu-app.yml --force
s3cmd get s3://hello-maven/release/com/hello/suripu/suripu-app/$VERSION/suripu-app-$VERSION.jar $TEMP_DIR/opt/hello/suripu-app.jar --force

cp init-scripts/suripuapp.conf $TEMP_DIR/etc/init/

fpm --force -s dir -C $TEMP_DIR -t deb --name "suripu-app" --version $VERSION --config-files etc/hello .
#fpm --force -s dir -C $TEMP_DIR -t deb --name "kenko" --version $VERSION --config-files etc/hello .

#rm -rf $TEMP_DIR

TEMP_DIR="/tmp/suripu-service"
mkdir -p $TEMP_DIR/opt/hello
mkdir -p $TEMP_DIR/etc/hello
mkdir -p $TEMP_DIR/etc/init/

s3cmd get s3://hello-deploy/configs/com/hello/suripu/suripu-service/$VERSION/suripu-service.prod.yml $TEMP_DIR/etc/hello/suripu-service.yml --force
s3cmd get s3://hello-maven/release/com/hello/suripu/suripu-service/$VERSION/suripu-service-$VERSION.jar $TEMP_DIR/opt/hello/suripu-service.jar --force

cp init-scripts/suripuservice.conf $TEMP_DIR/etc/init/

fpm --force -s dir -C $TEMP_DIR -t deb --name "suripu-service" --version $VERSION --config-files etc/hello .
#fpm --force -p $TEMP_DIR -s dir -t deb -n "suripu-service" -v $VERSION --config-files $TEMP_DIR/etc/hello $TEMP_DIR
#rm -rf $TEMP_DIR



TEMP_DIR="/tmp/suripu-workers"
mkdir -p $TEMP_DIR/opt/hello
mkdir -p $TEMP_DIR/etc/hello
mkdir -p $TEMP_DIR/etc/init/

s3cmd get s3://hello-deploy/configs/com/hello/suripu/suripu-workers/$VERSION/pillscorer.prod.yml $TEMP_DIR/etc/hello/pillscorer.yml --force
s3cmd get s3://hello-deploy/configs/com/hello/suripu/suripu-workers/$VERSION/pill.prod.yml $TEMP_DIR/etc/hello/pill.yml --force
s3cmd get s3://hello-deploy/configs/com/hello/suripu/suripu-workers/$VERSION/alarm_worker.prod.yml $TEMP_DIR/etc/hello/alarm_worker.yml --force
s3cmd get s3://hello-deploy/configs/com/hello/suripu/suripu-workers/$VERSION/sense_save.prod.yml $TEMP_DIR/etc/hello/sense_save.yml --force
s3cmd get s3://hello-deploy/configs/com/hello/suripu/suripu-workers/$VERSION/index_logs_worker.prod.yml $TEMP_DIR/etc/hello/index_logs_worker.yml --force
s3cmd get s3://hello-deploy/configs/com/hello/suripu/suripu-workers/$VERSION/timeline_logs_worker.prod.yml $TEMP_DIR/etc/hello/timeline_logs_worker.yml --force
s3cmd get s3://hello-deploy/configs/com/hello/suripu/suripu-workers/$VERSION/insights_generator.prod.yml $TEMP_DIR/etc/hello/insights_generator.yml --force
s3cmd get s3://hello-deploy/configs/com/hello/suripu/suripu-workers/$VERSION/timeline_worker.prod.yml $TEMP_DIR/etc/hello/timeline_worker.yml --force
s3cmd get s3://hello-deploy/configs/com/hello/suripu/suripu-workers/$VERSION/push-notifications.prod.yml $TEMP_DIR/etc/hello/push-notifications.yml --force
s3cmd get s3://hello-maven/release/com/hello/suripu/suripu-workers/$VERSION/suripu-workers-$VERSION.jar $TEMP_DIR/opt/hello/suripu-workers.jar --force

cp init-scripts/suripu-workers-index-logs.conf $TEMP_DIR/etc/init/
cp init-scripts/suripu-workers-timeline-logs.conf $TEMP_DIR/etc/init/
cp init-scripts/suripu-workers-insights.conf $TEMP_DIR/etc/init/                                                          
cp init-scripts/suripu-workers-pilldata.conf $TEMP_DIR/etc/init/                                                           
cp init-scripts/suripu-workers-pillscorer.conf $TEMP_DIR/etc/init/                                                             
cp init-scripts/suripu-workers-push.conf $TEMP_DIR/etc/init/                                                          
cp init-scripts/suripu-workers-sense.conf $TEMP_DIR/etc/init/                                                                
cp init-scripts/suripu-workers-smartalarm.conf $TEMP_DIR/etc/init/                                                               
cp init-scripts/suripu-workers-timeline.conf $TEMP_DIR/etc/init/



fpm --force -s dir -C $TEMP_DIR -t deb --name "suripu-workers" --version $VERSION --config-files etc/hello .





s3cmd put suripu-service_${VERSION}_amd64.deb s3://hello-deploy/pkg/suripu-service/suripu-service_${VERSION}_amd64.deb
s3cmd put suripu-app_${VERSION}_amd64.deb s3://hello-deploy/pkg/suripu-app/suripu-app_${VERSION}_amd64.deb
s3cmd put suripu-workers_${VERSION}_amd64.deb s3://hello-deploy/pkg/suripu-workers/suripu-workers_${VERSION}_amd64.deb
