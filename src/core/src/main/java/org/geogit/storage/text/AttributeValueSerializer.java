package org.geogit.storage.text;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;
import java.util.UUID;

import org.geogit.storage.EntityType;

import com.google.common.collect.Lists;
import com.google.common.primitives.Bytes;
import com.google.common.primitives.Doubles;
import com.google.common.primitives.Floats;
import com.google.common.primitives.Ints;
import com.google.common.primitives.Longs;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.io.WKTReader;

public class AttributeValueSerializer {

    public static CharSequence asText(Object value) {
        final EntityType type = EntityType.determineType(value);
        switch (type) {
        case CHAR_ARRAY:
            String chars = new String((char[]) value);
            return chars;
        case DOUBLE_ARRAY:
        case FLOAT_ARRAY:
        case INT_ARRAY:
        case LONG_ARRAY:
        case BYTE_ARRAY:
        case BOOLEAN_ARRAY:
            return arrayAsString((Object[]) value);
        case GEOMETRY:
            Geometry geom = (Geometry) value;
            return geom.toText();
        case NULL:
            return "";
        case STRING:
            return "\"" + escapeNewLines(value.toString()) + "\"";
        case BOOLEAN:
        case BYTE:
        case DOUBLE:
        case FLOAT:
        case INT:
        case LONG:
        case BIGDECIMAL:
        case BIGINT:
        case UNKNOWN_SERIALIZABLE:
        case UNKNOWN:
        case UUID:
        default:
            return value.toString();
        }

    }

    private static CharSequence escapeNewLines(String string) {
        return string.replace("\\n", "\\\\n").replace("\n", "\\n");
    }

    private static CharSequence arrayAsString(Object[] array) {
        StringBuilder sb = new StringBuilder("[");
        for (Object element : array) {
            sb.append(element.toString());
            sb.append(" ");
        }
        sb.deleteCharAt(sb.length() - 1);
        sb.append("]");
        return sb.toString();
    }

    public static Object fromText(String className, String value) {
        try {
            Class<?> clazz = Class.forName(className);
            if (clazz.equals(Double.class)) {
                return Double.parseDouble(value);
            } else if (clazz.equals(Integer.class)) {
                return Integer.parseInt(value);
            } else if (clazz.equals(Float.class)) {
                return Float.parseFloat(value);
            } else if (clazz.equals(Long.class)) {
                return Long.parseLong(value);
            } else if (clazz.equals(Boolean.class)) {
                return Boolean.parseBoolean(value);
            } else if (clazz.equals(Byte.class)) {
                return Byte.parseByte(value);
            } else if (clazz.equals(String.class)) {
                if (value.equals("\"\"")) {
                    return "";
                }
                String s = value.substring(1, value.length() - 1);
                return unescapeNewLines(s);
            } else if (clazz.equals(BigInteger.class)) {
                return new BigInteger(value);
            } else if (clazz.equals(BigDecimal.class)) {
                return new BigDecimal(value);
            } else if (clazz.equals(UUID.class)) {
                return UUID.fromString(value);
            } else if (Geometry.class.isAssignableFrom(clazz)) {
                return new WKTReader().read(value);
            } else if (clazz.equals(float[].class) || clazz.equals(double[].class)
                    || clazz.equals(int[].class) || clazz.equals(byte[].class)
                    || clazz.equals(long[].class)) {
                return stringAsArray(value, clazz);
            } else if (clazz.equals(char[].class)) {
                return value.toCharArray();
            } else {
                // TODO: try to somehow create instance of class from text value?
                throw new IllegalArgumentException("Cannot deserialize attribute. Unknown type: "
                        + className);
            }

        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Wrong attribute value: " + value);
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (ClassNotFoundException e) {
            throw new IllegalArgumentException("Cannot deserialize attribute. Unknown type: "
                    + className);
        } catch (Exception e) { // TODO: maybe add more detailed exception handling here
            throw new IllegalArgumentException("Cannot deserialize attribute: " + className + " "
                    + value);
        }
    }

    private static CharSequence unescapeNewLines(String string) {
        // TODO:
        return string;
    }

    private static Object stringAsArray(String value, Class<?> clazz) {
        String[] s = value.replace("[", "").replace("]", "").split(" ");
        List<Number> list = Lists.newArrayList();

        for (String token : s) {
            try {
                if (clazz.equals(double[].class)) {
                    list.add(Double.parseDouble(token));
                } else if (clazz.equals(float[].class)) {
                    list.add(Float.parseFloat(token));
                } else if (clazz.equals(long[].class)) {
                    list.add(Long.parseLong(token));
                } else if (clazz.equals(int[].class)) {
                    list.add(Integer.parseInt(token));
                } else if (clazz.equals(byte[].class)) {
                    list.add(Byte.parseByte(token));
                }
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Cannot parse number array: " + value);
            }
        }
        if (clazz.equals(double[].class)) {
            return Doubles.toArray(list);
        } else if (clazz.equals(float[].class)) {
            return Floats.toArray(list);
        } else if (clazz.equals(long[].class)) {
            return Longs.toArray(list);
        } else if (clazz.equals(int[].class)) {
            return Ints.toArray(list);
        } else if (clazz.equals(byte[].class)) {
            return Bytes.toArray(list);
        }

        throw new IllegalArgumentException("Wrong class: " + clazz.getName());

    }

}
