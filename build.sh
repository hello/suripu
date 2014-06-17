#!/bin/sh

DIR="build/"

if [[ -d $DIR ]]
then
  echo "deleting $DIR directory"
  rm -r $DIR
else
 echo "$DIR directory does not exists"
fi
mkdir -p $DIR

echo "creating $DIR directory"

cp suripu-app/target/suripu-*.jar $DIR
cp suripu-service/target/suripu-*.jar $DIR
cp suripu-factory/target/suripu-*.jar $DIR

echo "Added jar files to $DIR"

cp suripu-app/suripu-app.prod.yml $DIR
cp suripu-service/suripu-service.prod.yml $DIR
cp suripu-factory/suripu-factory.prod.yml $DIR

echo "Added config files to $DIR"