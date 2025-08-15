package com.ll.framework.ioc;

import com.ll.framework.ioc.annotations.Component;
import com.ll.standard.util.Ut;
import org.reflections.Reflections;

import java.lang.reflect.Constructor;
import java.util.*;

public class ApplicationContext {
    private final Reflections reflections;
    private final Map<String, Object> singletons = new HashMap<>();
    private final Map<String, Class<?>> beans = new HashMap<>();

    public ApplicationContext(String basePackage) {
        reflections = new Reflections(basePackage);
    }

    public void init() {
        Set<Class<?>> classes = reflections.getTypesAnnotatedWith(Component.class);

        for (Class<?> clazz : classes) {
            try {
                String beanName = Ut.str.lcfirst(clazz.getSimpleName());
                beans.put(beanName, clazz);

            } catch (Exception e) {
                throw new RuntimeException("빈 생성 실패: " + clazz, e);
            }
        }
    }

    private Object createBean(Class<?> clazz) throws Exception {
        Constructor<?>[] constructors = clazz.getDeclaredConstructors();

        // 파라미터 없는 생성자가 있으면 사용, 없으면 파라미터가 가장 적은 생성자 사용
        Constructor<?> constructor = Arrays.stream(constructors).min(Comparator.comparingInt(Constructor::getParameterCount)).orElseThrow();

        constructor.setAccessible(true); //reflection 에서 private 생성자에도 접근 할 수 있게 해줌

        Class<?>[] paramTypes = constructor.getParameterTypes();
        Object[] params = new Object[paramTypes.length];

        for (int i = 0; i < paramTypes.length; i++) {
            String depBeanName = Ut.str.lcfirst(paramTypes[i].getSimpleName());
            params[i] = genBean(depBeanName);
        }

        return constructor.newInstance(params);
    }

    public <T> T genBean(String beanName) {
        Object bean = singletons.get(beanName);
        if (bean != null) return (T) bean;

        Class<?> clazz = beans.get(beanName);
        if (clazz == null) return null;

        Object createdBean;
        try {
            createdBean = createBean(clazz);
        } catch (Exception e) {
            throw new RuntimeException("빈 생성 실패: " + clazz, e);
        }

        singletons.put(beanName, createdBean);
        return (T) createdBean;
    }
}
