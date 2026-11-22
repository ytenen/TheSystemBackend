package org.smorodin.infrastructure.web.dto.quest;

import org.smorodin.core.model.enums.QuestType;
import java.util.Map;

public record GrantedRewardDto(
        QuestType questType,
        int expReward,
        Map<String, Integer> statXpRewards
) {}
