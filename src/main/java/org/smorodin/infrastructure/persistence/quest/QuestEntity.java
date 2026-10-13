package org.smorodin.infrastructure.persistence.quest;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.smorodin.core.model.Quest;
import org.smorodin.core.model.enums.QuestType;
import org.smorodin.core.model.enums.ResetInterval;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

@Entity
@Table(name = "quests")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class QuestEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private QuestType type;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(length = 1000)
    private String description;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "json")
    private Map<String, Integer> targets = new HashMap<>();

    @Column(nullable = false)
    private int expReward;

    @Column(name = "stat_reward")
    private String statReward;

    @Column(name = "stat_reward_amount")
    private int statRewardAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "reset_interval", nullable = false)
    private ResetInterval resetInterval;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "ai_context", length = 5000)
    private String aiContext;

    @Column(name = "file_reference", length = 500)
    private String fileReference;

    // === Конвертация Domain → Entity ===
    public static QuestEntity fromDomain(Quest quest) {
        QuestEntity entity = new QuestEntity();
        entity.setId(quest.getId());
        entity.setType(quest.getType());
        entity.setTitle(quest.getTitle());
        entity.setDescription(quest.getDescription());
        entity.setTargets(new HashMap<>(quest.getTargets()));
        entity.setExpReward(quest.getExpReward());
        entity.setStatReward(quest.getStatReward());
        entity.setStatRewardAmount(quest.getStatRewardAmount());
        entity.setResetInterval(quest.getResetInterval());
        entity.setStartDate(quest.getStartDate());
        entity.setEndDate(quest.getEndDate());
        entity.setAiContext(quest.getAiContext());
        entity.setFileReference(quest.getFileReference());
        return entity;
    }

    // === Конвертация Entity → Domain ===
    public Quest toDomain() {
        return new Quest.Builder()
                .id(this.id)
                .type(this.type)
                .title(this.title)
                .description(this.description)
                .targets(new HashMap<>(this.targets))
                .expReward(this.expReward)
                .statReward(this.statReward)
                .statRewardAmount(this.statRewardAmount)
                .resetInterval(this.resetInterval)
                .startDate(this.startDate)
                .endDate(this.endDate)
                .aiContext(this.aiContext)
                .fileReference(this.fileReference)
                .build();
    }
}