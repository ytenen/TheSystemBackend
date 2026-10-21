package org.smorodin.infrastructure.web.dto.quest;

import lombok.Builder;
import lombok.Getter;

import java.util.Map;

@Getter
@Builder
public class ProgressResponseDto {

    private Map<String, Integer> progress;

    private boolean completed;
    private boolean hiddenCompleted;

    private String message;
}