package com.cloudmen.cloudguard.service.notification;

import com.cloudmen.cloudguard.domain.model.feedback.NotificationFeedback;
import com.cloudmen.cloudguard.exception.NotificationFeedbackValidationException;
import com.cloudmen.cloudguard.repository.NotificationFeedbackRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Validates and persists notification feedback; sends operator email via {@link FeedbackEmailService} when recipients configured.
 * Re-submitting text when a row already has body is ignored (returns existing row).
 */
@Service
public class NotificationFeedbackService {

    private final NotificationFeedbackRepository repository;
    private final FeedbackEmailService emailService;
    private final MessageSource messageSource;

    public NotificationFeedbackService(
            NotificationFeedbackRepository repository,
            FeedbackEmailService emailService,
            @Qualifier("messageSource") MessageSource messageSource) {
        this.repository = repository;
        this.emailService = emailService;
        this.messageSource = messageSource;
    }

    /**
     * Saves feedback for ({@code userId}, {@code source}, {@code notificationType}) or fills text on an empty placeholder row.
     *
     * @return persisted entity (possibly unchanged if duplicate non-blank text already stored)
     */
    @Transactional
    public NotificationFeedback submitFeedback(String userId, String source, String notificationType, String feedbackText) {
        validateSourceAndNotificationType(source, notificationType);

        Optional<NotificationFeedback> existingOpt =
                repository.findByUserIdAndSourceAndNotificationType(userId, source, notificationType);
        if (existingOpt.isPresent()) {
            NotificationFeedback existing = existingOpt.get();
            if (existing.getFeedbackText() != null && !existing.getFeedbackText().isBlank()) {
                return existing;
            }
            validateFeedbackText(feedbackText);
            existing.setFeedbackText(feedbackText);
            existing.setCreatedAt(LocalDateTime.now());
            emailService.sendFeedbackEmail(userId, source, notificationType, feedbackText);
            return repository.save(existing);
        }

        validateFeedbackText(feedbackText);
        NotificationFeedback notificationFeedback = new NotificationFeedback();
        notificationFeedback.setUserId(userId);
        notificationFeedback.setSource(source);
        notificationFeedback.setNotificationType(notificationType);
        notificationFeedback.setFeedbackText(feedbackText);
        emailService.sendFeedbackEmail(userId, source, notificationType, feedbackText);
        return repository.save(notificationFeedback);
    }

    private void validateSourceAndNotificationType(String source, String notificationType) {
        if (source == null || source.isBlank()) {
            throw new NotificationFeedbackValidationException(
                    messageSource.getMessage(
                            "api.feedback.validation.source_required", null, LocaleContextHolder.getLocale()));
        }
        if (notificationType == null || notificationType.isBlank()) {
            throw new NotificationFeedbackValidationException(
                    messageSource.getMessage(
                            "api.feedback.validation.notification_type_required",
                            null,
                            LocaleContextHolder.getLocale()));
        }
    }

    private void validateFeedbackText(String feedbackText) {
        if (feedbackText == null || feedbackText.isBlank()) {
            throw new NotificationFeedbackValidationException(
                    messageSource.getMessage(
                            "api.feedback.validation.feedback_text_required",
                            null,
                            LocaleContextHolder.getLocale()));
        }
    }

    /** {@code true} when stored feedback text is non-blank for this triple. */
    public boolean hasFeedback(String userId, String source, String notificationType){
        return repository.findByUserIdAndSourceAndNotificationType(userId,source,notificationType)
                .map(f->f.getFeedbackText()!=null && !f.getFeedbackText().isBlank())
                .orElse(false);
    }

    /** Keys {@code source:notificationType} with non-empty feedback for {@code userId}. */
    public Set<String> getFeedbackKeysForUser(String userId) {
        return repository.findByUserId(userId).stream()
                .filter(f -> f.getFeedbackText() != null && !f.getFeedbackText().isBlank())
                .map(f -> f.getSource() + ":" + f.getNotificationType())
                .collect(Collectors.toSet());
    }

    /** All keys with feedback across users — used to populate {@code hasReported} hints even when another user reported. */
    public Set<String> getAllFeedbackKeys() {
        return repository.findAllWithFeedback().stream()
                .map(f -> f.getSource() + ":" + f.getNotificationType())
                .collect(Collectors.toSet());
    }
}
