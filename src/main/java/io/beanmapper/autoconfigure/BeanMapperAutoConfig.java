package io.beanmapper.autoconfigure;

import static java.util.Collections.singletonList;

import java.util.List;

import org.hibernate.proxy.HibernateProxy;
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

import io.beanmapper.BeanMapper;
import io.beanmapper.config.BeanMapperBuilder;
import io.beanmapper.spring.converter.IdToEntityBeanConverter;
import io.beanmapper.spring.unproxy.HibernateAwareBeanUnproxy;
import io.beanmapper.spring.web.MergedFormMethodArgumentResolver;
import io.beanmapper.spring.web.converter.StructuredJsonMessageConverter;

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

    @Autowired
    private BeanMapperProperties props;
    @Autowired
    private ApplicationContext applicationContext;
    @Autowired(required = false)
    private BeanMapperBuilderCustomizer builderCustomizer;

    /**
     * Creates a {@link BeanMapper} bean with spring-data-jpa defaults.
     * If a {@link BeanMapperBuilderCustomizer} bean is found, uses this to 
     * customize the builder before the {@link BeanMapper} is build.
     * @return BeanMapper
     */
    @Bean
    @ConditionalOnMissingBean(BeanMapper.class)
    @ConditionalOnClass({ HibernateProxy.class, EntityInformation.class })
    public BeanMapper beanMapper() {
        BeanMapperBuilder builder = new BeanMapperBuilder()
                .setBeanUnproxy(new HibernateAwareBeanUnproxy())
                .addProxySkipClass(Enum.class)
                .addConverter(new IdToEntityBeanConverter(applicationContext));
        if (props.getPackagePrefix() != null) {
            builder.addPackagePrefix(props.getPackagePrefix());
        }
        if (builderCustomizer != null) {
            builderCustomizer.customize(builder);
        }
        return builder.build();
    }

    @Configuration
    @ConditionalOnWebApplication
    static class MergedFormConfig extends WebMvcConfigurerAdapter {

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
                argumentResolvers.add(new MergedFormMethodArgumentResolver(
                        singletonList(new StructuredJsonMessageConverter(mappingJackson2HttpMessageConverter)),
                        beanMapper,
                        applicationContext));
            }
        }
    }
}
