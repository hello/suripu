Suripu スリープ means “sleep” in Japanese.

For up to date information see our [wiki](https://github.com/hello/suripu/wiki)

Copy suripu-app.dev.yml.example to suripu-app.dev.yml and then make changes to your database config

```yaml
database:
  # the name of your JDBC driver
  driverClass: org.postgresql.Driver

  # the username
  user: tim

  # the password
  password: hello ingress user

  # the JDBC URL
  url: jdbc:postgresql://localhost:5432/tim
````

Same applies to suripu.dev.yml.example