package org.smorodin.infrastructure.web.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.smorodin.core.model.Player;
import org.smorodin.core.model.Stats;
import org.smorodin.core.model.StatExperience;
import org.smorodin.core.model.enums.StatType;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

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
    private Map<String, StatBarDto> statProgress;

    public record StatBarDto(int currentXp, int requiredXp) {}

    public static PlayerResponseDto of(
            Player player, Long id, boolean hasAvatar,
            Map<StatType, StatExperience> progress
    ) {
        Map<String, StatBarDto> bars = new LinkedHashMap<>();
        for (StatType type : StatType.values()) {
            StatExperience value = progress.get(type);
            bars.put(type.name().toLowerCase(Locale.ROOT),
                    new StatBarDto(value.currentXp(), value.requiredXp()));
        }
        return new PlayerResponseDto(player.getName(),
                player.getLevel(),
                player.getExp(),
                player.getExpToNextLevel(),
                player.getRank().toString(),
                player.getStats(),
                id,
                hasAvatar ? "/api/player/" + id + "/avatar" : null,
                bars);
    }
}
