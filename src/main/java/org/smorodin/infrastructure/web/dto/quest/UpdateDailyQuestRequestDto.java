package org.smorodin.infrastructure.web.dto.quest;

import lombok.Data;

import java.util.Map;

@Data
public class UpdateDailyQuestRequestDto {

    private Map<String, Integer> targets;

    private UpdateMode mode;

    public enum UpdateMode {
        NOW,
        TOMORROW
    }
}
