package io.beanmapper.autoconfigure;

import static java.util.stream.Collectors.toList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.springframework.test.util.ReflectionTestUtils.getField;

import java.util.List;

import javax.persistence.EntityManager;

import io.beanmapper.BeanMapper;
import io.beanmapper.config.BeanMapperBuilder;
import io.beanmapper.core.BeanPropertyMatch;
import io.beanmapper.core.collections.CollectionHandler;
import io.beanmapper.core.converter.BeanConverter;
import io.beanmapper.core.unproxy.BeanUnproxy;
import io.beanmapper.core.unproxy.DefaultBeanUnproxy;
import io.beanmapper.spring.converter.IdToEntityBeanConverter;
import io.beanmapper.spring.security.SpringRoleSecuredCheck;
import io.beanmapper.spring.unproxy.HibernateAwareBeanUnproxy;
import io.beanmapper.spring.web.MergedFormMethodArgumentResolver;

import org.junit.After;
import org.junit.Test;
import org.springframework.boot.autoconfigure.http.HttpMessageConvertersAutoConfiguration;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.WebMvcAutoConfiguration;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mock.web.MockServletContext;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter;

public class BeanMapperAutoConfigTest {

    private static final String BEANMAPPER_PACKAGE_PREFIX_PROP = "beanmapper.package-prefix=io.beanmapper.autoconfigure";
    private static final String BEANMAPPER_USE_HIBERNATE_UNPROXY_PROP = "beanmapper.use-hibernate-unproxy=false";
    private AnnotationConfigWebApplicationContext context;

    @After
    public void tearDown() {
        if (this.context != null) {
            this.context.close();
        }
    }

    @Test
    public void autoconfig_shouldCreateBeanMapper_ifNotExists() {
        loadApplicationContext();
        assertBeanMapper(1, 17);
        assertMergedFormArgResolver();
    }

    @Test
    public void autoconfig_shouldCreateCustomizedBeanMapper_ifNotExists() {
        loadApplicationContext(ConfigWithBeanMapperBuilderCustomizer.class);
        assertBeanMapper(1, 18);
        assertMergedFormArgResolver();
    }

    @Test
    public void autoconfig_shouldNotCreateBeanMapper_ifAlreadyExists() {
        loadApplicationContext(ConfigWithBeanMapper.class);
        assertBeanMapper(0, 12, false);
        assertMergedFormArgResolver();
    }

    @Test
    public void autoconfig_shouldCreateBeanMapper_withDefaultUnproxy_whenEnvIsSet() {
        loadApplicationContext(BEANMAPPER_USE_HIBERNATE_UNPROXY_PROP);
        assertBeanMapper(1, 17, false);
        assertMergedFormArgResolver();
    }

    @Test
    public void autoconfig_shouldRegisterCollectionHandler_ifScanned() {
        loadApplicationContext(BEANMAPPER_USE_HIBERNATE_UNPROXY_PROP);
        BeanMapper mapper = context.getBean(BeanMapper.class);
        List<CollectionHandler> customCollectionHandlers = mapper.configuration().getCollectionHandlers()
                .stream()
                .filter(handler ->
                    handler.getType().equals(TestEntity.class) ||
                    handler.getType().equals(TestEntity2.class))
                .collect(toList());
        assertEquals(2, customCollectionHandlers.size());
        TestCollectionHandlerWithAppCtx collectionHandler = (TestCollectionHandlerWithAppCtx)mapper.configuration().getCollectionHandlerFor(TestEntity2.class);
        assertNotNull(collectionHandler.getApplicationContext());
    }

    @Test
    public void autoconfig_shouldSetSecurityChecks() {
        loadApplicationContext(ConfigWithSecurity.class);
        BeanMapper mapper = context.getBean(BeanMapper.class);

        assertTrue(mapper.configuration().getRoleSecuredCheck() instanceof SpringRoleSecuredCheck);
    }



    @Test
    public void autoconfig_shouldNotSetSecurityChecks_ifSpringSecurityIsMissingFromClassPath() {
        loadApplicationContext(ConfigWithSecurity.class, new NoSpringSecurityClassLoader());
        BeanMapper mapper = context.getBean(BeanMapper.class);

        assertFalse(mapper.configuration().getRoleSecuredCheck() instanceof SpringRoleSecuredCheck);
    }

    @Test
    public void autoconfig_shouldLoadIdToEntityBeanConverterAndHibernateUnproxy_withSpringDataOnClassPath() {
        loadApplicationContext(ConfigWithSpringData.class);
        BeanMapper mapper = context.getBean(BeanMapper.class);
        List<BeanConverter> beanConverters = mapper.configuration().getBeanConverters();
        assertTrue(beanConverters.stream().anyMatch(c -> c instanceof IdToEntityBeanConverter));

        BeanUnproxy unproxyDelegate = (BeanUnproxy) getField(mapper.configuration().getBeanUnproxy(), "delegate");
        assertTrue(unproxyDelegate instanceof HibernateAwareBeanUnproxy);
    }

