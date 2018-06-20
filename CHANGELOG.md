# Changelog
All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](http://keepachangelog.com/en/1.0.0/)
and this project adheres to [Semantic Versioning](http://semver.org/spec/v2.0.0.html).

## [Unreleased]

## [2.4.1] - 2018-06-20
### Fixed
- Issue [#109](https://github.com/42BV/beanmapper/issues/109), **Specify the return type for AbstractBeanConverter.doConvert**; on extending AbstractBeanConverter, it is beneficial for the developer to immediately see the expected return type for the ```doConvert``` method.
- Bug [#111](https://github.com/42BV/beanmapper/issues/111), **BeanCollection null list overrides the existing list**; when a source and target have been assigned to a collection handler, it will now treat a null value for the source as special, subscribing it to the BeanCollectionUsage rules (default: CLEAR). That is, it will REUSE the target, CLEAR it, or CONSTRUCT a new one. This is the most logical behaviour with Hibernate on the other side. 
- Bug [#112](https://github.com/42BV/beanmapper/issues/112), **Strict mapping messes up the build sequence**; The validation took place before all configuration was done, resulting in collection handlers not being available in some cases. The validation of strict classes is done as part of the last step of the ```BeanMapperBuilder.build()``` method, after all required steps have been taken.

## [2.4.0] - 2018-03-28
This release effectively adds the following functionality:
- **@BeanRoleSecured annotation**; checks the Principal for any one of the stated roles. If allowed, the mapping will proceed as normal. If not allowed, the mapping will not take place.
- **@BeanLogicSecured annotation**; consults the stated class for the allowance check. The LogicSecuredCheck class will be instantiated by the starter. Its isAllowed() method will be called with the source and the target by BeanMapper. The types can be finetuned by setting the generics. Extending the class from AbstractSpringSecuredCheck gives access to the Principal and the ```hasRole()``` method. If allowed, the mapping will proceed as normal. If not allowed, the mapping will not take place.

### Added
- Issue [#8](https://github.com/42BV/beanmapper-spring-boot-starter/issues/8), **Make sure BeanMapper bootstraps RoleSecuredCheck**; when applySecuredProperties is set to true (in the YML), register SpringRoleSecuredCheck, so that BeanMapper can properly deal with properties annotated with @BeanSecuredProperty.
- Issue [#29](https://github.com/42BV/beanmapper-spring/issues/29); **Spring Security based implementation for @RoleSecuredCheck** Added a Spring Security implementation for the @RoleSecuredCheck. It will compare the Principal's authorities against the required authorities. At least one match will suffice to grant access.
- Issue [#105](https://github.com/42BV/beanmapper/issues/105), **Ability to deal with @BeanSecuredProperty by delegating to a RoleSecuredCheck**; when a field is tagged as @BeanSecuredProperty, BeanMapper will query its attached RoleSecuredCheck. The handler will most likely be associated with a security implementation, such as Spring Security (not handled here). If no handler is present, access is granted by default.
- Issue [#106](https://github.com/42BV/beanmapper/issues/106), **When a @BeanSecuredProperty is found without a RoleSecuredCheck being set, throw an exception**; the absence of a RoleSecuredCheck is by default a reason to throw an exception when @BeanSecuredProperty is used anywhere within the application. 
- Issue [#107](https://github.com/42BV/beanmapper/issues/107), **Test for access by running against LogicSecuredCheck instance**; ability to add LogicSecuredCheck classes to BeanMapper's configuration. These classes can be called upon using the @BeanLogicSecured annotation. It allows for more complex interaction with the enveloping security system, such as not only checking against roles, but also comparing fields in the source or target against information known about the Principal.

## [2.3.1] - 2017-11-02
### Fixed
- Issue [#99](https://github.com/42BV/beanmapper/issues/99), **Collections in superclasses did not get their generic types read**; one of the attributes of getDeclaredField(field) is that it only works on the active class. The fix means that the superclasses will be checked for presence of the field. When found, it will call getDeclaredField on that class to get its generic type. Also, collection mapping instructions are not used when no collection element type can be determined; that is the one crucial element required for mapping collections. 
- Issue [#100](https://github.com/42BV/beanmapper/issues/100), **Collections.EmptySet can not have add() called on**; BeanCollectionUsage will check the canonical classname of the collection. If it starts with "java.util.Collections.", it will be tagged as reconstructable.

## [2.3.0] - 2017-11-02
### Added
- Issue [#92](https://github.com/42BV/beanmapper/issues/92), **BeanCollection no longer required for mapping collections**; BeanMapper is now capable of determining the collection element type of the target collection by examining the generic type. It will use this type as input for the mapping process. BeanCollection.elementType is no longer a required value. BeanMapper will merge collection information from target and source, giving preference to target information. 
- Issue [#97](https://github.com/42BV/beanmapper/issues/97), **StringToEnumConverter replaced with AnyToEnumConverter**; this makes it possible to convert enums to enums as well. Functionality is required because lists are no longer copied by reference by default. 

## [2.2.0] - 2017-11-01
### Fixed
- **BREAKING CHANGE** Issue [#93](https://github.com/42BV/beanmapper/issues/93), **config() not threadsafe**; it is possible for override configurations to be reused between threads, theoretically allowing fields to be changed before the map is called. This is not threadsafe. Now, nowhere is override configuration reused; it will always create a new override configuration. Both config() and wrapConfig() have been replaced with wrap(), which does the same as wrapConfig(). Internally, some OverrideConfiguration properties have been delegated to an OverrideField which takes care of returning the right value for a property. The clear() method has been removed; calling wrap automatically resets these properties (expect for downsize source/target, which are primarily used internally).
- Issue [#60](https://github.com/42BV/beanmapper/issues/60), **Unmatched BeanProperty did not throw an exception**; properties annotated with BeanProperty must match. If they do not, an exception must be thrown. Due to a bug, this did not always occur (only with BeanProperty on the target side). The current mechanism keep tabs on matched properties and does a final verification. If unmatched properties remain that should have been matched, an exception is thrown.
### Changed
- Issue [#89](https://github.com/42BV/beanmapper/issues/89), **Use sensible implementation for Set**; when a set is created an no preferredCollectionClass is passed, the handler will look at the collection element type. If the type is Comparable, it will return a TreeSet. If not, it will return a HashSet.
### Added
- **BREAKING CHANGE** Issue [#26](https://github.com/42BV/beanmapper-spring/issues/26), **Validations for the entity are not run for Lazy targets**; if the @MergedForm maps to a Lazy object, it delays the process of mapping until the time that get() is called on the Lazy object. At that time, it should work exactly the same way as a regular validation run. Forms are always validated right away. However, the final objects are only validated when direct mapping takes place. The process has been refactored so that validation on the final target is included in the delayed mapping process. Note that Lazy.get() must now deal with Exception. The pro of this approach is that it hooks onto the regular binding result handler.
- **BREAKING CHANGE** Issue [#90](https://github.com/42BV/beanmapper/issues/90), **Introduce a global flushEnabled, BeanCollection.flushAfterClear default true**; a global flushEnabled setting has been introduced, which is false by default. The BeanCollection.flushAfterClear has changed from a default false to true. BeanMapper asserts that both settings must be true before flushing. BeanMapper Spring [#28](https://github.com/42BV/beanmapper-spring/issues/28) sets flushEnabled=true when Lazy is used, because this offers the best chance of the EntityManager running in a transaction context. 

## [2.1.0] - 2017-10-25
### Fixed
- Issue [#83](https://github.com/42BV/beanmapper/issues/83), **The name field from an enum is not mapped to target field**; in the resolution of issue [#78](https://github.com/42BV/beanmapper/issues/78) the definition of getter fields has been tightened, because previously all private fields were tagged as available as well. One project made use of this loophole by reading the name field of an enumeration class to a String field. With the new fix this is no longer possible, since the name field is private. This fix makes an exception for the name field of an enum class. It will be considered available for reading.
### Added
- Issue [#6](https://github.com/42BV/beanmapper-spring-boot-starter/issues/6), **auto-register the JpaAfterClearFlusher**; when BeanMapper is in a JPA context, make sure the JpaAfterClearFlusher is registered with a valid EntityManager. This EM's flush can be called after a collection has been cleared. The result will be that the ORM is forced to execute its delete before its insert statement (the reverse of what would happen otherwise). Sample code:

```
@BeanCollection(elementType = ProjectSkill.class, flushAfterClear = true)
public List<ProjectSkillForm> skills;
```

## [2.0.2] - 2017-10-18
### Fixed
- Issue [#21](https://github.com/42BV/beanmapper-spring/issues/21), **Multipart part name not used correctly**; the multipart part name was not handled correctly. Spring's multipart handler is now passed a MethodParameter which has the correct part name and also disables the parameter name explorer, so it is forced to used the overwritten part name.

## [2.0.2] - 2017-10-18
### Fixed
- Issue [#21](https://github.com/42BV/beanmapper-spring/issues/21), **Multipart part name not used correctly**; the multipart part name was not handled correctly. Spring's multipart handler is now passed a MethodParameter which has the correct part name and also disables the parameter name explorer, so it is forced to used the overwritten part name.

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
