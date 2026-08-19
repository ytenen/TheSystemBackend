package org.smorodin.infrastructure.persistence;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.smorodin.core.model.Player;

@Entity
@Data
@Table(name = "players")
@NoArgsConstructor
public class PlayerEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private int level;
    private long exp;

    @Embedded
    private StatsEmbedded stats;

    public PlayerEntity(Long id, String name, int level, long exp, StatsEmbedded stats) {
        this.id = id;
        this.name = name;
        this.level = level;
        this.exp = exp;
        this.stats = stats;
    }

    public static PlayerEntity of(Player player, Long id){
        return new PlayerEntity(id, player.getName(), player.getLevel(), player.getExp(), StatsEmbedded.of(player.getStats()));
    }

    public Player toDomain(){
        return new Player.Builder()
                .name(this.getName())
                .level(this.getLevel())
                .exp(this.getExp())
                .stats(this.getStats().toDomain()).build();
    }

}