    @Test
    public void autoconfig_shouldNotLoadIdToEntityBeanConverterAndHibernateUnproxy_withoutSpringDataOnClassPath() {
        loadApplicationContext(ConfigWithSpringData.class, new NoSpringDataClassLoader());
        BeanMapper mapper = context.getBean(BeanMapper.class);
        List<BeanConverter> beanConverters = mapper.configuration().getBeanConverters();
        assertFalse(beanConverters.stream().anyMatch(c -> c instanceof IdToEntityBeanConverter));

        BeanUnproxy unproxyDelegate = (BeanUnproxy) getField(mapper.configuration().getBeanUnproxy(), "delegate");
        assertFalse(unproxyDelegate instanceof HibernateAwareBeanUnproxy);
        assertTrue(unproxyDelegate instanceof DefaultBeanUnproxy);
    }

    @Configuration
    static class ConfigWithSpringData {

        @Bean
        public EntityManager entityManager() {
            return new NoOpEntityManager();
        }
    }

    @Configuration
    static class ConfigWithSecurity {

        @Bean
        public AuthenticationManager authenticationManager() {
            return authentication -> {
                throw new UnsupportedOperationException("Stub!");
            };
        }
    }

    @Configuration
    static class ConfigWithBeanMapper {

        @Bean
        public BeanMapper beanMapper() {
            return new BeanMapperBuilder().build();
        }

    }

    @Configuration
    static class ConfigWithBeanMapperBuilderCustomizer {

        @Bean
        public BeanMapperBuilderCustomizer beanMapperCustomizer() {
            return builder -> {
                builder.addConverter(new BeanConverter() {

                    @Override
                    public Object convert(BeanMapper beanMapper, Object source, Class<?> targetClass, BeanPropertyMatch beanFieldMatch) {
                        return null;
                    }

                    @Override
                    public boolean match(Class<?> sourceClass, Class<?> targetClass) {
                        return false;
                    }

                });
            };
        }

    }

    private void assertBeanMapper(int expectedNumberOfPackagePrefixes, int expectedNumberOfConverters) {
        assertBeanMapper(expectedNumberOfPackagePrefixes, expectedNumberOfConverters, true);
    }

    private void assertBeanMapper(int expectedNumberOfPackagePrefixes, int expectedNumberOfConverters, boolean hibernateUnproxy) {
        BeanMapper mapper = context.getBean(BeanMapper.class);
        io.beanmapper.config.Configuration config = mapper.configuration();
        assertEquals(expectedNumberOfPackagePrefixes, config.getPackagePrefixes().size());
        if (expectedNumberOfPackagePrefixes == 1) {
            assertEquals("io.beanmapper.autoconfigure", config.getPackagePrefixes().get(0));
        }
        assertEquals(expectedNumberOfConverters, config.getBeanConverters().size());
        BeanUnproxy unproxyDelegate = (BeanUnproxy) getField(config.getBeanUnproxy(), "delegate");
        if (hibernateUnproxy) {
            assertEquals(HibernateAwareBeanUnproxy.class, unproxyDelegate.getClass());
        } else {
            assertEquals(DefaultBeanUnproxy.class, unproxyDelegate.getClass());
        }
    }

    private void assertMergedFormArgResolver() {
        RequestMappingHandlerAdapter requestHandler = context.getBean(RequestMappingHandlerAdapter.class);
        List<HandlerMethodArgumentResolver> argResolvers = requestHandler.getArgumentResolvers()
                .stream()
                .filter(argResolver -> MergedFormMethodArgumentResolver.class == argResolver.getClass())
                .collect(toList());
        assertEquals(1, argResolvers.size());
    }

    private void loadApplicationContext(String... env) {
        loadApplicationContext(null, env);
    }

    private void loadApplicationContext(Class<?> config, String... env) {
        loadApplicationContext(config, null, env);
    }

    private void loadApplicationContext(Class<?> config, ClassLoader classLoader, String... env) {
        AnnotationConfigWebApplicationContext applicationContext = new AnnotationConfigWebApplicationContext();

        if (classLoader != null) {
            applicationContext.setClassLoader(classLoader);
        }

        TestPropertyValues.of(env).applyTo(applicationContext);
        TestPropertyValues.of(BEANMAPPER_PACKAGE_PREFIX_PROP).applyTo(applicationContext);
        if (config != null) {
            applicationContext.register(config);
        }
        applicationContext.register(
                WebMvcAutoConfiguration.class,
                JacksonAutoConfiguration.class,
                HttpMessageConvertersAutoConfiguration.class,
                BeanMapperAutoConfig.class);
        applicationContext.setServletContext(new MockServletContext());
        applicationContext.refresh();
        this.context = applicationContext;
    }
}