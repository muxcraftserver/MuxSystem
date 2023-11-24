package me.muxteam.basic;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

public class NMSReflection {
	public static void setObject(final Class<?> target, final String variable, final Object ob, final Object toset) {
		try {
			final Field f = target.getDeclaredField(variable);
			f.setAccessible(true);
			f.set(ob, toset);
		} catch (final Exception e) {
			e.printStackTrace();
		}
	}

	public static Object getObject(final Class<?> target, final String variable, final Object cl) {
		try {
			final Field f = target.getDeclaredField(variable);
			f.setAccessible(true);
			return f.get(cl);
		} catch (final Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	public static Field getField(final Class<?> target, final String variable) {
		try {
			final Field f = target.getDeclaredField(variable);
			f.setAccessible(true);
			return f;
		} catch (final Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	public static Method getMethod(final Class<?> target, final String method) {
		try {
			for (final Method methods : target.getMethods()) {
				if (methods.getName().equals(method)) return methods;
			}
		} catch (final Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	public static Object invoke(final Class<?> target, final Method method, final Object obj, final Object... args) {
		return invoke(target, method.getName(), method.getParameterTypes(), method.getReturnType(), obj, args);
	}

	public static <T> T invoke(final Class<?> target, final String method, final Class<?>[] arguments, final Class<T> returntype, final Object cl, final Object... args) {
		try {
			final Method m = target.getMethod(method, arguments);
			m.setAccessible(true);
			return returntype.cast(m.invoke(cl, args));
		} catch (final Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	public static void removeFinal(final Field field) {
		try {
			final Field modifiersField = Field.class.getDeclaredField("modifiers");
			modifiersField.setAccessible(true);
			modifiersField.setInt(field, field.getModifiers() & ~Modifier.FINAL);
		} catch (final Exception e) {
			e.printStackTrace();
		}
	}
}