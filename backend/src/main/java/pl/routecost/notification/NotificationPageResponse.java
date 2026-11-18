package pl.routecost.notification;

import org.springframework.data.domain.Page;

import java.util.List;

public record NotificationPageResponse(List<NotificationResponse> content, int page, int size,
                                       long totalElements, int totalPages) {
    public static NotificationPageResponse from(Page<UserNotification> notifications) {
        return new NotificationPageResponse(notifications.getContent().stream().map(NotificationResponse::from).toList(),
                notifications.getNumber(), notifications.getSize(), notifications.getTotalElements(), notifications.getTotalPages());
    }
}
