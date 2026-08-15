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

    public PlayerEntity(String name, int level, long exp, StatsEmbedded stats) {
        this.name = name;
        this.level = level;
        this.exp = exp;
        this.stats = stats;
    }

    public static PlayerEntity of(Player player){
        return new PlayerEntity(player.getName(), player.getLevel(), player.getExp(), StatsEmbedded.of(player.getStats()));
    }

}
