package org.smorodin.infrastructure.web.dto.quest;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.Map;

@Getter
@Setter
public class CreatePersonalQuestRequestDto {

    private String title;
    private String description;
    private Map<String, Integer> targets;
    private LocalDate endDate;
}
