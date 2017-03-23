package io.beanmapper.autoconfigure;

import static io.beanmapper.utils.Classes.forName;

import java.io.IOException;
import java.util.HashSet;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScanner;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.classreading.MetadataReader;
import org.springframework.core.type.classreading.MetadataReaderFactory;
import org.springframework.core.type.filter.TypeFilter;

import io.beanmapper.core.converter.BeanConverter;
import io.beanmapper.utils.Classes;

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

    Set<Class<? extends BeanConverter>> findBeanConverterClasses(String basePackage) {
        Set<Class<? extends BeanConverter>> converterClasses = new HashSet<>();
        classpathScanner.addIncludeFilter(new TypeFilter() {
            @Override
            public boolean match(MetadataReader metadataReader, MetadataReaderFactory metadataReaderFactory) throws IOException {
                String className = metadataReader.getClassMetadata().getClassName();
                Class<?> clazz = Classes.forName(className);
                return BeanConverter.class.isAssignableFrom(clazz);
            }
        });
        classpathScanner.findCandidateComponents(basePackage).forEach(
                bd -> converterClasses.add((Class<? extends BeanConverter>) forName(bd.getBeanClassName())));
        return converterClasses;
    }
}
