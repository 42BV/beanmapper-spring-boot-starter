package io.beanmapper.autoconfigure;

import org.springframework.context.ApplicationContext;

import io.beanmapper.core.converter.SimpleBeanConverter;

public class TestConverterWithApplicationContext extends SimpleBeanConverter<String, Long> {

    public TestConverterWithApplicationContext(ApplicationContext appContext) {

    }

    @Override
    protected Long doConvert(String source) {
        return Long.parseLong(source);
    }

}
