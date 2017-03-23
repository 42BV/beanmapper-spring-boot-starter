package io.beanmapper.autoconfigure;

import io.beanmapper.core.converter.SimpleBeanConverter;

public class TestConverter extends SimpleBeanConverter<String, Long> {

    @Override
    protected Long doConvert(String source) {
        return Long.parseLong(source);
    }

}
