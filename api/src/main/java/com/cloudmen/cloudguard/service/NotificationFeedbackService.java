package com.cloudmen.cloudguard.service;

import com.cloudmen.cloudguard.domain.feedback.NotificationFeedback;
import com.cloudmen.cloudguard.repository.NotificationFeedbackRepository;
import com.cloudmen.cloudguard.repository.NotificationRepository;
import com.cloudmen.cloudguard.utility.GoogleApiFactory;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class NotificationFeedbackService {

    private final GoogleApiFactory apiFactory;
    private final NotificationFeedbackRepository repository;

    public NotificationFeedbackService(GoogleApiFactory apiFactory, NotificationFeedbackRepository repository) {
        this.apiFactory = apiFactory;
        this.repository = repository;
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
                    return repository.save(existing);
                })
                .orElseGet(()->{
                    NotificationFeedback notificationFeedback = new NotificationFeedback();
                    notificationFeedback.setUserId(userId);
                    notificationFeedback.setSource(source);
                    notificationFeedback.setNotificationType(notificationType);
                    notificationFeedback.setFeedbackText(feedbackText);
                    return repository.save(notificationFeedback);
                });
    }

    public boolean hasFeedback(String userId, String source, String notificationType){
        return repository.findByUserIdAndSourceAndNotificationType(userId,source,notificationType)
                .map(f->f.getFeedbackText()!=null && !f.getFeedbackText().isBlank())
                .orElse(false);
    }
}
