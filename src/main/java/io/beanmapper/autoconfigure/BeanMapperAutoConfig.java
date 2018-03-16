package io.beanmapper.autoconfigure;

import static java.util.Collections.singletonList;
import static org.springframework.beans.BeanUtils.instantiate;
import static org.springframework.beans.BeanUtils.instantiateClass;

import java.util.List;

import javax.annotation.PostConstruct;
import javax.persistence.EntityManager;

import io.beanmapper.BeanMapper;
import io.beanmapper.config.BeanMapperBuilder;
import io.beanmapper.core.collections.CollectionHandler;
import io.beanmapper.core.converter.BeanConverter;
import io.beanmapper.spring.converter.IdToEntityBeanConverter;
import io.beanmapper.spring.flusher.JpaAfterClearFlusher;
import io.beanmapper.spring.security.SpringSecuredPropertyHandler;
import io.beanmapper.spring.unproxy.HibernateAwareBeanUnproxy;
import io.beanmapper.spring.web.MergedFormMethodArgumentResolver;
import io.beanmapper.spring.web.converter.StructuredJsonMessageConverter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanInstantiationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.web.WebMvcAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.repository.core.EntityInformation;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;

/**
 * In no BeanMapper bean is found, it will be created with sensible webapplication/spring-data-jpa mapping defaults.
 * It's possible to customize the BeanMapperBuilder by adding a bean of type {@link BeanMapperBuilderCustomizer} 
 * to your configuration.
 * When a {@link MappingJackson2HttpMessageConverter} bean is found, a {@link MergedFormMethodArgumentResolver} 
 * will be added to the Spring MVC context.
 */
@Configuration
@AutoConfigureAfter(WebMvcAutoConfiguration.class)
@EnableConfigurationProperties(BeanMapperProperties.class)
public class BeanMapperAutoConfig {

    private final Logger log = LoggerFactory.getLogger(BeanMapperAutoConfig.class);
    @Autowired
    private BeanMapperProperties props;
    @Autowired
    private ApplicationContext applicationContext;
    @Autowired(required = false)
    private BeanMapperBuilderCustomizer builderCustomizer;
    @Autowired(required = false)
    private EntityManager entityManager;

    private ApplicationScanner collectionHandlerAppScanner;
    private ApplicationScanner beanConverterAppScanner;

    @PostConstruct
    private void initApplicationScanner() {
        collectionHandlerAppScanner = new ApplicationScanner(applicationContext);
        beanConverterAppScanner = new ApplicationScanner(applicationContext);
    }

    /**
     * Creates a {@link BeanMapper} bean with spring-data-jpa defaults.
     * If a {@link BeanMapperBuilderCustomizer} bean is found, uses this to 
     * customize the builder before the {@link BeanMapper} is build.
     * @return BeanMapper
     */
    @Bean
    @ConditionalOnMissingBean(BeanMapper.class)
    @ConditionalOnClass({ EntityInformation.class })
    public BeanMapper beanMapper() {
        String packagePrefix = determinePackagePrefix();
        BeanMapperBuilder builder = new BeanMapperBuilder()
                .setApplyStrictMappingConvention(props.getApplyStrictMappingConvention())
                .setStrictSourceSuffix(props.getStrictSourceSuffix())
                .setStrictTargetSuffix(props.getStrictTargetSuffix())
                .addPackagePrefix(packagePrefix)
                .addConverter(new IdToEntityBeanConverter(applicationContext));
        addCollectionHandlers(builder, packagePrefix);
        addCustomConverters(builder, packagePrefix);
        addCustomBeanPairs(builder);
        addAfterClearFlusher(builder);
        setSecuredPropertyHandler(builder);
        setUnproxy(builder);
        customize(builder);
        return builder.build();
    }

    private void setSecuredPropertyHandler(BeanMapperBuilder builder) {
        if (!props.getApplySecuredProperties()) {
            return;
        }
        builder.setSecuredPropertyHandler(new SpringSecuredPropertyHandler());
    }

    private void addAfterClearFlusher(BeanMapperBuilder builder) {
        if (entityManager == null) {
            return;
        }
        builder.addAfterClearFlusher(new JpaAfterClearFlusher(entityManager));
    }

    private String determinePackagePrefix() {
        String packagePrefix = props.getPackagePrefix();
        if (packagePrefix == null) {
            log.info("No beanmapper.package-prefix found in environment properties, "
                    + "defaulting to SpringBootApplication annotated class package.");
            packagePrefix = beanConverterAppScanner.findApplicationPackage()
                    .orElseThrow(() -> new RuntimeException(
                            "Application package not found, define beanmapper.package-prefix property in your environment!"));
        }
        log.info("Set beanmapper packagePrefix [{}]", packagePrefix);
        return packagePrefix;
    }

