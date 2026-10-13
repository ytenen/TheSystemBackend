package org.smorodin.core.model;

import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@Getter
public class PlayerQuest {
    private final Long id;
    private final Long playerId;
    private final Long questId;
    private final Map<String, Integer> progress;
    private final boolean completed;
    private final LocalDateTime completedAt;
    private final LocalDate lastResetDate;

    private PlayerQuest(Builder builder) {
        this.id = builder.id;
        this.playerId = builder.playerId;
        this.questId = builder.questId;
        this.progress = builder.progress;
        this.completed = builder.completed;
        this.completedAt = builder.completedAt;
        this.lastResetDate = builder.lastResetDate;
        validate();
    }

    private void validate() {
        if (playerId == null || playerId < 1) {
            throw new IllegalArgumentException("Player id cannot be null or less than 1");
        }
        if (questId == null || questId < 1) {
            throw new IllegalArgumentException("Quest id cannot be null or less than 1");
        }
        if (progress == null) {
            throw new IllegalArgumentException("Progress cannot be null");
        }
    }

    // Увеличить прогресс по конкретному действию
    public PlayerQuest increaseProgress(String action, int amount) {
        Map<String, Integer> newProgress = new HashMap<>(this.progress);
        newProgress.put(action, newProgress.getOrDefault(action, 0) + amount);
        return new Builder()
                .id(this.id)
                .playerId(this.playerId)
                .questId(this.questId)
                .progress(newProgress)
                .completed(this.completed)
                .completedAt(this.completedAt)
                .lastResetDate(this.lastResetDate)
                .build();
    }

    // Проверить, достигнуты ли все цели квеста
    public boolean isAllTargetsMet(Map<String, Integer> targets) {
        for (Map.Entry<String, Integer> entry : targets.entrySet()) {
            int current = progress.getOrDefault(entry.getKey(), 0);
            if (current < entry.getValue()) {
                return false;
            }
        }
        return true;
    }

    // Проверить, достигнута ли двойная цель (для скрытого квеста)
    public boolean isDoubleTargetsMet(Map<String, Integer> targets) {
        for (Map.Entry<String, Integer> entry : targets.entrySet()) {
            int current = progress.getOrDefault(entry.getKey(), 0);
            if (current < entry.getValue() * 2) {
                return false;
            }
        }
        return true;
    }

    // Отметить как выполненный
    public PlayerQuest markCompleted() {
        return new Builder()
                .id(this.id)
                .playerId(this.playerId)
                .questId(this.questId)
                .progress(this.progress)
                .completed(true)
                .completedAt(LocalDateTime.now())
                .lastResetDate(this.lastResetDate)
                .build();
    }

    // Сбросить прогресс (для ежедневных квестов)
    public PlayerQuest reset() {
        return new Builder()
                .id(this.id)
                .playerId(this.playerId)
                .questId(this.questId)
                .progress(new HashMap<>())
                .completed(false)
                .completedAt(null)
                .lastResetDate(LocalDate.now())
                .build();
    }

    public static class Builder {
        private Long id;
        private Long playerId;
        private Long questId;
        private Map<String, Integer> progress = new HashMap<>();
        private boolean completed = false;
        private LocalDateTime completedAt;
        private LocalDate lastResetDate = LocalDate.now();

        public Builder id(Long id) { this.id = id; return this; }
        public Builder playerId(Long playerId) { this.playerId = playerId; return this; }
        public Builder questId(Long questId) { this.questId = questId; return this; }
        public Builder progress(Map<String, Integer> progress) { this.progress = progress; return this; }
        public Builder completed(boolean completed) { this.completed = completed; return this; }
        public Builder completedAt(LocalDateTime completedAt) { this.completedAt = completedAt; return this; }
        public Builder lastResetDate(LocalDate lastResetDate) { this.lastResetDate = lastResetDate; return this; }

        public PlayerQuest build() {
            return new PlayerQuest(this);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PlayerQuest that = (PlayerQuest) o;
        return completed == that.completed &&
                Objects.equals(id, that.id) &&
                Objects.equals(playerId, that.playerId) &&
                Objects.equals(questId, that.questId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, playerId, questId, completed);
    }

    @Override
    public String toString() {
        return "PlayerQuest{" +
                "id=" + id +
                ", playerId=" + playerId +
                ", questId=" + questId +
                ", progress=" + progress +
                ", completed=" + completed +
                ", lastResetDate=" + lastResetDate +
                '}';
    }
}