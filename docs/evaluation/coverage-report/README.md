# Test Coverage Reports

This directory contains JaCoCo HTML coverage reports generated for each Maven module.

## Generating Reports

Run the following command from the repository root:

```bash
mvn verify
```

This triggers the JaCoCo `report` goal (bound to the `verify` phase) as configured in the root `pom.xml`. Tests are compiled and executed by `maven-surefire-plugin`, then JaCoCo generates the HTML report for each module.

To skip tests and only run previously compiled results (not recommended):

```bash
mvn verify -DskipTests
```

## Output Structure

Each module produces its own report subdirectory:

```
docs/evaluation/coverage-report/
├── wildstore-cli/
│   └── index.html
├── wildstore-common/
│   └── index.html
├── wildstore-crawl/
│   └── index.html
├── wildstore-meta/
│   └── index.html
├── wildstore-testdata/
│   └── index.html
└── ...
```

Open any `index.html` in a browser to view the interactive coverage report for that module.

## Configuration

The JaCoCo plugin and output path are defined in the root `pom.xml`:

```xml
<plugin>
  <groupId>org.jacoco</groupId>
  <artifactId>jacoco-maven-plugin</artifactId>
  <version>0.8.12</version>
  <executions>
    <execution>
      <id>prepare-agent</id>
      <goals><goal>prepare-agent</goal></goals>
    </execution>
    <execution>
      <id>report</id>
      <phase>verify</phase>
      <goals><goal>report</goal></goals>
      <configuration>
        <outputDirectory>
          ${maven.multiModuleProjectDirectory}/docs/evaluation/coverage-report/${project.artifactId}
        </outputDirectory>
      </configuration>
    </execution>
  </executions>
</plugin>
```

The `${project.artifactId}` variable resolves to the module name (e.g., `wildstore-common`), so each module's report lands in its own subdirectory automatically.