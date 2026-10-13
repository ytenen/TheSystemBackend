package org.smorodin.core.model;

import lombok.Getter;
import org.smorodin.core.model.enums.QuestType;
import org.smorodin.core.model.enums.ResetInterval;

import java.time.LocalDate;
import java.util.Map;
import java.util.Objects;

@Getter
public class Quest {
    private final Long id;
    private final QuestType type;
    private final String title;
    private final String description;
    private final Map<String, Integer> targets;
    private final int expReward;
    private final String statReward;
    private final int statRewardAmount;
    private final ResetInterval resetInterval;
    private final LocalDate startDate;
    private final LocalDate endDate;
    private final String aiContext;
    private final String fileReference;

    private Quest(Builder builder) {
        this.id = builder.id;
        this.type = builder.type;
        this.title = builder.title;
        this.description = builder.description;
        this.targets = builder.targets;
        this.expReward = builder.expReward;
        this.statReward = builder.statReward;
        this.statRewardAmount = builder.statRewardAmount;
        this.resetInterval = builder.resetInterval;
        this.startDate = builder.startDate;
        this.endDate = builder.endDate;
        this.aiContext = builder.aiContext;
        this.fileReference = builder.fileReference;
        validate();
    }

    private void validate() {
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("Title cannot be empty");
        }
        if (targets == null || targets.isEmpty()) {
            throw new IllegalArgumentException("Targets cannot be empty");
        }
        for (Map.Entry<String, Integer> entry : targets.entrySet()) {
            if (entry.getKey() == null || entry.getKey().isBlank()) {
                throw new IllegalArgumentException("Target action name cannot be empty");
            }
            if (entry.getValue() == null || entry.getValue() <= 0) {
                throw new IllegalArgumentException("Target value must be positive");
            }
        }
        if (expReward < 0) {
            throw new IllegalArgumentException("Exp reward cannot be negative");
        }
        if (type == null) {
            throw new IllegalArgumentException("Quest type cannot be null");
        }
    }

    public static class Builder {
        private Long id;
        private QuestType type = QuestType.DAILY;
        private String title;
        private String description;
        private Map<String, Integer> targets;
        private int expReward = 0;
        private String statReward;
        private int statRewardAmount = 0;
        private ResetInterval resetInterval = ResetInterval.DAILY;
        private LocalDate startDate;
        private LocalDate endDate;
        private String aiContext;
        private String fileReference;

        public Builder id(Long id) { this.id = id; return this; }
        public Builder type(QuestType type) { this.type = type; return this; }
        public Builder title(String title) { this.title = title; return this; }
        public Builder description(String description) { this.description = description; return this; }
        public Builder targets(Map<String, Integer> targets) { this.targets = targets; return this; }
        public Builder expReward(int expReward) { this.expReward = expReward; return this; }
        public Builder statReward(String statReward) { this.statReward = statReward; return this; }
        public Builder statRewardAmount(int statRewardAmount) { this.statRewardAmount = statRewardAmount; return this; }
        public Builder resetInterval(ResetInterval resetInterval) { this.resetInterval = resetInterval; return this; }
        public Builder startDate(LocalDate startDate) { this.startDate = startDate; return this; }
        public Builder endDate(LocalDate endDate) { this.endDate = endDate; return this; }
        public Builder aiContext(String aiContext) { this.aiContext = aiContext; return this; }
        public Builder fileReference(String fileReference) { this.fileReference = fileReference; return this; }

        public Quest build() {
            return new Quest(this);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Quest quest = (Quest) o;
        return expReward == quest.expReward &&
                statRewardAmount == quest.statRewardAmount &&
                Objects.equals(id, quest.id) &&
                type == quest.type &&
                Objects.equals(title, quest.title) &&
                Objects.equals(targets, quest.targets) &&
                resetInterval == quest.resetInterval;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, type, title, targets, expReward, statRewardAmount, resetInterval);
    }

    @Override
    public String toString() {
        return "Quest{" +
                "id=" + id +
                ", type=" + type +
                ", title='" + title + '\'' +
                ", targets=" + targets +
                ", expReward=" + expReward +
                '}';
    }
}