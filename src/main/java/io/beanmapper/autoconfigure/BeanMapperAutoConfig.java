package io.beanmapper.autoconfigure;

import io.beanmapper.BeanMapper;
import io.beanmapper.annotations.LogicSecuredCheck;
import io.beanmapper.config.BeanMapperBuilder;
import io.beanmapper.core.collections.CollectionHandler;
import io.beanmapper.core.converter.BeanConverter;
import io.beanmapper.spring.converter.IdToEntityBeanConverter;
import io.beanmapper.spring.flusher.JpaAfterClearFlusher;
import io.beanmapper.spring.security.SpringRoleSecuredCheck;
import io.beanmapper.spring.unproxy.HibernateAwareBeanUnproxy;
import io.beanmapper.spring.web.MergedFormMethodArgumentResolver;
import io.beanmapper.spring.web.converter.StructuredJsonMessageConverter;
import jakarta.annotation.Nonnull;
import jakarta.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanInstantiationException;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.web.servlet.WebMvcAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.util.ClassUtils;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

import static java.util.Collections.singletonList;
import static org.springframework.beans.BeanUtils.instantiateClass;

/**
 * In no BeanMapper bean is found, it will be created with sensible webapplication/spring-data-jpa mapping defaults.
 * It's possible to customize the BeanMapperBuilder by adding a bean of type {@link BeanMapperBuilderCustomizer}
 * to your configuration.
 * When a {@link MappingJackson2HttpMessageConverter} bean is found, a {@link MergedFormMethodArgumentResolver}
 * will be added to the Spring MVC context.
 */
@Configuration
@EnableAspectJAutoProxy
@AutoConfigureAfter(WebMvcAutoConfiguration.class)
@EnableConfigurationProperties(BeanMapperProperties.class)
public class BeanMapperAutoConfig {

    private final Logger log = LoggerFactory.getLogger(BeanMapperAutoConfig.class);
    private final BeanMapperProperties props;
    private final ApplicationContext applicationContext;
    private final BeanMapperBuilderCustomizer builderCustomizer;

    private ApplicationScanner collectionHandlerAppScanner;
    private ApplicationScanner beanConverterAppScanner;

    public BeanMapperAutoConfig(final BeanMapperProperties props, final ApplicationContext applicationContext, @Autowired(required = false) final BeanMapperBuilderCustomizer builderCustomizer) {
        this.props = props;
        this.applicationContext = applicationContext;
        this.builderCustomizer = builderCustomizer;
    }

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
    public BeanMapper beanMapper() {
        String packagePrefix = determinePackagePrefix();
        BeanMapperBuilder builder = new BeanMapperBuilder()
                .setApplyStrictMappingConvention(props.getApplyStrictMappingConvention())
                .setStrictSourceSuffix(props.getStrictSourceSuffix())
                .setStrictTargetSuffix(props.getStrictTargetSuffix())
                .addPackagePrefix(packagePrefix);

        if (isSpringDataJpaOnClasspath()) {
                builder.addConverter(new IdToEntityBeanConverter(applicationContext));
        } else {
            log.info("Spring Data JPA is not present on the classpath. BeanMapper's IdToEntityBeanConverter will not be activated");
        }

        addCollectionHandlers(builder, packagePrefix);
        addCustomConverters(builder, packagePrefix);
        addCustomBeanPairs(builder);

        if (isSpringDataJpaOnClasspath()) {
            addAfterClearFlusher(builder);
        } else {
            log.info("Spring Data JPA is not present on the classpath. BeanMapper's afterClearFlusher will not be activated");
        }

        if (isSpringSecurityOnClasspath()) {
            setSecuredChecks(builder, packagePrefix);
        } else {
            log.info("Spring Security is not present on the classpath. BeanMapper's @BeanLogicSecured and @BeanRoleSecured annotations will not be processed.");
        }

        setUnproxy(builder);
        customize(builder);
        BeanMapper beanMapper = builder.build();
        if (props.getDiagnosticsDetailLevel().isEnabled()) {
            beanMapper = beanMapper.wrap(props.getDiagnosticsDetailLevel()).build();
        }
        return beanMapper;
    }

    private void setSecuredChecks(BeanMapperBuilder builder, String packagePrefix) {
        if (!props.getApplySecuredProperties()) {
            return;
        }
        addLogicSecuredChecks(builder, packagePrefix);
        builder.setSecuredPropertyHandler(new SpringRoleSecuredCheck());
    }

