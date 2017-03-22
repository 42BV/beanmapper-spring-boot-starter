package io.beanmapper.autoconfigure;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Properties for the {@link BeanMapperAutoConfig}
 */
@ConfigurationProperties("beanmapper")
public class BeanMapperProperties {

    /**
     * All classes in that package and sub-packages are
     * eligible for mapping. The root source and target do not need to be set as such, because
     * the verification is only run against nested classes which should be mapped implicity as
     * well
     */
    private String packagePrefix;

    public String getPackagePrefix() {
        return packagePrefix;
    }

    public void setPackagePrefix(String basePackageName) {
        this.packagePrefix = basePackageName;
    }

}
