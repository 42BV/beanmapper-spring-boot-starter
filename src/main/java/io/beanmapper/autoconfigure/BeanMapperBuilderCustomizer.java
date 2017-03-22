package io.beanmapper.autoconfigure;

import io.beanmapper.config.BeanMapperBuilder;

public interface BeanMapperBuilderCustomizer {

    void customize(BeanMapperBuilder builder);
}
