# CED MDI

This repository is the clean MDI-based successor to the CLAS12 Event Display
(CED). It is being built alongside the existing CED application so each view
can be ported and compared without leaving the production application in a
partially migrated state.

## Requirements

- Java 21 or newer (required by coatjava 14.1.2)
- Maven 3.9 or newer
- MDI `1.2.2-SNAPSHOT` installed in the local Maven repository
- coatjava `org.jlab.coat:coat-libs:14.1.2` installed in the local Maven repository

## Build and run

```bash
mvn clean verify
mvn exec:java
```

The packaged executable jar is `target/mdi-ced.jar`:

```bash
java -jar target/mdi-ced.jar
```

## Migration rule

Infrastructure is ported deliberately into the `edu.cnu.ced` namespace. The
legacy CED view, item, container, and renderer trees are not copied wholesale;
they will be replaced by coherent MDI-based vertical slices.
