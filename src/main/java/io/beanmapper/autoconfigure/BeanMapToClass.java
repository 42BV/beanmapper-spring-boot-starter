package io.beanmapper.autoconfigure;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Sets the annotated class up as a strict source. The target class is the recipient. Both classes are
 * registered with the BeanMapperBuilder with the source as strict. All properties in the source must
 * have matching properties in the target. If matches are missing, an exception will be thrown at startup
 * time, disrupting the boot process.
 */
@Target({ ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
public @interface BeanMapToClass {

    /**
     * Target class and recipient of the source, ie the annotated class
     */
    Class<?> target();

}
