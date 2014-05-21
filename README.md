Suripu スリープ means “sleep” in Japanese.

For up to date information see our [wiki](https://github.com/hello/suripu/wiki)

It is suggested to install the following git hook:

```
$ sudo pip install -r requirements.txt
$ ln -s -f ../../hooks/pre-commit .git/hooks/pre-commit
$ chmod +x hooks/pre-commit
```

to enjoy output like:

```
[INFO]  Currently on revision [09b33ddd4a5f51be485675b36254ecdf8e976ed8]
[INFO]  Also known as bacon-pennsylvania-burger-kentucky
[INFO]  Config files seem valid.
```