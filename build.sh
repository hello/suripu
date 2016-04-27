#!/bin/sh

DIR="build"

if [ -d "$DIR" ]; then
  # Control will enter here if $DIRECTORY exists.
  rm -r $DIR
  echo "$DIR was deleted"
fi
mkdir -p $DIR

echo "creating $DIR directory"

cp suripu-service/target/suripu-*.jar $DIR/
cp suripu-factory/target/suripu-*.jar $DIR/

echo "Added jar files to $DIR"

cp suripu-service/suripu-service.prod.yml $DIR/
cp suripu-factory/suripu-factory.prod.yml $DIR/

echo "Added config files to $DIR"