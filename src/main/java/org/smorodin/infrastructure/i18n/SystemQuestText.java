package org.smorodin.infrastructure.i18n;

import org.smorodin.core.model.Quest;
import org.smorodin.core.model.enums.QuestType;
import org.springframework.stereotype.Component;

import java.util.Locale;

@Component
public class SystemQuestText {

    private final ApiText text;

    public SystemQuestText(ApiText text) {
        this.text = text;
    }

    public String title(Quest quest, Locale locale) {
        String key = keyFor(quest, "title");
        return key == null ? quest.getTitle() : text.get(key, locale);
    }

    public String description(Quest quest, Locale locale) {
        String key = keyFor(quest, "description");
        return key == null ? quest.getDescription() : text.get(key, locale);
    }

    private String keyFor(Quest quest, String field) {
        if (quest.getOwnerId() != null) {
            return null;
        }
        if (quest.getType() == QuestType.DAILY) {
            return "quest.system.daily." + field;
        }
        if (quest.getType() == QuestType.HIDDEN) {
            return "quest.system.hidden." + field;
        }
        return null;
    }
}
