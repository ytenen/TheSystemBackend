package org.smorodin.core.model;

import lombok.Getter;

import java.util.Objects;

@Getter
public class User {
    private final String userName;
    private final String passwordHash;
    private final Long playerId;
    private final Long id;

    private User(Builder builder) {
        this.userName = builder.userName;
        this.passwordHash = builder.passwordHash;
        this.playerId = builder.playerId;
        this.id = builder.id;
        validate();
    }

    private void validate() {
        if (userName == null || userName.length() < 3) {  // ← изменил на 3
            throw new IllegalArgumentException("Username must be at least 3 characters");
        }
        if (passwordHash == null || passwordHash.isEmpty()) {
            throw new IllegalArgumentException("Password hash cannot be empty");
        }
        if (playerId == null || playerId < 1) {
            throw new IllegalArgumentException("Player id cannot be null or less than 1");
        }
        if (id == null || id < 1) {
            throw new IllegalArgumentException("Id cannot be null or less than 1");
        }
    }


    // Билдер
    public static class Builder {
        private String userName;
        private String passwordHash;
        private Long playerId;
        private Long id;

        public Builder userName(String userName) {
            this.userName = userName;
            return this;
        }

        public Builder passwordHash(String passwordHash) {
            this.passwordHash = passwordHash;
            return this;
        }

        public Builder playerId(Long playerId) {
            this.playerId = playerId;
            return this;
        }

        public Builder id(Long id) {
            this.id = id;
            return this;
        }

        public User build() {
            return new User(this);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        User user = (User) o;
        return Objects.equals(userName, user.userName) &&
                Objects.equals(id, user.id) &&
                Objects.equals(playerId, user.playerId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userName, id, playerId);
    }

    @Override
    public String toString() {
        return "User{" +
                "userName='" + userName + '\'' +
                ", playerId=" + playerId +
                '}';
    }
}
