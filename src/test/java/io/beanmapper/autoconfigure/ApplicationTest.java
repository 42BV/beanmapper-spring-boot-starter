package io.beanmapper.autoconfigure;

import io.beanmapper.BeanMapper;
import io.beanmapper.config.CoreConfiguration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest(classes = {BeanMapperAutoConfig.class, CoreConfiguration.class, BeanMapper.class})
public class ApplicationTest {



    @Autowired
    private BeanMapper beanMapper;

    @Test
    public void beanMapper_shouldBeAutowired() {
        assertNotNull(beanMapper);
    }

}
