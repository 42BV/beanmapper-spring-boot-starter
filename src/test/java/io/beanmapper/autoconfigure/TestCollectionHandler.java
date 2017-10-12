package io.beanmapper.autoconfigure;

import io.beanmapper.BeanMapper;
import io.beanmapper.core.collections.AbstractCollectionHandler;

public class TestCollectionHandler extends AbstractCollectionHandler<TestEntity> {

    @Override
    protected void clear(TestEntity target) {}

    @Override
    protected TestEntity create() {
        return null;
    }

    @Override
    public TestEntity copy(BeanMapper beanMapper, Class collectionElementClass, TestEntity source, TestEntity target) {
        return null;
    }

}
