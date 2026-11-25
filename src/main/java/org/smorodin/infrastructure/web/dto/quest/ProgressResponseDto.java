package org.smorodin.infrastructure.web.dto.quest;

import lombok.Builder;
import lombok.Getter;

import java.util.Map;
import java.util.List;

@Getter
@Builder
public class ProgressResponseDto {

    private Map<String, Integer> progress;

    private boolean completed;
    private boolean hiddenCompleted;

    private String message;
    private List<GrantedRewardDto> rewardsGranted;
}