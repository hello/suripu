git checkout master
git pull
mvn release:clean release:prepare
git tag $( git describe --tags `git rev-list --tags --max-count=1`) -F CHANGELOG.md -f
git push origin  $( git describe --tags `git rev-list --tags --max-count=1`) -f
