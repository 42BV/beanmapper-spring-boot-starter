package io.beanmapper.autoconfigure;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;


/**
 * Sets the annotated class up as a strict target. The source class is the beneficiary. Both classes are
 * registered with the BeanMapperBuilder with the target as strict. All properties in the target must
 * have matching properties in the source. If matches are missing, an exception will be thrown at startup
 * time, disrupting the boot process.
 */
@Target({ ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
public @interface BeanMapFromClass {

    /**
     * Source class and beneficiary of the target, ie the annotated class
     */
    Class<?> source();

}