package org.smorodin.infrastructure.web.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.smorodin.core.model.Player;
import org.smorodin.core.model.Stats;

@Data
@AllArgsConstructor
public class PlayerResponseDto {
    private String name;
    private int level;
    private long exp;
    private long expToNextLevel;
    private String rank;
    private Stats stats;
    private Long id;
    private String avatarUrl;

    public static PlayerResponseDto of(Player player, Long id, boolean hasAvatar){
        return new PlayerResponseDto(player.getName(),
                player.getLevel(),
                player.getExp(),
                player.getExpToNextLevel(),
                player.getRank().toString(),
                player.getStats(),
                id,
                hasAvatar ? "/api/player/" + id + "/avatar" : null);
    }
}