    private void addAfterClearFlusher(BeanMapperBuilder builder) {
        jakarta.persistence.EntityManager entityManager;

        try {
            entityManager = applicationContext.getBean(jakarta.persistence.EntityManager.class);
        } catch (NoSuchBeanDefinitionException e) {
            log.warn("No EntityManager bean has been configured within your application. BeanMapper's afterClearFlusher can not be activated.");
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
            try {
                if (beanMapToClass != null) {
                    builder.addBeanPairWithStrictSource(cls, beanMapToClass.target());
                } else if (beanMapFromClass != null) {
                    builder.addBeanPairWithStrictTarget(beanMapFromClass.source(), cls);
                }
            } catch (Exception e) {
                String annotationType = beanMapToClass != null ? "@BeanMapToClass" : "@BeanMapFromClass";
                String sourceClass = beanMapToClass != null ? cls.getSimpleName() : beanMapFromClass.source().getSimpleName();
                String targetClass = beanMapToClass != null ? beanMapToClass.target().getSimpleName() : cls.getSimpleName();
                
                String detailedMessage = String.format(
                    "Failed to configure BeanMapper strict mapping for %s annotation. " +
                    "Source class: %s, Target class: %s. " +
                    "This typically indicates a property mismatch between the classes. " +
                    "Ensure all properties in the target class have corresponding properties in the source class. " +
                    "Original error: %s", 
                    annotationType, sourceClass, targetClass, 
                    e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName()
                );
                
                throw new RuntimeException(detailedMessage, e);
            }
        });
    }

    private void addLogicSecuredChecks(BeanMapperBuilder builder, String basePackage) {
        collectionHandlerAppScanner.findLogicSecuredCheckClasses(basePackage).forEach(cls -> {
            LogicSecuredCheck<?, ?> logicSecuredCheck = instantiateClassAppContextOptional(cls, "logic secured check");
            if (logicSecuredCheck == null) {
                try {
                    logicSecuredCheck = this.applicationContext.getBean(cls);
                } catch (BeansException e) {
                    log.error("Could not instantiate class [{}] as Spring Bean.", cls.getName());
                    return;
                }
            }
            builder.addLogicSecuredCheck(logicSecuredCheck);
        });
    }

    private void addCollectionHandlers(BeanMapperBuilder builder, String basePackage) {
        collectionHandlerAppScanner.findCollectionHandlerClasses(basePackage).forEach(cls -> {
            CollectionHandler<?> collectionHandler = instantiateClassAppContextOptional(cls, "collection handler");
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
            T created = instantiateClass(cls);
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
            if (isHibernateOnClasspath()) {
                builder.setBeanUnproxy(new HibernateAwareBeanUnproxy());
                log.info("Set HibernateAwareUnproxy as bean unproxy mechanism.");
            } else {
                log.warn("use-hibernate-unproxy was set to true, but no Hibernate / Spring Data JPA was found on your classpath. Did you perhaps forget to include spring-boot-start-data-jpa in your project?");
            }

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
    @ConditionalOnClass({ org.springframework.data.repository.core.EntityInformation.class })
    static class MergedFormConfig implements WebMvcConfigurer {

        private final Logger log = LoggerFactory.getLogger(MergedFormConfig.class);
        private final MappingJackson2HttpMessageConverter mappingJackson2HttpMessageConverter;
        private final BeanMapper beanMapper;
        private final ApplicationContext applicationContext;
        private final jakarta.persistence.EntityManager entityManager;

        public MergedFormConfig(@Autowired(required = false) final MappingJackson2HttpMessageConverter mappingJackson2HttpMessageConverter,
                final BeanMapper beanMapper, final ApplicationContext applicationContext, @Autowired(required = false) final jakarta.persistence.EntityManager entityManager) {
            this.mappingJackson2HttpMessageConverter = mappingJackson2HttpMessageConverter;
            this.beanMapper = beanMapper;
            this.applicationContext = applicationContext;
            this.entityManager = entityManager;
        }

        /**
         * If a {@link MappingJackson2HttpMessageConverter} bean is found, adds a {@link MergedFormMethodArgumentResolver} to the Spring MVC context.
         */
        @Override
        public void addArgumentResolvers(@Nonnull List<HandlerMethodArgumentResolver> argumentResolvers) {
            if (mappingJackson2HttpMessageConverter != null) {
                log.info("Adding MergedFormArgumentResolver to MVC application.");
                argumentResolvers.add(new MergedFormMethodArgumentResolver(
                        singletonList(new StructuredJsonMessageConverter(mappingJackson2HttpMessageConverter)),
                        beanMapper,
                        applicationContext,
                        entityManager));
            } else {
                log.warn("No MergedFormArgumentResolver added to MVC application because no MappingJackson2HttpMessageConverter bean found!");
            }
        }
    }

    private boolean isSpringDataJpaOnClasspath() {
        return ClassUtils.isPresent("jakarta.persistence.EntityManager", applicationContext.getClassLoader());
    }

    private boolean isHibernateOnClasspath() {
        return ClassUtils.isPresent("org.hibernate.proxy.HibernateProxy", applicationContext.getClassLoader());
    }

    private boolean isSpringSecurityOnClasspath() {
        return ClassUtils.isPresent("org.springframework.security.authentication.AuthenticationManager", applicationContext.getClassLoader());
    }

}
