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

    /**
     * Tell the beanmapper to use Hibernate unproxy mechanism.
     */
    private boolean useHibernateUnproxy = true;

    private Boolean applyStrictMappingConvention = true;

    private String strictSourceSuffix = "Form";

    private String strictTargetSuffix = "Result";

    public boolean isUseHibernateUnproxy() {
        return useHibernateUnproxy;
    }

    public void setUseHibernateUnproxy(boolean hibernateUnproxy) {
        this.useHibernateUnproxy = hibernateUnproxy;
    }

    public String getPackagePrefix() {
        return packagePrefix;
    }

    public void setPackagePrefix(String basePackageName) {
        this.packagePrefix = basePackageName;
    }

    public Boolean isApplyStrictMappingConvention() {
        return applyStrictMappingConvention;
    }

    public void setApplyStrictMappingConvention(Boolean applyStrictMappingConvention) {
        this.applyStrictMappingConvention = applyStrictMappingConvention;
    }

    public String getStrictSourceSuffix() {
        return strictSourceSuffix;
    }

    public void setStrictSourceSuffix(String strictSourceSuffix) {
        this.strictSourceSuffix = strictSourceSuffix;
    }

    public String getStrictTargetSuffix() {
        return strictTargetSuffix;
    }

    public void setStrictTargetSuffix(String strictTargetSuffix) {
        this.strictTargetSuffix = strictTargetSuffix;
    }
}
