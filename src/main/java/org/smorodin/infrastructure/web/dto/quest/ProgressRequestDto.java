package org.smorodin.infrastructure.web.dto.quest;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ProgressRequestDto {

    private String action;
    private int amount;
}