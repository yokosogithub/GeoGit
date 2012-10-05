package org.geogit.api;

import static com.google.common.base.Objects.equal;

public class RevPerson {

    private String name;

    private String email;

    public RevPerson(String name, String email) {
        this.name = name;
        this.email = email;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof RevPerson)) {
            return false;
        }
        RevPerson person = (RevPerson) o;
        return equal(getName(), person.getName()) && equal(getEmail(), person.getEmail());
    }
}
