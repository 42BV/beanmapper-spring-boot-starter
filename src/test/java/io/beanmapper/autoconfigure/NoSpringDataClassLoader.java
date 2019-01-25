package io.beanmapper.autoconfigure;

/**
 * Mocked classLoader to simulate usage of beanmapper-spring-boot-starter without Spring Data JPA on the classpath.
 */
public class NoSpringDataClassLoader extends ClassLoader {

    @Override
    public Class<?> loadClass(String name) throws ClassNotFoundException {
        if (name != null && !name.equals("javax.persistence.EntityManager") && !name.equals("org.hibernate.proxy.HibernateProxy")) {
            return super.loadClass(name);
        }

        throw new NoClassDefFoundError(name);
    }
}
