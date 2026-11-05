package org.smorodin.core.model;

import lombok.Getter;
import org.smorodin.core.model.enums.QuestStatus;

import java.time.Clock;
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

    private final QuestStatus status;

    private final LocalDateTime completedAt;
    private final LocalDate lastResetDate;

    private PlayerQuest(Builder builder) {
        this.id = builder.id;
        this.playerId = builder.playerId;
        this.questId = builder.questId;
        this.progress = builder.progress;
        this.status = builder.status;
        this.completedAt = builder.completedAt;
        this.lastResetDate = builder.lastResetDate;

        validate();
    }

    private void validate() {
        if (playerId == null || playerId < 1) {
            throw new IllegalArgumentException(
                    "Player id cannot be null or less than 1"
            );
        }

        if (questId == null || questId < 1) {
            throw new IllegalArgumentException(
                    "Quest id cannot be null or less than 1"
            );
        }

        if (progress == null) {
            throw new IllegalArgumentException(
                    "Progress cannot be null"
            );
        }

        if (status == null) {
            throw new IllegalArgumentException(
                    "Quest status cannot be null"
            );
        }
    }

    /*
     * Compatibility method.
     *
     * Старый код использует isCompleted().
     * Пока оставляем его, чтобы не ломать
     * QuestService, DTO и frontend.
     */
    public boolean isCompleted() {
        return status == QuestStatus.COMPLETED;
    }

    public boolean isActive() {
        return status == QuestStatus.ACTIVE;
    }

    public boolean isLocked() {
        return status == QuestStatus.LOCKED;
    }

    /*
     * LOCKED -> ACTIVE
     */
    public PlayerQuest activate() {
        if (status != QuestStatus.LOCKED) {
            return this;
        }

        return copyBuilder()
                .status(QuestStatus.ACTIVE)
                .build();
    }

    /*
     * Увеличиваем прогресс конкретного действия.
     */
    public PlayerQuest increaseProgress(
            String action,
            int amount
    ) {
        if (status == QuestStatus.LOCKED) {
            throw new IllegalStateException(
                    "Cannot update progress of locked quest"
            );
        }

        if (status == QuestStatus.COMPLETED) {
            /*
             * Пока разрешаем продолжать прогресс после
             * выполнения Daily Quest.
             *
             * Это необходимо для Hidden Quest,
             * который срабатывает на двойной цели.
             */
        }

        Map<String, Integer> newProgress =
                new HashMap<>(this.progress);

        newProgress.put(
                action,
                newProgress.getOrDefault(action, 0) + amount
        );

        return copyBuilder()
                .progress(newProgress)
                .build();
    }

    /*
     * Проверяем выполнение всех целей.
     */
    public boolean isAllTargetsMet(
            Map<String, Integer> targets
    ) {
        for (
                Map.Entry<String, Integer> entry :
                targets.entrySet()
        ) {
            int current =
                    progress.getOrDefault(
                            entry.getKey(),
                            0
                    );

            if (current < entry.getValue()) {
                return false;
            }
        }

        return true;
    }

    /*
     * Проверяем двойные цели.
     *
     * Сейчас используется Hidden Quest.
     */
    public boolean isDoubleTargetsMet(
            Map<String, Integer> targets
    ) {
        for (
                Map.Entry<String, Integer> entry :
                targets.entrySet()
        ) {
            int current =
                    progress.getOrDefault(
                            entry.getKey(),
                            0
                    );

            if (current < entry.getValue() * 2) {
                return false;
            }
        }

        return true;
    }

    /*
     * ACTIVE -> COMPLETED
     */

    public PlayerQuest markCompleted() {
        return markCompleted(Clock.systemDefaultZone());
    }

    public PlayerQuest markCompleted(Clock clock) {
        if (status == QuestStatus.COMPLETED) {
            return this;
        }

        if (status == QuestStatus.LOCKED) {
            throw new IllegalStateException(
                    "Locked quest cannot be completed"
            );
        }

        return copyBuilder()
                .status(QuestStatus.COMPLETED)
                .completedAt(LocalDateTime.now(clock))
                .build();
    }


    /*
     * Сброс Daily Quest.
     *
     * После reset квест снова ACTIVE.
     */

    public PlayerQuest reset() {
        return reset(Clock.systemDefaultZone());
    }

    public PlayerQuest reset(Clock clock) {
        return copyBuilder()
                .progress(new HashMap<>())
                .status(QuestStatus.ACTIVE)
                .completedAt(null)
                .lastResetDate(LocalDate.now(clock))
                .build();
    }


    /*
     * Небольшой helper, чтобы при создании новой
     * immutable-модели не забывать поля.
     */
    private Builder copyBuilder() {
        return new Builder()
                .id(this.id)
                .playerId(this.playerId)
                .questId(this.questId)
                .progress(new HashMap<>(this.progress))
                .status(this.status)
                .completedAt(this.completedAt)
                .lastResetDate(this.lastResetDate);
    }

    public static class Builder {

        private Long id;
        private Long playerId;
        private Long questId;

        private Map<String, Integer> progress =
                new HashMap<>();

        /*
         * Пока ACTIVE по умолчанию.
         *
         * Благодаря этому существующие Daily/Hidden
         * продолжают работать.
         */
        private QuestStatus status =
                QuestStatus.ACTIVE;

        private LocalDateTime completedAt;

        private LocalDate lastResetDate =
                LocalDate.now();

        public Builder id(Long id) {
            this.id = id;
            return this;
        }

        public Builder playerId(Long playerId) {
            this.playerId = playerId;
            return this;
        }

        public Builder questId(Long questId) {
            this.questId = questId;
            return this;
        }

        public Builder progress(
                Map<String, Integer> progress
        ) {
            this.progress = progress;
            return this;
        }

        public Builder status(QuestStatus status) {
            this.status = status;
            return this;
        }

        public Builder completedAt(
                LocalDateTime completedAt
        ) {
            this.completedAt = completedAt;
            return this;
        }

        public Builder lastResetDate(
                LocalDate lastResetDate
        ) {
            this.lastResetDate = lastResetDate;
            return this;
        }

        public PlayerQuest build() {
            return new PlayerQuest(this);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (
                o == null ||
                        getClass() != o.getClass()
        ) {
            return false;
        }

        PlayerQuest that =
                (PlayerQuest) o;

        return Objects.equals(id, that.id)
                && Objects.equals(
                playerId,
                that.playerId
        )
                && Objects.equals(
                questId,
                that.questId
        )
                && status == that.status;
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                id,
                playerId,
                questId,
                status
        );
    }

    @Override
    public String toString() {
        return "PlayerQuest{" +
                "id=" + id +
                ", playerId=" + playerId +
                ", questId=" + questId +
                ", progress=" + progress +
                ", status=" + status +
                ", lastResetDate=" +
                lastResetDate +
                '}';
    }
}