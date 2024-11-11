package com.avolution.actor.util;

import sun.misc.Unsafe;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public final class DeepCopy {

    private static final Unsafe UNSAFE;
    private static final ConcurrentHashMap<Class<?>, Field[]> FIELDS_CACHE = new ConcurrentHashMap<>();
    private static final Set<Class<?>> IMMUTABLE_TYPES = new HashSet<>();

    static {
        try {
            Field f = Unsafe.class.getDeclaredField("theUnsafe");
            f.setAccessible(true);
            UNSAFE = (Unsafe) f.get(null);

            // 初始化不可变类型集合
            IMMUTABLE_TYPES.addAll(Arrays.asList(
                    String.class, Boolean.class, Byte.class, Character.class,
                    Short.class, Integer.class, Long.class, Float.class,
                    Double.class, Void.class
            ));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @SuppressWarnings("unchecked")
    public static <T> T copy(T obj) {
        if (obj == null) return null;

        // 处理不可变对象
        if (IMMUTABLE_TYPES.contains(obj.getClass())) {
            return obj;
        }

        return (T) deepCopy(obj, new IdentityHashMap<>());
    }

    private static Object deepCopy(Object obj, Map<Object, Object> visited) {
        if (obj == null) return null;

        Class<?> clazz = obj.getClass();

        // 检查循环引用
        if (visited.containsKey(obj)) {
            return visited.get(obj);
        }

        // 处理数组
        if (clazz.isArray()) {
            return copyArray(obj, visited);
        }

        // 处理集合类型
        if (obj instanceof Collection) {
            return copyCollection((Collection<?>) obj, visited);
        }

        // 处理Map类型
        if (obj instanceof Map) {
            return copyMap((Map<?, ?>) obj, visited);
        }

        // 创建新实例
        Object copy = createInstance(clazz);
        visited.put(obj, copy);

        // 复制字段
        copyFields(obj, copy, clazz, visited);

        return copy;
    }

    private static Object createInstance(Class<?> clazz) {
        try {
            return UNSAFE.allocateInstance(clazz);
        } catch (InstantiationException e) {
            throw new RuntimeException("Failed to create instance of " + clazz, e);
        }
    }

    private static void copyFields(Object src, Object dest, Class<?> clazz, Map<Object, Object> visited) {
        Field[] fields = FIELDS_CACHE.computeIfAbsent(clazz, cls -> {
            List<Field> fieldList = new ArrayList<>();
            while (cls != Object.class) {
                for (Field field : cls.getDeclaredFields()) {
                    if (!Modifier.isStatic(field.getModifiers())) {
                        field.setAccessible(true);
                        fieldList.add(field);
                    }
                }
                cls = cls.getSuperclass();
            }
            return fieldList.toArray(new Field[0]);
        });

        for (Field field : fields) {
            try {
                Object value = field.get(src);
                field.set(dest, deepCopy(value, visited));
            } catch (IllegalAccessException e) {
                throw new RuntimeException("Failed to copy field: " + field, e);
            }
        }
    }

    private static Object copyArray(Object array, Map<Object, Object> visited) {
        int length = Array.getLength(array);
        Object copy = Array.newInstance(array.getClass().getComponentType(), length);
        visited.put(array, copy);

        for (int i = 0; i < length; i++) {
            Array.set(copy, i, deepCopy(Array.get(array, i), visited));
        }
        return copy;
    }

    private static Collection<?> copyCollection(Collection<?> col, Map<Object, Object> visited) {
        Collection<Object> copy;
        try {
            copy = (Collection<Object>) col.getClass().getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            copy = new ArrayList<>();
        }
        visited.put(col, copy);

        for (Object item : col) {
            copy.add(deepCopy(item, visited));
        }
        return copy;
    }

    private static Map<?, ?> copyMap(Map<?, ?> map, Map<Object, Object> visited) {
        Map<Object, Object> copy;
        try {
            copy = (Map<Object, Object>) map.getClass().getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            copy = new HashMap<>();
        }
        visited.put(map, copy);

        for (Map.Entry<?, ?> entry : map.entrySet()) {
            copy.put(
                    deepCopy(entry.getKey(), visited),
                    deepCopy(entry.getValue(), visited)
            );
        }
        return copy;
    }
}