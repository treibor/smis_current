# SMIS

## Temporary local no-auth testing

For local UI testing only, start the application with the `no-auth-test` Spring profile:

```text
mvn spring-boot:run -Dspring-boot.run.profiles=no-auth-test
```

In Eclipse, create or edit a **Maven Build** run configuration, set the goal to
`spring-boot:run`, and add this to the parameters/properties:

```text
-Dspring-boot.run.profiles=no-auth-test
```

The profile selects the first enabled database user with the application's `SUPER`
role and authenticates requests with that user's normal Spring Security principal.
The configured database must therefore contain a suitable user. Startup fails if it
does not, or if `no-auth-test` is combined with the `prod` or `production` Spring
profile. Without `no-auth-test`, the existing login and security flow is unchanged.
