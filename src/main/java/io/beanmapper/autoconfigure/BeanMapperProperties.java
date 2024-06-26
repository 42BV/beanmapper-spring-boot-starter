package io.beanmapper.autoconfigure;

import io.beanmapper.utils.diagnostics.DiagnosticsDetailLevel;

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

    private boolean applyStrictMappingConvention = true;

    private boolean applySecuredProperties = true;

    private String strictSourceSuffix = "Form";

    private String strictTargetSuffix = "Result";

    private DiagnosticsDetailLevel diagnosticsLevel = DiagnosticsDetailLevel.DISABLED;

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

    public boolean getApplyStrictMappingConvention() {
        return applyStrictMappingConvention;
    }

    public void setApplyStrictMappingConvention(boolean applyStrictMappingConvention) {
        this.applyStrictMappingConvention = applyStrictMappingConvention;
    }

    public boolean getApplySecuredProperties() {
        return applySecuredProperties;
    }

    public void setApplySecuredProperties(boolean applySecuredProperties) {
        this.applySecuredProperties = applySecuredProperties;
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

    public DiagnosticsDetailLevel getDiagnosticsDetailLevel() {
        return diagnosticsLevel;
    }

    public void setDiagnosticsDetailLevel(DiagnosticsDetailLevel diagnosticsLevel) {
        this.diagnosticsLevel = diagnosticsLevel;
    }
}
