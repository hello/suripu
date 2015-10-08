#!/bin/sh
if mvn clean package ; then

scp -i ~/.ssh/research.pem suripu-research/target/suripu-research-*-SNAPSHOT.jar $RESEARCH_SERVER:~/benbuild/suripu-research2.jar 
say -v "Daniel" Completed building and uploading.
else
say -v "Daniel" Something went terribly wrong.
fi
