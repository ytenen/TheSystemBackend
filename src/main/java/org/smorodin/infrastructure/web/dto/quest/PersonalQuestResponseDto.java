package org.smorodin.infrastructure.web.dto.quest;

import lombok.Builder;
import lombok.Getter;
import org.smorodin.core.model.enums.QuestStatus;

import java.time.LocalDate;
import java.util.Map;

@Getter
@Builder
public class PersonalQuestResponseDto {

    private Long id;
    private String title;
    private String description;
    private Map<String, Integer> targets;
    private Map<String, Integer> progress;
    private QuestStatus status;
    private int expReward;
    private LocalDate endDate;
}
