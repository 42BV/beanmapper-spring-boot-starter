package io.beanmapper.autoconfigure;

import org.hibernate.proxy.HibernateProxy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.web.WebMvcAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.repository.core.EntityInformation;

import io.beanmapper.BeanMapper;
import io.beanmapper.config.BeanMapperBuilder;
import io.beanmapper.spring.converter.IdToEntityBeanConverter;
import io.beanmapper.spring.unproxy.HibernateAwareBeanUnproxy;

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
    private ApplicationContext applicationContext;

    @Bean
    public BeanMapper beanMapper() {
        return new BeanMapperBuilder()
                .addPackagePrefix(props.getPackagePrefix())
                .setBeanUnproxy(new HibernateAwareBeanUnproxy())
                .addProxySkipClass(Enum.class)
                .addConverter(new IdToEntityBeanConverter(applicationContext))
                .build();
    }
}
