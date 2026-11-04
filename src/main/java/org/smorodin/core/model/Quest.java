
package org.smorodin.core.model;

import lombok.Getter;
import org.smorodin.core.model.enums.QuestType;
import org.smorodin.core.model.enums.ResetInterval;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@Getter
public class Quest {

    private final Long id;
    private final Long ownerId;
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
        this.ownerId = builder.ownerId;
        this.type = builder.type;
        this.title = builder.title;
        this.description = builder.description;
        this.targets = builder.targets == null
                ? null
                : Map.copyOf(builder.targets);
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
        if (type == null) {
            throw new IllegalArgumentException("Quest type is required");
        }

        if (title == null || title.isBlank() || title.length() > 200) {
            throw new IllegalArgumentException("Invalid quest title");
        }

        if (targets == null || targets.isEmpty()) {
            throw new IllegalArgumentException("Targets cannot be empty");
        }

        for (Map.Entry<String, Integer> entry : targets.entrySet()) {
            if (entry.getKey() == null || entry.getKey().isBlank()
                    || entry.getValue() == null || entry.getValue() <= 0) {
                throw new IllegalArgumentException("Invalid quest target");
            }
        }

        if (expReward < 0 || statRewardAmount < 0) {
            throw new IllegalArgumentException("Rewards cannot be negative");
        }

        if (resetInterval == null) {
            throw new IllegalArgumentException("Reset interval is required");
        }

        if (type == QuestType.PERSONAL) {
            if (ownerId == null || ownerId <= 0) {
                throw new IllegalArgumentException(
                        "Personal quest must have an owner"
                );
            }

            if (resetInterval != ResetInterval.NONE) {
                throw new IllegalArgumentException(
                        "Personal quest cannot reset automatically"
                );
            }
        } else if (ownerId != null) {
            throw new IllegalArgumentException(
                    "System quest cannot have an owner"
            );
        }

        if (startDate != null && endDate != null
                && endDate.isBefore(startDate)) {
            throw new IllegalArgumentException(
                    "End date cannot be before start date"
            );
        }
    }

    public static class Builder {
        private Long id;
        private Long ownerId;
        private QuestType type = QuestType.DAILY;
        private String title;
        private String description;
        private Map<String, Integer> targets;
        private int expReward;
        private String statReward;
        private int statRewardAmount;
        private ResetInterval resetInterval = ResetInterval.DAILY;
        private LocalDate startDate;
        private LocalDate endDate;
        private String aiContext;
        private String fileReference;

        public Builder id(Long value) {
            id = value;
            return this;
        }

        public Builder ownerId(Long value) {
            ownerId = value;
            return this;
        }

        public Builder type(QuestType value) {
            type = value;
            return this;
        }

        public Builder title(String value) {
            title = value;
            return this;
        }

        public Builder description(String value) {
            description = value;
            return this;
        }

        public Builder targets(Map<String, Integer> value) {
            targets = value == null ? null : new HashMap<>(value);
            return this;
        }

        public Builder expReward(int value) {
            expReward = value;
            return this;
        }

        public Builder statReward(String value) {
            statReward = value;
            return this;
        }

        public Builder statRewardAmount(int value) {
            statRewardAmount = value;
            return this;
        }

        public Builder resetInterval(ResetInterval value) {
            resetInterval = value;
            return this;
        }

        public Builder startDate(LocalDate value) {
            startDate = value;
            return this;
        }

        public Builder endDate(LocalDate value) {
            endDate = value;
            return this;
        }

        public Builder aiContext(String value) {
            aiContext = value;
            return this;
        }

        public Builder fileReference(String value) {
            fileReference = value;
            return this;
        }

        public Quest build() {
            return new Quest(this);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Quest quest)) return false;

        return Objects.equals(id, quest.id)
                && Objects.equals(ownerId, quest.ownerId)
                && type == quest.type
                && Objects.equals(title, quest.title)
                && Objects.equals(targets, quest.targets);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, ownerId, type, title, targets);
    }

    @Override
    public String toString() {
        return "Quest{" +
                "id=" + id +
                ", ownerId=" + ownerId +
                ", type=" + type +
                ", title='" + title + '\'' +
                '}';
    }
}
