package org.smorodin.infrastructure.web.dto.quest;

import lombok.Builder;
import lombok.Getter;

import java.util.Map;

@Getter
@Builder
public class QuestResponseDto {

    private Long id;
    private String title;
    private String description;

    private Map<String, Integer> targets;
    private Map<String, Integer> progress;

    private int expReward;
    private Map<String, Integer> statXpRewards;

    private boolean completed;
}