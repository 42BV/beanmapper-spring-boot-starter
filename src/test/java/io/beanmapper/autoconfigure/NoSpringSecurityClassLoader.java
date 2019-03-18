package io.beanmapper.autoconfigure;

/**
 * Mocked ClassLoader which does not load the class used to detect if Spring Security is on the classpath.
 * This way, we can test if BeanMapperAutoConfig correctly omits the security-related features in such cases.
 */
public class NoSpringSecurityClassLoader extends ClassLoader {

    @Override
    public Class<?> loadClass(String name) throws ClassNotFoundException {
        if (name != null && !name.equals("org.springframework.security.authentication.AuthenticationManager")) {
            return super.loadClass(name);
        }

        throw new NoClassDefFoundError(name);
    }
}
