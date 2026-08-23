package org.smorodin.infrastructure.web.dto;

import lombok.Data;

@Data
public class IncreaseStatRequestDto {
    private String statName;
    private int amount;
}
