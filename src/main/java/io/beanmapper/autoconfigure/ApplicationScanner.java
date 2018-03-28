package io.beanmapper.autoconfigure;

import static io.beanmapper.utils.Classes.forName;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.util.Collections;
import java.util.HashSet;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;

import io.beanmapper.annotations.LogicSecuredCheck;
import io.beanmapper.core.collections.CollectionHandler;
import io.beanmapper.core.converter.BeanConverter;
import io.beanmapper.utils.Classes;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScanner;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.classreading.MetadataReader;
import org.springframework.core.type.classreading.MetadataReaderFactory;
import org.springframework.core.type.filter.TypeFilter;

/**
 * Utility that helps searching for beans/classes within the application.
 */
class ApplicationScanner {

    private final Logger log = LoggerFactory.getLogger(ApplicationScanner.class);
    private final EntityScanner entityScanner;
    private final ClassPathScanningCandidateComponentProvider classpathScanner;

    ApplicationScanner(ApplicationContext context) {
        this.entityScanner = new EntityScanner(context);
        this.classpathScanner = new ClassPathScanningCandidateComponentProvider(
                false);
        this.classpathScanner.setEnvironment(context.getEnvironment());
        this.classpathScanner.setResourceLoader(context);
    }

    Optional<String> findApplicationPackage() {
        try {
            Set<Class<?>> appClasses = entityScanner.scan(SpringBootApplication.class);
            Class<?> appClass = appClasses.iterator().next();
            return Optional.of(appClass.getPackage().getName());
        } catch (ClassNotFoundException | NoSuchElementException e) {
            log.error("Cannot find class annotated with SpringBootApplication. ", e);
            return Optional.empty();
        }
    }

    Set<Class<?>> findBeanPairInstructions() {
        Set<Class<?>> foundAnnotations = findBeanPairInstructions(BeanMapFromClass.class);
        foundAnnotations.addAll(findBeanPairInstructions(BeanMapToClass.class));
        return foundAnnotations;
    }

    private Set<Class<?>> findBeanPairInstructions(Class<? extends Annotation> markerAnnotation) {
        try {
            return entityScanner.scan(markerAnnotation);
        } catch (ClassNotFoundException | NoSuchElementException e) {
            return Collections.emptySet();
        }
    }

    Set<Class<? extends BeanConverter>> findBeanConverterClasses(String basePackage) {
        return findClasses(basePackage, BeanConverter.class);
    }

    Set<Class<? extends CollectionHandler>> findCollectionHandlerClasses(String basePackage) {
        return findClasses(basePackage, CollectionHandler.class);
    }

    Set<Class<? extends LogicSecuredCheck>> findLogicSecuredCheckClasses(String basePackage) {
        return findClasses(basePackage, LogicSecuredCheck.class);
    }

    private <T> Set<Class<? extends T>> findClasses(String basePackage, Class<T> lookForClass) {
        Set<Class<? extends T>> converterClasses = new HashSet<>();
        classpathScanner.addIncludeFilter(createTypeFilterForClass(lookForClass));
        classpathScanner.findCandidateComponents(basePackage).forEach(
                bd -> converterClasses.add((Class<T>) forName(bd.getBeanClassName())));
        classpathScanner.resetFilters(false);
        return converterClasses;
    }

    private TypeFilter createTypeFilterForClass(Class<?> clazz) {
        return new TypeFilter() {
            @Override
            public boolean match(MetadataReader metadataReader, MetadataReaderFactory metadataReaderFactory) throws IOException {
                String className = metadataReader.getClassMetadata().getClassName();
                Class<?> currentClass = Classes.forName(className);
                return clazz.isAssignableFrom(currentClass);
            }
        };
    }

}
