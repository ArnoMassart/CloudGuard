package com.cloudmen.cloudguard.service.notification;

import com.cloudmen.cloudguard.domain.feedback.NotificationFeedback;
import com.cloudmen.cloudguard.repository.NotificationFeedbackRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class NotificationFeedbackService {

    private final NotificationFeedbackRepository repository;
    private final FeedbackEmailService emailService;

    public NotificationFeedbackService(NotificationFeedbackRepository repository,  FeedbackEmailService emailService) {
        this.repository = repository;
        this.emailService = emailService;
    }

    @Transactional
    public NotificationFeedback submitFeedback(String userId, String source, String notificationType, String feedbackText) {
        return repository.findByUserIdAndSourceAndNotificationType(userId, source, notificationType)
                .map(existing -> {
                    if(existing.getFeedbackText()!=null && !existing.getFeedbackText().isBlank()){
                        return existing;
                    }
                    existing.setFeedbackText(feedbackText);
                    existing.setCreatedAt(LocalDateTime.now());

                    emailService.sendFeedbackEmail(userId, source, notificationType, feedbackText);
                    return repository.save(existing);
                })
                .orElseGet(()->{
                    NotificationFeedback notificationFeedback = new NotificationFeedback();
                    notificationFeedback.setUserId(userId);
                    notificationFeedback.setSource(source);
                    notificationFeedback.setNotificationType(notificationType);
                    notificationFeedback.setFeedbackText(feedbackText);

                    emailService.sendFeedbackEmail(userId, source, notificationType, feedbackText);
                    return repository.save(notificationFeedback);
                });
    }

    public boolean hasFeedback(String userId, String source, String notificationType){
        return repository.findByUserIdAndSourceAndNotificationType(userId,source,notificationType)
                .map(f->f.getFeedbackText()!=null && !f.getFeedbackText().isBlank())
                .orElse(false);
    }

    public Set<String> getFeedbackKeysForUser(String userId) {
        return repository.findByUserId(userId).stream()
                .filter(f -> f.getFeedbackText() != null && !f.getFeedbackText().isBlank())
                .map(f -> f.getSource() + ":" + f.getNotificationType())
                .collect(Collectors.toSet());
    }
}
