package io.beanmapper.autoconfigure;

import io.beanmapper.BeanMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
public class ApplicationTest {

    @Autowired
    private BeanMapper beanMapper;

    @Test
    public void beanMapper_shouldBeAutowired() {
        assertNotNull(beanMapper);
    }

}
