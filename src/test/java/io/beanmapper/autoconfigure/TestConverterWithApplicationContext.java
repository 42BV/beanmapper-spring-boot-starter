package io.beanmapper.autoconfigure;

import io.beanmapper.core.converter.SimpleBeanConverter;

import org.springframework.context.ApplicationContext;

public class TestConverterWithApplicationContext extends SimpleBeanConverter<String, Long> {

    public TestConverterWithApplicationContext(ApplicationContext appContext) {

    }

    @Override
    protected Long doConvert(String source) {
        return Long.parseLong(source);
    }

}
