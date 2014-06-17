rm -r build/
mkdir -p build
cp suripu-app/target/suripu-*.jar build/
cp suripu-service/target/suripu-*.jar build/
cp suripu-factory/target/suripu-*.jar build/

cp suripu-app/suripu-app.prod.yml build/
cp suripu-service/suripu-service.prod.yml build/
cp suripu-factory/suripu-factory.prod.yml build/