    private void addCustomBeanPairs(BeanMapperBuilder builder) {
        beanConverterAppScanner.findBeanPairInstructions().forEach(cls -> {
            BeanMapToClass beanMapToClass = cls.getDeclaredAnnotation(BeanMapToClass.class);
            BeanMapFromClass beanMapFromClass = cls.getDeclaredAnnotation(BeanMapFromClass.class);
            if (beanMapToClass != null) {
                builder.addBeanPairWithStrictSource(cls, beanMapToClass.target());
            } else if (beanMapFromClass != null) {
                builder.addBeanPairWithStrictTarget(beanMapFromClass.source(), cls);
            }
        });
    }

    private void addCollectionHandlers(BeanMapperBuilder builder, String basePackage) {
        collectionHandlerAppScanner.findCollectionHandlerClasses(basePackage).forEach(cls -> {
            CollectionHandler collectionHandler = instantiateClassAppContextOptional(cls, "collection handler");
            if (collectionHandler != null) {
                builder.addCollectionHandler(collectionHandler);
            }
        });
    }

    private void addCustomConverters(BeanMapperBuilder builder, String basePackage) {
        beanConverterAppScanner.findBeanConverterClasses(basePackage).forEach(cls -> {
            BeanConverter converter = instantiateClassAppContextOptional(cls,"bean converter");
            if (converter != null) {
                builder.addConverter(converter);
            }
        });
    }

    private <T> T instantiateClassAppContextOptional(Class<T> cls, String label) {

        log.info("Found {} candidate class [{}], now trying to instantiate...", label, cls);
        try {
            T created = instantiate(cls);
            log.info("Added [{}] [{}] to bean mapper.", label, cls);
            return created;
        } catch (BeanInstantiationException e) {
            log.debug("Cannot instantiate bean of class [{}] with no-arg constructor, now trying appContext constructor...", cls);
            try {
                T created = instantiateClass(cls.getConstructor(ApplicationContext.class), applicationContext);
                log.info("Added [{}] [{}] to bean mapper.", label, cls);
                return created;
            } catch (BeanInstantiationException | NoSuchMethodException | SecurityException ex) {
                log.warn("Cannot instantiate bean of class [{}] with applicationContext constructor, this [{}] will be skipped!", cls, label);
            }
        }
        return null;
    }

    private void setUnproxy(BeanMapperBuilder builder) {
        if (props.isUseHibernateUnproxy()) {
            builder.setBeanUnproxy(new HibernateAwareBeanUnproxy());
            log.info("Set HibernateAwareUnproxy as bean unproxy mechanism.");
        } else {
            log.info("use-hibernate-unproxy set to false, keeping default unproxy mechanism.");
        }
    }

    private void customize(BeanMapperBuilder builder) {
        if (builderCustomizer != null) {
            log.info("Customizing BeanMapperBuilder...");
            builderCustomizer.customize(builder);
        }
    }

    @Configuration
    @ConditionalOnWebApplication
    @ConditionalOnClass({ EntityInformation.class })
    static class MergedFormConfig extends WebMvcConfigurerAdapter {

        private final Logger log = LoggerFactory.getLogger(MergedFormConfig.class);
        @Autowired(required = false)
        private MappingJackson2HttpMessageConverter mappingJackson2HttpMessageConverter;
        @Autowired
        private BeanMapper beanMapper;
        @Autowired
        private ApplicationContext applicationContext;

        /**
         * If a {@link MappingJackson2HttpMessageConverter} bean is found, adds a {@link MergedFormMethodArgumentResolver} to the Spring MVC context.
         */
        @Override
        public void addArgumentResolvers(List<HandlerMethodArgumentResolver> argumentResolvers) {
            if (mappingJackson2HttpMessageConverter != null) {
                log.info("Adding MergedFormArgumentResolver to MVC application.");
                argumentResolvers.add(new MergedFormMethodArgumentResolver(
                        singletonList(new StructuredJsonMessageConverter(mappingJackson2HttpMessageConverter)),
                        beanMapper,
                        applicationContext));
            } else {
                log.warn("No MergedFormArgumentResolver added to MVC application because no MappingJackson2HttpMessageConverter bean found!");
            }
        }
    }
}
