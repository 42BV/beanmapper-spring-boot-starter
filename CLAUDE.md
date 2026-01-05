# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

BeanMapper Spring Boot Starter provides Spring Boot autoconfiguration for the BeanMapper library (http://beanmapper.io). This starter automatically configures a BeanMapper instance, scans for custom converters, and integrates with Spring Data JPA, Spring Security, and Spring MVC when available on the classpath.

**Core Dependency**: beanmapper-spring (version 7.0.0)
**Java Version**: 21
**Spring Boot Version**: 4.0.1

## Build and Test Commands

```bash
# Build the project
mvn clean install

# Run tests
mvn test

# Run tests with coverage (generates Jacoco report)
mvn clean test

# Run a single test class
mvn test -Dtest=BeanMapperAutoConfigTest

# Run a single test method
mvn test -Dtest=BeanMapperAutoConfigTest#autoconfig_shouldCreateBeanMapper_ifNotExists

# Security vulnerability check
mvn dependency-check:check

# Generate Javadoc
mvn javadoc:javadoc
```

## Release Process

The project uses Maven Central publishing with the `release` profile:

```bash
mvn clean deploy -P release
```

This profile activates:
- Javadoc generation
- Source attachment
- GPG signing
- Central publishing plugin

## Architecture

### Autoconfiguration Flow

The autoconfiguration is triggered by Spring Boot's autoconfiguration mechanism via `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`, which references `BeanMapperAutoConfig`.

**BeanMapperAutoConfig** (src/main/java/io/beanmapper/autoconfigure/BeanMapperAutoConfig.java:54) is the central configuration class that:

1. **Scans for package prefix**: Uses `ApplicationScanner` to find the `@SpringBootApplication` annotated class package, unless `beanmapper.package-prefix` property is set
2. **Scans for custom components**: Finds and instantiates `BeanConverter`, `CollectionHandler`, and `LogicSecuredCheck` implementations within the package prefix
3. **Conditionally configures integrations** based on classpath detection:
   - Spring Data JPA: Adds `IdToEntityBeanConverter` and `JpaAfterClearFlusher`
   - Hibernate: Configures `HibernateAwareBeanUnproxy`
   - Spring Security: Configures `SpringRoleSecuredCheck` for secured properties
   - Spring MVC: Adds `MergedFormMethodArgumentResolver` for merged form handling

### Key Components

**ApplicationScanner** (src/main/java/io/beanmapper/autoconfigure/ApplicationScanner.java): Utility class that scans the classpath for:
- The application package (via `@SpringBootApplication` annotation)
- Classes annotated with `@BeanMapToClass` or `@BeanMapFromClass`
- Implementations of `BeanConverter`, `CollectionHandler`, and `LogicSecuredCheck`

**BeanMapperProperties** (src/main/java/io/beanmapper/autoconfigure/BeanMapperProperties.java): Configuration properties under the `beanmapper` prefix:
- `package-prefix`: Root package for scanning (defaults to `@SpringBootApplication` package)
- `use-hibernate-unproxy`: Enable Hibernate proxy unwrapping (default: true)
- `apply-strict-mapping-convention`: Apply strict mapping rules (default: true)
- `apply-secured-properties`: Enable secured property handling (default: true)
- `strict-source-suffix`: Suffix for strict source classes (default: "Form")
- `strict-target-suffix`: Suffix for strict target classes (default: "Result")
- `diagnostics-level`: Diagnostics detail level (default: DISABLED)

**BeanMapperBuilderCustomizer** (src/main/java/io/beanmapper/autoconfigure/BeanMapperBuilderCustomizer.java): Functional interface allowing users to customize the `BeanMapperBuilder` before the `BeanMapper` bean is created. Users can define a bean of this type to add custom configuration.

### Conditional Component Instantiation

The autoconfiguration attempts to instantiate custom components in two ways (src/main/java/io/beanmapper/autoconfigure/BeanMapperAutoConfig.java:202):
1. First attempts no-arg constructor
2. If that fails, attempts constructor with `ApplicationContext` parameter

This allows custom converters and handlers to optionally receive the Spring ApplicationContext for dependency injection.

### MergedForm Support

The nested `MergedFormConfig` class (src/main/java/io/beanmapper/autoconfigure/BeanMapperAutoConfig.java:243) is conditionally activated when:
- The application is a web application (`@ConditionalOnWebApplication`)
- Spring Data's `EntityInformation` class is on the classpath

It registers a `MergedFormMethodArgumentResolver` that enables merging form data with existing JPA entities.

## Testing Approach

Tests use Spring's `AnnotationConfigWebApplicationContext` to simulate different Spring Boot configurations. Key patterns:

- **Custom ClassLoaders**: Tests like `autoconfig_shouldNotSetSecurityChecks_ifSpringSecurityIsMissingFromClassPath` use custom classloaders (e.g., `NoSpringSecurityClassLoader`, `NoSpringDataClassLoader`) to simulate missing dependencies
- **Property-based testing**: Use `TestPropertyValues` to inject configuration properties
- **Component scanning**: Test classes are in the same package to be discovered by the autoconfiguration scanner
- **Reflection-based assertions**: Use `ReflectionTestUtils.getField()` to verify internal configuration state

## Configuration Properties

Users can customize the BeanMapper configuration through application properties:

```properties
# Override the package to scan for converters/handlers
beanmapper.package-prefix=com.example.myapp

# Disable Hibernate unproxy mechanism
beanmapper.use-hibernate-unproxy=false

# Configure strict mapping convention
beanmapper.apply-strict-mapping-convention=true
beanmapper.strict-source-suffix=Form
beanmapper.strict-target-suffix=Result

# Disable secured property handling
beanmapper.apply-secured-properties=false
```

Or via a `BeanMapperBuilderCustomizer` bean for programmatic configuration.
