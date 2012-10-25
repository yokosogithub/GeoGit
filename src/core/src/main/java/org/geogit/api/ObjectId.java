/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the LGPL 2.1 license, available at the root
 * application directory.
 */
package org.geogit.api;

import java.util.Arrays;

import com.google.common.base.Preconditions;
import com.google.common.hash.HashCode;
import com.google.common.hash.HashCodes;
import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;

/**
 * A semi-mutable SHA-1 abstraction.
 * <p>
 * An ObjectId is mutable as long as it has not been assigned a raw value already
 * </p>
 */
public class ObjectId implements Comparable<ObjectId> {

    public static final ObjectId NULL;

    public static final HashFunction HASH_FUNCTION;

    private static final int NUM_BYTES;

    private static int NUM_CHARS;
    static {
        HASH_FUNCTION = Hashing.sha1();

        NUM_BYTES = HASH_FUNCTION.bits() / 8;

        NUM_CHARS = 2 * NUM_BYTES;

        NULL = new ObjectId(new byte[20]);
    }

    private final byte[] hashCode;

    public ObjectId() {
        this.hashCode = NULL.hashCode;
    }

    public ObjectId(byte[] raw) {
        Preconditions.checkNotNull(raw);
        Preconditions.checkArgument(raw.length == NUM_BYTES);
        this.hashCode = raw.clone();
    }

    public boolean isNull() {
        return NULL.equals(this);
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof ObjectId)) {
            return false;
        }
        return Arrays.equals(hashCode, ((ObjectId) o).hashCode);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(hashCode);
    }

    /**
     * @return a human friendly representation of this SHA1
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return HashCodes.fromBytes(hashCode).toString();
    }

    /**
     * Returns the objectid represented by its string form, this method is the inverse of
     * {@link #toString()}
     * 
     * @return
     */
    public static ObjectId valueOf(final String hash) {
        Preconditions.checkNotNull(hash);
        Preconditions.checkArgument(hash.length() == NUM_CHARS, hash,
                String.format("Invalid hash string %s", hash));

        // this is perhaps the worse way of doing this...

        final byte[] raw = new byte[NUM_BYTES];
        final int radix = 16;
        for (int i = 0; i < NUM_BYTES; i++) {
            raw[i] = (byte) Integer.parseInt(hash.substring(2 * i, 2 * i + 2), radix);
        }
        return new ObjectId(raw);
    }

    public static byte[] toRaw(final String hash) {
        Preconditions.checkNotNull(hash);
        for (int i = 0; i < hash.length(); i++) {
            char c = hash.charAt(i);
            if (-1 == Character.digit(c, 16)) {
                throw new IllegalArgumentException("At index " + i
                        + ": partialId is not a valid hash subsequence '" + hash + "'");
            }
        }

        final byte[] raw = new byte[hash.length() / 2];
        final int radix = 16;
        for (int i = 0; i < raw.length; i++) {
            raw[i] = (byte) Integer.parseInt(hash.substring(2 * i, 2 * i + 2), radix);
        }
        return raw;
    }

    /**
     * @see java.lang.Comparable#compareTo(java.lang.Object)
     */
    public int compareTo(final ObjectId o) {
        byte[] left = this.hashCode;
        byte[] right = o.hashCode;
        return compare(left, right);
    }

    private static int compare(byte[] left, byte[] right) {
        int c;
        for (int i = 0; i < left.length; i++) {
            c = left[i] - right[i];
            if (c != 0) {
                return c;
            }
        }
        return 0;
    }

    public byte[] getRawValue() {
        return hashCode.clone();
    }

    /**
     * Creates a new SHA-1 ObjectId for the byte[] contents of the given string.
     * <p>
     * Note this method is to hash a string, not to convert the string representation of an ObjectId
     * </p>
     * 
     * @param strToHash
     * @return
     */
    public static ObjectId forString(final String strToHash) {
        Preconditions.checkNotNull(strToHash);
        HashCode hashCode = HASH_FUNCTION.hashString(strToHash);
        return new ObjectId(hashCode.asBytes());
    }

    /**
     * Prints the object ID just like the git command "0000000..0000000"
     * 
     * @return
     */
    public String printSmallId() {
        String out = toString();
        return out.substring(0, 7) + ".." + out.substring(out.length() - 7, out.length());
    }

    /**
     * @param index
     * @return
     */
    public int byteN(int index) {
        return this.hashCode[index];
    }
}
