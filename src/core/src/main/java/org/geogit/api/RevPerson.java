/* Copyright (c) 2013 OpenPlans. All rights reserved.
 * This code is licensed under the BSD New License, available at the root
 * application directory.
 */
package org.geogit.api;

import static com.google.common.base.Objects.equal;

import javax.annotation.Nullable;

import com.google.common.base.Objects;
import com.google.common.base.Optional;

/**
 * The GeoGit identity of a single individual, composed of a name and email address.
 */
public class RevPerson {

    private String name;

    private String email;

    private long timeStamp;

    private int timeZoneOffset;

    /**
     * Constructs a new {@code RevPerson} from a name, email address, timestamp, and time zone
     * offset.
     * 
     * @param name
     * @param email
     * @param timestamp milliseconds since January 1, 1970, 00:00:00 GMT
     * @param timeZoneOffset milliseconds to add to the GMT timestamp
     */
    public RevPerson(@Nullable String name, @Nullable String email, long timeStamp,
            int timeZoneOffset) {
        this.name = name;
        this.email = email;
        this.timeStamp = timeStamp;
        this.timeZoneOffset = timeZoneOffset;
    }

    /**
     * @return the name
     */
    public Optional<String> getName() {
        return Optional.fromNullable(name);
    }

    /**
     * @return the email
     */
    public Optional<String> getEmail() {
        return Optional.fromNullable(email);
    }

    /**
     * @return this person's timestamp, as milliseconds since January 1, 1970, 00:00:00 GMT
     */
    public long getTimestamp() {
        return timeStamp;
    }

    /**
     * @return the time zone offset from UTC, in milliseconds
     */
    public int getTimeZoneOffset() {
        return timeZoneOffset;
    }

    /**
     * @see Object#equals(Object)
     */
    @Override
    public boolean equals(Object o) {
        if (!(o instanceof RevPerson)) {
            return false;
        }
        RevPerson person = (RevPerson) o;
        return equal(getName(), person.getName()) && equal(getEmail(), person.getEmail())
                && getTimestamp() == person.getTimestamp()
                && getTimeZoneOffset() == person.getTimeZoneOffset();
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(getName(), getEmail(), getTimestamp(), getTimeZoneOffset());
    }

    @Override
    public String toString() {
        return Optional.fromNullable(name).or("<>") + " <" + Optional.fromNullable(email).or("")
                + "> " + timeStamp + "/" + timeZoneOffset;
    }
}
