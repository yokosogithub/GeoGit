package org.geogit.storage.text;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.geogit.storage.FieldType;

import com.google.common.base.Joiner;
import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.io.ParseException;
import com.vividsolutions.jts.io.WKTReader;

/**
 * A text serializer for attribute values
 * 
 */
public class TextValueSerializer {

    static interface ValueSerializer {

        public Object fromString(String in) throws ParseException;

        public String toString(Object value);

    }

    static abstract class DefaultValueSerializer implements ValueSerializer {
        @Override
        public String toString(Object value) {
            return value.toString();
        }
    }

    static abstract class ArraySerializer implements ValueSerializer {
        @Override
        public String toString(Object value) {
            return "[" + Joiner.on(" ").join((Object[]) value) + "]";
        }
    }

    static Map<FieldType, ValueSerializer> serializers = new HashMap<FieldType, ValueSerializer>();
    static {
        serializers.put(FieldType.NULL, new DefaultValueSerializer() {
            @Override
            public Object fromString(String in) {
                return null;
            }

            @Override
            public String toString(Object value) {
                return "";
            }
        });
        serializers.put(FieldType.BOOLEAN, new DefaultValueSerializer() {
            @Override
            public Object fromString(String in) {
                return new Boolean(in);
            }

        });
        serializers.put(FieldType.BYTE, new DefaultValueSerializer() {
            @Override
            public Object fromString(String in) {
                return new Byte(in);
            }
        });
        serializers.put(FieldType.SHORT, new DefaultValueSerializer() {
            @Override
            public Object fromString(String in) {
                return new Short(in);
            }
        });
        serializers.put(FieldType.INTEGER, new DefaultValueSerializer() {
            @Override
            public Object fromString(String in) {
                return new Integer(in);
            }
        });
        serializers.put(FieldType.LONG, new DefaultValueSerializer() {
            @Override
            public Object fromString(String in) {
                return new Long(in);
            }
        });
        serializers.put(FieldType.FLOAT, new DefaultValueSerializer() {
            @Override
            public Object fromString(String in) {
                return new Float(in);
            }
        });
        serializers.put(FieldType.DOUBLE, new DefaultValueSerializer() {
            @Override
            public Object fromString(String in) {
                return new Double(in);
            }
        });
        serializers.put(FieldType.STRING, new DefaultValueSerializer() {
            @Override
            public Object fromString(String in) {
                return in;
            }
        });

        serializers.put(FieldType.BOOLEAN_ARRAY, new ArraySerializer() {
            @Override
            public Object fromString(String in) {
                String[] s = in.split(" ");
                List<Boolean> list = Lists.newArrayList();
                for (String token : s) {
                    list.add(new Boolean(token));
                }
                return list.toArray(new Boolean[0]);
            }
        });
        serializers.put(FieldType.BYTE_ARRAY, new ArraySerializer() {
            @Override
            public Object fromString(String in) {
                String[] s = in.split(" ");
                List<Byte> list = Lists.newArrayList();
                for (String token : s) {
                    list.add(new Byte(token));
                }
                return list.toArray(new Byte[0]);
            }
        });
        serializers.put(FieldType.SHORT_ARRAY, new ArraySerializer() {
            @Override
            public Object fromString(String in) {
                String[] s = in.replace("[", "").replace("]", "").split(" ");
                List<Short> list = Lists.newArrayList();
                for (String token : s) {
                    list.add(new Short(token));
                }
                return list.toArray(new Short[0]);
            }
        });
        serializers.put(FieldType.INTEGER_ARRAY, new ArraySerializer() {
            @Override
            public Object fromString(String in) {
                String[] s = in.split(" ");
                List<Integer> list = Lists.newArrayList();
                for (String token : s) {
                    list.add(new Integer(token));
                }
                return list.toArray(new Integer[0]);
            }
        });
        serializers.put(FieldType.LONG_ARRAY, new ArraySerializer() {
            @Override
            public Object fromString(String in) {
                String[] s = in.split(" ");
                List<Long> list = Lists.newArrayList();
                for (String token : s) {
                    list.add(new Long(token));
                }
                return list.toArray(new Long[0]);
            }
        });
        serializers.put(FieldType.FLOAT_ARRAY, new ArraySerializer() {
            @Override
            public Object fromString(String in) {
                String[] s = in.split(" ");
                List<Float> list = Lists.newArrayList();
                for (String token : s) {
                    list.add(new Float(token));
                }
                return list.toArray(new Float[0]);
            }
        });
        serializers.put(FieldType.DOUBLE_ARRAY, new ArraySerializer() {
            @Override
            public Object fromString(String in) {
                String[] s = in.split(" ");
                List<Double> list = Lists.newArrayList();
                for (String token : s) {
                    list.add(new Double(token));
                }
                return list.toArray(new Byte[0]);
            }
        });
        ValueSerializer geometry = new ValueSerializer() {

            @Override
            public Object fromString(String in) throws ParseException {
                return new WKTReader().read(in);
            }

            @Override
            public String toString(Object value) {
                return ((Geometry) value).toText();
            }

        };
        serializers.put(FieldType.GEOMETRY, geometry);
        serializers.put(FieldType.POINT, geometry);
        serializers.put(FieldType.LINESTRING, geometry);
        serializers.put(FieldType.POLYGON, geometry);
        serializers.put(FieldType.MULTIPOINT, geometry);
        serializers.put(FieldType.MULTILINESTRING, geometry);
        serializers.put(FieldType.MULTIPOLYGON, geometry);
        serializers.put(FieldType.GEOMETRYCOLLECTION, geometry);
        serializers.put(FieldType.UUID, new DefaultValueSerializer() {
            @Override
            public Object fromString(String in) {
                return UUID.fromString(in);
            }

        });
        serializers.put(FieldType.BIG_INTEGER, new DefaultValueSerializer() {
            @Override
            public Object fromString(String in) {
                return new BigInteger(in);
            }

        });
        serializers.put(FieldType.BIG_DECIMAL, new DefaultValueSerializer() {
            @Override
            public Object fromString(String in) {
                return new BigDecimal(in);
            }
        });

    }

    /**
     * Returns a string representation of the passed field value
     * 
     * @param opt
     */
    public static String asString(Optional<Object> opt) {
        FieldType type = FieldType.forValue(opt);
        if (serializers.containsKey(type)) {
            return serializers.get(type).toString(opt.get());
        } else {
            throw new IllegalArgumentException("The specified type is not supported");
        }
    }

    /**
     * Creates a value object from its string representation
     * 
     * @param type
     * @param in
     * @return
     */
    public static Object fromString(FieldType type, String in) {
        if (serializers.containsKey(type)) {
            try {
                return serializers.get(type).fromString(in);
            } catch (ParseException e) {
                throw new IllegalArgumentException("Unable to parse wrong value: " + in);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Unable to parse wrong value: " + in);
            }
        } else {
            throw new IllegalArgumentException("The specified type is not supported");
        }
    }
}
