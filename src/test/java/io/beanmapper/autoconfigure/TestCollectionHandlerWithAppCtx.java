package io.beanmapper.autoconfigure;

import io.beanmapper.BeanMapper;
import io.beanmapper.core.collections.AbstractCollectionHandler;

import org.springframework.context.ApplicationContext;

public class TestCollectionHandlerWithAppCtx extends AbstractCollectionHandler<TestEntity2> {

    private final ApplicationContext applicationContext;

    public TestCollectionHandlerWithAppCtx(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    public ApplicationContext getApplicationContext() {
        return applicationContext;
    }

    @Override
    protected void clear(TestEntity2 target) {}

    @Override
    protected TestEntity2 create() {
        return null;
    }

    @Override
    public TestEntity2 copy(BeanMapper beanMapper, Class collectionElementClass, TestEntity2 source, TestEntity2 target) {
        return null;
    }
}
