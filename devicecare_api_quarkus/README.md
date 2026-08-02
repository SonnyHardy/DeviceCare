# DeviceCare Quarkus API

## Setup

1. Run in dev mode against the shared Postgres (make sure `docker compose up postgres flyway` has run first):

```bash
cd devicecare_api_quarkus
./mvnw quarkus:dev
```

2. Check the generated OpenAPI spec:

```
http://localhost:8080/openapi.json
```

3. Check the OpenAPI UI:

```
http://localhost:8080/q/swagger-ui
```

## Endpoints

| Method | Path | Description |
|---|---|---|
| GET | `/equipment` | List (filters: `status`, `type`) |
| GET | `/equipment/{id}` | Get one |
| POST | `/equipment` | Create |
| PUT | `/equipment/{id}` | Update |
| DELETE | `/equipment/{id}` | Delete |
| GET | `/tickets` | List (filters: `equipmentId`, `status`, `priority`) |
| GET | `/tickets/{id}` | Get one |
| POST | `/tickets` | Create (triggers business rule: equipment → `IN_MAINTENANCE`) |
| PATCH | `/tickets/{id}/status` | Change status (triggers business rule: equipment → `OPERATIONAL` once no ticket is open) |
| PUT | `/tickets/{id}` | Update (title, description, priority, assignedTo) |
| DELETE | `/tickets/{id}` | Delete |


## Notes

- Panache entities (`Equipment`, `MaintenanceTicket`) use `PanacheEntityBase` with an explicit `@Id @GeneratedValue(strategy = GenerationType.UUID)`, matching the `UUID` columns from the Flyway migration.

- `quarkus.hibernate-orm.database.generation=validate`: Hibernate never creates or alters tables.
  Flyway (run once via docker-compose, outside of this app) is the single owner of the schema.

- DTOs in `dto/` are what actually goes over the wire. Entities are never serialized directly, to avoid JSON issues with the bidirectional `Equipment ↔ MaintenanceTicket` relation.


## Running the application in dev mode

> **_NOTE:_**  Quarkus now ships with a Dev UI, which is available in dev mode only at <http://localhost:8080/q/dev/>.

## Packaging and running the application

The application can be packaged using:

```shell script
./mvnw package
```

It produces the `quarkus-run.jar` file in the `target/quarkus-app/` directory.

The application is now runnable using `java -jar target/quarkus-app/quarkus-run.jar`.

If you want to build an _über-jar_, execute the following command:

```shell script
./mvnw package -Dquarkus.package.jar.type=uber-jar
```

The application, packaged as an _über-jar_, is now runnable using `java -jar target/*-runner.jar`.

## Creating a native executable

You can create a native executable using:

```shell script
./mvnw package -Dnative
```

Or, if you don't have GraalVM installed, you can run the native executable build in a container using:

```shell script
./mvnw package -Dnative -Dquarkus.native.container-build=true
```

You can then execute your native executable with: `./target/devicecare_api_quarkus-1.0-SNAPSHOT-runner`
