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

    /**
     * Constructs a new {@code RevPerson} from a name and email address.
     * 
     * @param name
     * @param email
     */
    public RevPerson(@Nullable String name, @Nullable String email) {
        this.name = name;
        this.email = email;
    }

    /**
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * @return the email
     */
    public String getEmail() {
        return email;
    }

    /**
     * Equality based on name and email.
     * 
     * @see Object#equals(Object)
     */
    @Override
    public boolean equals(Object o) {
        if (!(o instanceof RevPerson)) {
            return false;
        }
        RevPerson person = (RevPerson) o;
        return equal(getName(), person.getName()) && equal(getEmail(), person.getEmail());
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(getName(), getEmail());
    }

    @Override
    public String toString() {
        return Optional.fromNullable(name).or("<>") + " <" + Optional.fromNullable(email).or("")
                + ">";
    }
}
