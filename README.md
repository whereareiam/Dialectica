# Dialectica

Dialectica provides lightweight database helpers for Java plugins using Jdbi:
dialect-specific statements, query/update handlers, and entity schema initialization.
Java 17 or newer is required.

## Installation

```kotlin
repositories {
    maven("https://registry.whereareiam.me/maven/packages")
}

dependencies {
    implementation("me.whereareiam:dialectica:1.0.0")
    implementation("org.jdbi:jdbi3-core:<compatible-version>")
    implementation("org.jdbi:jdbi3-sqlobject:<compatible-version>")
    // Add your database's JDBC driver.
}
```

Jdbi is supplied by the application. The tested version is recorded in
`gradle/libs.versions.toml`. Plugins with a dependency loader can load these
artifacts through that loader instead of bundling them.

## Usage

```java
import me.whereareiam.dialectica.Dialectica;
import me.whereareiam.dialectica.DialectPlugin;
import me.whereareiam.dialectica.type.DatabaseType;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.sqlobject.SqlObjectPlugin;

Jdbi jdbi = Jdbi.create(dataSource);
jdbi.installPlugin(new SqlObjectPlugin());
jdbi.installPlugin(new DialectPlugin(DatabaseType.POSTGRES));

Dialectica.schema(jdbi)
        .scanPackages(getClass().getClassLoader(), "example.plugin.database.entity")
        .initialize();
```

Entity classes implement `EntitySchemaProvider` and carry `@Entity`. Register them
explicitly with `registerEntity`, or scan through their owning plugin classloader.
Schema initialization follows declared entity dependencies.

`StatementProvider` supplies dialect-specific statements to Dialectica's Jdbi
handlers. Ordinary Jdbi operations remain available for application queries.

## Migrations

Installation upgrades belong to [Strata](https://github.com/whereareiam/strata).
Use `strata-integration-dialectica` for Java actions with a borrowed Jdbi handle,
or `strata-integration-jdbc` for standalone JDBC and versioned SQL resources.
Run Strata before normal schema initialization and repository startup.

This is a breaking migration-API change: migration scopes, scanning, history-table
configuration, and the old migration runner have been removed from Dialectica.
Move those declarations into Strata streams. `LegacyDialectica.detector` supports
explicit adoption of old scope history together with actual schema/data validation.
The old history table remains available as evidence; do not blindly baseline it.

`CREATE TABLE IF NOT EXISTS` initializes absent tables; it does not upgrade existing
columns or constraints. `@Entity.version` is descriptive metadata and does not
execute migrations. Strata's history is authoritative for upgraded schemas.

## Building and publishing

```sh
./gradlew test build
./gradlew -PintegrationTests=true test
```

The first command runs fast checks without containers. The second includes
PostgreSQL and MariaDB integration tests and requires Docker. Set `DOCKER_HOST`
when using a non-default socket.

Development publishing runs only through the manual workflow. Releases run the
complete test suite, publish public packages through the shared DevOps OIDC action,
and attach binary, source and Javadoc JARs.

Release Drafter follows `dev`. Use `feature`, `change`, `bug`, or `dependencies`
labels; add `major` for breaking changes and `skip-changelog` to omit an entry.
