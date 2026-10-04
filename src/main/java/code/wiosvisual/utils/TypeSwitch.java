package code.wiosvisual.utils;

public final class TypeSwitch {
    private TypeSwitch() {
    }

    public static int typeSwitch(Object[] types, Object value, int startIndex) {
        if (value == null) {
            return -1;
        }
        for (int i = Math.max(0, startIndex); i < types.length; ++i) {
            Object type = types[i];
            if (type instanceof Class<?> clazz && clazz.isInstance(value)) {
                return i;
            }
        }
        return -1;
    }
}
