package io.beanmapper.autoconfigure;

import java.util.Collections;
import java.util.List;

import org.hibernate.proxy.HibernateProxy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
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

@Configuration
@ConditionalOnMissingBean(BeanMapper.class)
@ConditionalOnProperty("beanmapper.packagePrefix")
@ConditionalOnClass({ HibernateProxy.class, EntityInformation.class })
@AutoConfigureAfter(WebMvcAutoConfiguration.class)
@EnableConfigurationProperties(BeanMapperProperties.class)
public class BeanMapperAutoConfig {

    @Autowired
    private BeanMapperProperties props;
    @Autowired
    public ApplicationContext applicationContext;

    @Bean
    public BeanMapper beanMapper() {
        return new BeanMapperBuilder()
                .addPackagePrefix(props.getPackagePrefix())
                .setBeanUnproxy(new HibernateAwareBeanUnproxy())
                .addProxySkipClass(Enum.class)
                .addConverter(new IdToEntityBeanConverter(applicationContext))
                // add extra converters
                .build();
    }

    @Configuration
    @ConditionalOnWebApplication
    static class MergedFormWebConfig extends WebMvcConfigurerAdapter {

        @Autowired
        private MappingJackson2HttpMessageConverter mappingJackson2HttpMessageConverter;
        @Autowired
        private BeanMapper beanMapper;
        @Autowired
        private ApplicationContext applicationContext;

        @Override
        public void addArgumentResolvers(List<HandlerMethodArgumentResolver> argumentResolvers) {
            argumentResolvers.add(new MergedFormMethodArgumentResolver(
                    Collections.singletonList(new StructuredJsonMessageConverter(mappingJackson2HttpMessageConverter)),
                    beanMapper,
                    applicationContext));
        }
    }
}
