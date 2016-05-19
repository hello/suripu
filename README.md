# suripu [![Build Status](https://travis-ci.com/hello/suripu.svg?token=jET3vYUAMBzHw6zm9Pxy&branch=master)](https://travis-ci.com/hello/suripu)

Suripu スリープ means “sleep” in Japanese.

For up to date information see our [wiki](https://github.com/hello/suripu/wiki)

It is suggested to install the following git hook:

```
$ pip install -r requirements.txt --user
$ ln -s -f ../../hooks/pre-commit .git/hooks/pre-commit
$ chmod +x hooks/pre-commit
```

to enjoy output like:

```
[INFO]  Currently on revision [09b33ddd4a5f51be485675b36254ecdf8e976ed8]
[INFO]  Also known as bacon-pennsylvania-burger-kentucky
[INFO]  Config files seem valid.
[INFO]	Running $mvn clean compile now. Might take a few seconds to complete
[INFO]	suripu-parent
[INFO]	suripu-api
[INFO]	suripu-core
[INFO]	suripu-service
[INFO]	suripu-app
[INFO]	suripu-factory
[INFO]	suripu-sync
[INFO]	suripu-workers
[INFO]	suripu-parent ..................................... SUCCESS [0.132s]
[INFO]	suripu-api ........................................ SUCCESS [3.315s]
[INFO]	suripu-core ....................................... SUCCESS [1.549s]
[INFO]	suripu-service .................................... SUCCESS [0.554s]
[INFO]	suripu-app ........................................ SUCCESS [0.341s]
[INFO]	suripu-factory .................................... SUCCESS [0.254s]
[INFO]	suripu-sync ....................................... SUCCESS [0.175s]
[INFO]	suripu-workers .................................... SUCCESS [0.227s]
```


To deploy jars locally into the `repo` folder:

1. cd into the project directory (ex: `~/java/dropwizard-mikkusu`)
2. run `mvn package`
3. cd into the target `directory`
4. run `mvn deploy:deploy-file -Durl=file:///path/to/Suripu/repo/ -Dfile=dropwizard-mikkusu-0.0.1.jar -DgroupId=com.hello.dropwizard -DartifactId=dropwizard-mikkusu -Dpackaging=jar -Dversion=0.0.1`


To tail the logs:

1. Make sure you ran `pip install -r requirements.txt --user`
2. Make sure you have your `AWS_ACCESS_KEY_ID` and `AWS_SECRET_KEY` environmnent variable set up properly.
3. cd into `/logging`
4. python kinesis.py
5. Enjoy


