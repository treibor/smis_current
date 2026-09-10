# SMIS

## Tomcat deployment

Build the deployable WAR with JDK 17 using the production Maven profile:

```powershell
.\mvnw.cmd clean package -Pproduction
```

Deploy the WAR from `target` to Tomcat 10.1. The production build compiles the
Java views before generating the Vaadin JavaScript bundle and application theme.
Do not deploy an IDE-exported WAR or a development build.
When replacing an existing deployment, stop or undeploy that application first
and replace its old expanded application directory along with the WAR.
If the browser retains an older PWA bundle, clear site data for the deployment
and reload.

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
