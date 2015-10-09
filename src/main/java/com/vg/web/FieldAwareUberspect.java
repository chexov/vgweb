package com.vg.web;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import org.apache.velocity.util.introspection.Info;
import org.apache.velocity.util.introspection.UberspectImpl;
import org.apache.velocity.util.introspection.VelMethod;
import org.apache.velocity.util.introspection.VelPropertyGet;

public class FieldAwareUberspect extends UberspectImpl {
    public VelPropertyGet getPropertyGet(Object obj, String identifier, Info info) throws Exception {
        VelPropertyGet get = null;

        if (get == null) {
            try {
                get = new PublicFieldGet(obj, identifier);
            } catch(Exception e) { }
        }

        if (get == null) {
            try {
                get = new DeclaringMethodGet(obj, identifier);
            } catch(Exception e) { }
        }

        if (get == null) {
            try {
                get = new DeclaringFieldGet(obj, identifier);
            } catch(Exception e) { }
        }


        if (get == null) get = super.getPropertyGet(obj, identifier, info);

        return get;
    }

    @Override
    public VelMethod getMethod(Object obj, String methodName, Object[] args, Info i) throws Exception {
        VelMethod method = null;

        try {
            method = new DeclaringMethod(obj, methodName, args);
        } catch(Exception e) { }

        if (method == null)
            method = super.getMethod(obj, methodName, args, i);

        return method;
    }

    public static class PublicFieldGet implements VelPropertyGet {
        final Field f;

        public PublicFieldGet(Object obj, String identifier) throws Exception {
            f = obj.getClass().getField(identifier);
        }

        public Object invoke(Object o) throws Exception {
            return f.get(o);
        }

        public boolean isCacheable() {
            return false;
        }

        public String getMethodName() {
            return f.getName();
        }
    }

    public static class DeclaringFieldGet implements VelPropertyGet {
        final Field f;

        public DeclaringFieldGet(Object obj, String identifier) throws Exception {
            f = obj.getClass().getDeclaredField(identifier);

            if (!f.isAccessible()) f.setAccessible(true);
        }

        public Object invoke(Object o) throws Exception {
            return f.get(o);
        }

        public boolean isCacheable() {
            return false;
        }
        public String getMethodName() {
            return f.getName();
        }

    }

    public static class DeclaringMethodGet implements VelPropertyGet {
        private final Method m;
        private final Object obj;

        public DeclaringMethodGet(Object obj, String identifier) throws SecurityException, NoSuchMethodException {
            this.obj = obj;
            m = obj.getClass().getDeclaredMethod(getMethodName(identifier), new Class[] { });

            if (!m.isAccessible()) m.setAccessible(true);
        }

        private String getMethodName(String identifier) {
            return "get" + Character.toUpperCase(identifier.charAt(0)) + identifier.substring(1);
        }

        @Override
        public String getMethodName() {
            return m.getName();
        }

        @Override
        public Object invoke(Object arg0) throws Exception {
            return m.invoke(obj);
        }

        @Override
        public boolean isCacheable() {
            return false;
        }

    }

    public static class DeclaringMethod implements VelMethod {
        private final Method m;

        public DeclaringMethod(Object obj, String identifier, Object[] args) throws SecurityException, NoSuchMethodException {
            m = findMethod(obj.getClass(), identifier, args);

            if (!m.isAccessible()) m.setAccessible(true);
        }

        private Method findMethod(Class<?> cls, String name, Object[] args) {
            outer:
            for (Method m : cls.getDeclaredMethods()) {
                if (!m.getName().equals(name)) continue;

                Class<?>[] parameterTypes = m.getParameterTypes();
                if (parameterTypes.length != args.length) continue;

                for (int i = 0; i < args.length; i ++) {
                    if (args[i] == null && parameterTypes[i].isPrimitive()) continue outer;

                    if (args[i] != null)
                        if (!parameterTypes[i].isAssignableFrom(args[i].getClass()))
                            continue outer;
                }

                return m;
            }

            if (cls.getSuperclass() == null) return null;

            return findMethod(cls.getSuperclass(), name, args);
        }

        @Override
        public String getMethodName() {
            return m.getName();
        }

        @Override
        public Object invoke(Object obj, Object[] args) throws Exception {
            return m.invoke(obj, args);
        }

        @Override
        public boolean isCacheable() {
            return false;
        }

        @Override
        public Class<?> getReturnType() {
            return m.getReturnType();
        }

    }
}
