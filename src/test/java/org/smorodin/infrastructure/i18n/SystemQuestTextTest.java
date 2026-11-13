package org.smorodin.infrastructure.i18n;

import org.junit.jupiter.api.Test;
import org.smorodin.core.model.Quest;
import org.smorodin.core.model.enums.QuestType;
import org.smorodin.core.model.enums.ResetInterval;
import org.springframework.context.support.ResourceBundleMessageSource;

import java.util.Locale;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SystemQuestTextTest {

    private final ResourceBundleMessageSource messages = new ResourceBundleMessageSource();
    private final SystemQuestText localizer;

    SystemQuestTextTest() {
        messages.setBasename("messages");
        messages.setDefaultEncoding("UTF-8");
        messages.setFallbackToSystemLocale(false);
        localizer = new SystemQuestText(new ApiText(messages));
    }

    @Test
    void shouldTranslateHiddenTemplateWithoutModifyingIt() {
        Quest hidden = new Quest.Builder()
                .type(QuestType.HIDDEN)
                .title("Overachiever")
                .description("Complete double the daily training")
                .targets(Map.of("pushups", 40))
                .build();

        assertEquals("Сверхдостижение",
                localizer.title(hidden, Locale.forLanguageTag("ru")));
        assertEquals("Выполни двойную норму ежедневной тренировки",
                localizer.description(hidden, Locale.forLanguageTag("ru")));
        assertEquals("Overachiever", localizer.title(hidden, Locale.ENGLISH));
        assertEquals("Complete double the daily training",
                localizer.description(hidden, Locale.ENGLISH));

        // Domain object stays unchanged: translations are presentation-only.
        assertEquals("Overachiever", hidden.getTitle());
        assertEquals("pushups", hidden.getTargets().keySet().iterator().next());
    }

    @Test
    void shouldNeverTranslatePersonalContent() {
        Quest personal = new Quest.Builder()
                .type(QuestType.PERSONAL)
                .ownerId(1L)
                .title("Keep my own title")
                .description("Keep my own description")
                .targets(Map.of("my_action", 5))
                .resetInterval(ResetInterval.NONE)
                .build();

        assertEquals("Keep my own title",
                localizer.title(personal, Locale.forLanguageTag("ru")));
        assertEquals("Keep my own description",
                localizer.description(personal, Locale.forLanguageTag("ru")));
    }
}
