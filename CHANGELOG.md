# Changelog
All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](http://keepachangelog.com/en/1.0.0/)
and this project adheres to [Semantic Versioning](http://semver.org/spec/v2.0.0.html).

## [Unreleased]

## [2.0.1] - 2017-10-18
### Fixed
- Issue [#19](https://github.com/42BV/beanmapper-spring/issues/19), **Spring handles multipart forms differently**; v4.1.6 deal with the multipart form by getting the parameterType as the target class. Later Spring versions (at least from 4.3.10.RELEASE onwards), do this by checking the genericParameterType. The solution is to check for the genericParameterType. If it exists, it is overwritten for the multipart form resolution attempt.

## [2.0.0] - 2017-10-13
### Breaking change
- Issue [#68](https://github.com/42BV/beanmapper/issues/68), **Change to BeanMapper interface**; the following methods are no longer supported:
  - ```map(Object, Class, BeanInitializer, boolean)```
  - ```map(Object, Class, boolean)```
  - ```map(Collection, Class)```
  - ```map(Collection, Class, Class)```

  The following sugarcoated interfaces have been added:
  - ```map(List, Class)```
  - ```map(Set, Class)```
  - ```map(Map, Class)``` 
- Issue [#68](https://github.com/42BV/beanmapper/issues/68), **Change to BeanCollection annotation**; the following field is no longer supported:
  - ```targetCollectionType```; the collection type is now inferred

  The following field has replaced it:
  - ```preferredCollectionClass```; states that if a collection is to be created, this preference is used instead of the CollectionHandler's default.
- Issue [#59](https://github.com/42BV/beanmapper/issues/59), **BeanCollectionUsage default CLEAR, not REUSE**; the dominant choice for applications is to have CLEAR as collection usage strategy. By making this the default, it does not have to be configured. 
### Architecture
- Issue [#68](https://github.com/42BV/beanmapper/issues/68), **Streamlining of mapping of collections**; in the previous situation, the mapping of collections happened in two different ways, one by ```MapCollectionStrategy``` and the other by ```CollectionConverter```. The latter now defers the mapping process to the former. 
### Added
- Issue [#4](https://github.com/42BV/beanmapper-spring-boot-starter/issues/4), **Scan, instantiate and register CollectionHandlers**; the starter will automatically pick up all custom CollectionHandlers and make sure they are registered in the BeanMapperBuilder. 
### Fixed
- Issue [#68](https://github.com/42BV/beanmapper/issues/68), **Arrays.asList and anonymous collections handled incorrectly**; the root cause was the difference between the two handlings of the mappings for collections. By streamlining the architecture, this problem has been solved.
- Issue [#76](https://github.com/42BV/beanmapper/issues/76), **Unproxy unable to deal with anonymous classes**; bean unproxying was unable to deal with anonymous classes. This has been fixed by making sure the SkippingBeanUnproxy checks for anonimity. If this is the case, the superclass will be passed to the unproxy delegate.

## [1.0.0] - 2017-10-04
### Added
- Issue [#75](https://github.com/42BV/beanmapper/issues/75), **Optional strict handling of BeanMapper mappings**; two layers of protection have been introduced. The first is the possibility to register a class pair with one side being strict. The strict side must have matching properties for all its valid properties. If properties are not matched, an exception will be thrown detailing the mismatches. The second layer of protection works on the Form/Result convention. It checks whether the source is consider a form Ie, classname has the suffix 'Form') or a target is a result (ie, classname has the suffix 'Result'). If this is the case, the other side must have matching properties as well. This second layer of defense works runtime right before the mapping takes place. Note that the suffix can be changed and the convention for strict mapping can be disabled.
- Issue [#2](https://github.com/42BV/beanmapper-spring-boot-starter/issues/2), **Add strict mapping annotations @BeanMapToClass / @BeanMapFromClass**; hooks into the BeanMapper's underlying strict handling of BeanMapping mappings. By adding the annotation to a class, it will be considered strict. Its counterpart must have matching relevant properties for all relevant properties (ie getters/public fields in the source or setters/public fields in the target) in the source. If matches cannot be made, an exception is thrown during boot time, disrupting the startup. All properties not having matches will be logged.
- Issue [#15](https://github.com/42BV/beanmapper-spring/issues/15), **Retain both pre-merged and merged entities**; on using the MergedForm annotation, when the class MergePair is set as a result and when the annotation field mergePairClass is set, both the original and the target class will be preserved. This allows the developer to compare the before and after situation and react accordingly. One note that must be understood; the original is not the real original (as in; the exact instance found in the database), but is mapped by BeanMapper from the fetched entity to a new, similar entity. The reason for this is that the original instance is cached by Hibernate and will be reused by the target. It cannot be preserved.
- Issue [#16](https://github.com/42BV/beanmapper-spring/issues/16), **@MergedForm must be able to read from RequestPart**; MergedForm can now read from multipart request bodies as well. When the annotation field multipart is set, the value is used to determine which part the content must be read from. Spring's RequestPartMethodArgumentResolver is reused for the process of actually reading the multipart form.
- Issue [#78](https://github.com/42BV/beanmapper/issues/78), BeanMapper contained an error that considered all fields (even private fields) as readable. This dormant error, previously seems not to have manifested itself, but with the strict handling of mappings it did. The fix has been to make sure to check the field modifiers for public access.
