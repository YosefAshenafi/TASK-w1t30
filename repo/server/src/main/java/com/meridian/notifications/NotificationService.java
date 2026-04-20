package com.meridian.notifications;

import com.meridian.notifications.entity.InAppNotification;
import com.meridian.notifications.repository.InAppNotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final InAppNotificationRepository notificationRepository;

    @Transactional(propagation = Propagation.REQUIRED)
    public void send(UUID userId, String templateKey, String payloadJson) {
        notificationRepository.save(InAppNotification.of(userId, templateKey, payloadJson));
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public void sendToAll(List<UUID> userIds, String templateKey, String payloadJson) {
        for (UUID uid : userIds) {
            send(uid, templateKey, payloadJson);
        }
    }
}
