package org.smorodin.infrastructure.persistence.quest;

import org.smorodin.core.model.Quest;
import org.smorodin.core.model.enums.QuestType;
import org.smorodin.core.model.enums.ResetInterval;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class QuestDataInitializer implements CommandLineRunner {

    private final QuestRepository questRepository;

    public QuestDataInitializer(QuestRepository questRepository) {
        this.questRepository = questRepository;
    }

    @Override
    public void run(String... args) {

        if (questRepository.findByType(QuestType.DAILY).isEmpty()) {
            Quest dailyQuest = createDailyQuest();

            questRepository.save(
                    QuestEntity.fromDomain(dailyQuest)
            );
        }

        if (questRepository.findByType(QuestType.HIDDEN).isEmpty()) {
            Quest hiddenQuest = createHiddenQuest();

            questRepository.save(
                    QuestEntity.fromDomain(hiddenQuest)
            );
        }
    }

    private Quest createDailyQuest() {
        return new Quest.Builder()
                .type(QuestType.DAILY)
                .title("Daily Training")
                .description("Complete your daily training")
                .targets(Map.of(
                        "pushups", 20,
                        "squats", 30,
                        "plank", 60
                ))
                .expReward(100)
                .statReward("STRENGTH")
                .statRewardAmount(1)
                .resetInterval(ResetInterval.DAILY)
                .build();
    }

    private Quest createHiddenQuest() {
        return new Quest.Builder()
                .type(QuestType.HIDDEN)
                .title("Overachiever")
                .description("Complete double the daily training")
                .targets(Map.of(
                        "pushups", 40,
                        "squats", 60,
                        "plank", 120
                ))
                .expReward(200)
                .statReward("AGILITY")
                .statRewardAmount(1)
                .resetInterval(ResetInterval.DAILY)
                .build();
    }
}