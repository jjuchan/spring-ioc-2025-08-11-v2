package com.ll.framework.ioc;

import com.ll.framework.ioc.annotations.Component;
import com.ll.standard.util.Ut;
import org.reflections.Reflections;

import java.lang.reflect.Constructor;
import java.util.*;

public class ApplicationContext {
    private final Reflections reflections;
    private final Map<String, Object> beans = new HashMap<>();

    public ApplicationContext(String basePackage) {
        reflections = new Reflections(basePackage);
    }

    public void init() {
        Set<Class<?>> classes = reflections.getTypesAnnotatedWith(Component.class);

        for (Class<?> clazz : classes) {
            if (clazz.isAnnotation()) continue;

            try {
                Object instance = createBean(clazz);
                String beanName = Ut.str.lcfirst(clazz.getSimpleName());
                beans.put(beanName, instance);

            } catch (Exception e) {
                throw new RuntimeException("빈 생성 실패: " + clazz, e);
            }
        }
    }

    private Object createBean(Class<?> clazz) throws Exception {
        Constructor<?>[] constructors = clazz.getConstructors();

        // 파라미터 없는 생성자가 있으면 사용, 없으면 파라미터가 가장 적은 생성자 사용
        Constructor<?> constructor = Arrays.stream(constructors)
                .min(Comparator.comparingInt(Constructor::getParameterCount))
                .orElseThrow();

        Class<?>[] paramTypes = constructor.getParameterTypes();
        Object[] params = new Object[paramTypes.length];

        for (int i = 0; i < paramTypes.length; i++) {
            String depBeanName = Ut.str.lcfirst(paramTypes[i].getSimpleName());
            params[i] = beans.get(depBeanName);
        }

        return constructor.newInstance(params);
    }

    public <T> T genBean(String beanName) {
        return (T) beans.get(beanName);
    }
}
