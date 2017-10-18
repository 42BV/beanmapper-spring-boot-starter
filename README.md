[![BCH compliance](https://bettercodehub.com/edge/badge/42BV/beanmapper-spring-boot-starter?branch=master)](https://bettercodehub.com/)

# beanmapper-spring-boot-starter

Spring boot starter/autoconfig project for Beanmapper

* For more information about BeanMapper: http://beanmapper.io
* The BeanMapper-Spring project: https://github.com/42BV/beanmapper-spring

## Features

Auto-configures a `BeamMapper` instance and adds it as a spring bean to the `ApplicationContext`.
All custom implementations of `BeanConverter` found in the application's packages are instantiated and added to the `BeanMapper`.  
A `MergedFormMethodArgumentResolver` is instantiated and added to the already configured Spring MVC argument resolvers.

## Usage

Add this maven dependency to your project and you can start wiring the `BeanMapper` instance:

```xml
<dependency>
    <groupId>io.beanmapper</groupId>
    <artifactId>beanmapper-spring-boot-starter</artifactId>
    <version>2.0.1</version>
</dependency>
```

## Customization

1. By default, the package of the `@SpringBootApplication` annotated class will be used as root where `BeanMapper` will look for classes to convert and
where it will search for implementors of `BeanConverter` to instantiate and configure.
If you want to override this package, add the `beanmapper.package-prefix=<custom-root-package>` property to the application environment.
2. If you do not want to make use of the `HibernateAwareUnproxy` feature, add `beanmapper.use-hibernate-unproxy=false` to the application environment.
3. If you want full control over the `BeanMapper` configuration, add a bean of type `BeanMapperBuilderCustomizer` to the application context:

```java
@Bean
public BeanMapperBuilderCustomizer beanMapperCustomizer() {
    return builder -> {
        // After the autoconfiguration is done with the BeanMapperBuilder,
        // control over this builder is passed to this callback just before build()
        // is called to create the BeanMapper.
    };
}
```