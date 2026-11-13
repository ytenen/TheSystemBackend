package org.smorodin.infrastructure.i18n;

import org.springframework.context.MessageSource;
import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.Map;

@Component
public class ApiText {
    private static final Map<String, String> ERROR_KEYS = Map.ofEntries(
            Map.entry("Wrong password", "error.auth.invalidCredentials"),
            Map.entry("Invalid player ID", "error.player.invalidId"),
            Map.entry("Name must be longer than 5 symbols", "error.player.invalidName"),
            Map.entry("Username must be at least 3 characters", "error.player.invalidName"),
            Map.entry("Level cant be lower than 1", "error.player.invalidLevel"),
            Map.entry("Exp cant be lower than 0", "error.player.invalidExperience"),
            Map.entry("exp to next level cant be lower than 0", "error.player.invalidExperience"),
            Map.entry("Exp amount must be positive", "error.player.expPositive"),
            Map.entry("Progress amount must be positive", "error.quest.amountPositive"),
            Map.entry("Action cannot be empty", "error.quest.actionRequired"),
            Map.entry("Quest not found", "error.quest.notFound"),
            Map.entry("Quest progress not found", "error.quest.progressNotFound"),
            Map.entry("Player quest not found", "error.quest.progressNotFound"),
            Map.entry("Daily quest not found", "error.quest.templateNotFound"),
            Map.entry("Daily quest template not found", "error.quest.templateNotFound"),
            Map.entry("Quest is locked", "error.quest.locked"),
            Map.entry("Cannot update progress of locked quest", "error.quest.locked"),
            Map.entry("Locked quest cannot be completed", "error.quest.locked"),
            Map.entry("Quest is already completed", "error.quest.completed"),
            Map.entry("Target is already completed", "error.quest.targetCompleted"),
            Map.entry("Invalid quest title", "error.quest.titleInvalid"),
            Map.entry("Quest type is required", "error.quest.titleInvalid"),
            Map.entry("Targets cannot be empty", "error.quest.targetsRequired"),
            Map.entry("Invalid quest target", "error.quest.targetInvalid"),
            Map.entry("Quest must contain between 1 and 20 targets", "error.quest.targetsCount"),
            Map.entry("Invalid target action", "error.quest.actionInvalid"),
            Map.entry("Request is required", "error.quest.requestRequired"),
            Map.entry("Description is too long", "error.quest.descriptionTooLong"),
            Map.entry("Deadline cannot be in the past", "error.quest.deadlinePast"),
            Map.entry("End date cannot be before start date", "error.quest.endBeforeStart"),
            Map.entry("Personal quest must have an owner", "error.quest.ownerRequired"),
            Map.entry("Personal quest cannot reset automatically", "error.quest.resetUnsupported"),
            Map.entry("System quest cannot have an owner", "error.quest.systemOwnerInvalid"),
            Map.entry("Update mode is required", "error.daily.modeRequired"),
            Map.entry("Update mode must be NOW", "error.daily.modeNow"),
            Map.entry("This method only supports TOMORROW", "error.daily.modeTomorrow"),
            Map.entry("Training must contain 1 to 20 exercises", "error.daily.targetsCount"),
            Map.entry("Invalid exercise name", "error.daily.exerciseNameInvalid"),
            Map.entry("Exercise target must be between 1 and 100000", "error.daily.exerciseTargetInvalid"),
            Map.entry("Avatar image is required", "error.avatar.required"),
            Map.entry("Avatar upload must not exceed 10 MB", "error.avatar.tooLarge"),
            Map.entry("Only PNG and JPEG avatars are supported", "error.avatar.format"),
            Map.entry("Unable to read avatar image", "error.avatar.readFailure"),
            Map.entry("Invalid avatar image", "error.avatar.invalid"),
            Map.entry("Image content does not match its type", "error.avatar.typeMismatch"),
            Map.entry("Image dimensions are too large", "error.avatar.dimensions"),
            Map.entry("Processed avatar exceeds 2 MB", "error.avatar.processedTooLarge"),
            Map.entry("Avatar not found", "error.avatar.notFound"),
            Map.entry("Player not found", "error.player.notFound")
    );

    private final MessageSource source;

    public ApiText(MessageSource source) {
        this.source = source;
    }

    public String get(String key, Locale locale, Object... args) {
        return source.getMessage(key, args, locale);
    }

    /**
     * Legacy domain/service exceptions contain English diagnostics. Map only
     * known reasons to public resource keys; never return arbitrary exception text.
     */
    public String error(String reason, Locale locale) {
        if (reason != null && reason.startsWith("Unknown quest action: ")) {
            String action = reason.substring("Unknown quest action: ".length());
            return get("error.quest.unknownAction", locale, action);
        }
        if (reason != null && reason.startsWith("Player with id: ")) {
            return get("error.player.notFound", locale);
        }
        if ("Wrong password".equals(reason)) {
            return get("error.auth.invalidCredentials", locale);
        }
        if (reason == null) {
            return get("error.request.invalid", locale);
        }
        if (reason.startsWith("No enum constant org.smorodin.core.model.enums.StatType.")) {
            return get("error.player.invalidStat", locale);
        }
        String key = ERROR_KEYS.getOrDefault(reason, "error.request.invalid");
        return get(key, locale);
    }
}
