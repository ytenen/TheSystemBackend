package org.smorodin.infrastructure.persistence;

import jakarta.persistence.*;
import lombok.*;
import org.smorodin.core.model.User;

@Entity
@Table(name = "users")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class UserEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, length = 100)
    private String userName;

    @Column(nullable = false)
    private String passwordHash;

    @Column(name = "player_id", nullable = false, unique = true)
    private Long playerId;

    public static UserEntity of(User user){
        return new UserEntity(user.getId(), user.getUserName(), user.getPasswordHash(), user.getPlayerId());
    }

    public User toDomain(){
        return new User.Builder().userName(this.getUserName())
                .passwordHash(this.getPasswordHash())
                .id(this.getId())
                .playerId(this.getPlayerId()).build();
    }

    public static UserEntity createNewUser(String userName, String passwordHash){
        UserEntity user = new UserEntity();
        user.setUserName(userName);
        user.setPasswordHash(passwordHash);
        user.setPlayerId(user.getId());
        return user;
    }

}
