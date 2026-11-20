package pl.routecost.reminder;

import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import pl.routecost.notification.UserNotificationRepository;
import pl.routecost.notification.UserNotification;
import pl.routecost.user.JwtService;
import pl.routecost.user.User;
import pl.routecost.user.UserRepository;
import pl.routecost.vehicle.FuelType;
import pl.routecost.vehicle.Vehicle;
import pl.routecost.vehicle.VehicleRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "routecost.jwt.secret-base64=MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=",
        "routecost.reminders.scan-delay-ms=3600000",
        "routecost.reminders.publish-delay-ms=3600000"
})
@org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
class ReminderIntegrationTests extends pl.routecost.AbstractIntegrationTest {

    private static final Pattern ID_PATTERN = Pattern.compile("\"id\":(\\d+)");
    private final Map<String, Long> ownerIdsByToken = new HashMap<>();

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private VehicleRepository vehicleRepository;

    @Autowired
    private ReminderRepository reminderRepository;

    @Autowired
    private ReminderNotificationRepository notificationRepository;

    @Autowired
    private ReminderNotificationService notificationService;

    @Autowired
    private ReminderNotificationPublisher notificationPublisher;

    @Autowired
    private ReminderNotificationProcessor notificationProcessor;

    @Autowired
    private ReminderNotificationConsumer notificationConsumer;

    @Autowired
    private UserNotificationRepository userNotificationRepository;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private JwtService jwtService;

    @org.junit.jupiter.api.AfterEach
    void removeUsersCreatedByTest() {
        userRepository.deleteAllById(ownerIdsByToken.values());
        ownerIdsByToken.clear();
    }

    @Test
    void createsFiltersTransitionsAndProtectsReminderCrud() throws Exception {
        String ownerToken = createUserToken();
        Long vehicleId = createVehicle(ownerId(ownerToken));
        LocalDate firstDueDate = LocalDate.now().plusDays(20);
        String reminderId = createReminder(ownerToken, vehicleId, firstDueDate, "SERVICE", 15_000);
        String otherUserToken = createUserToken();

        mockMvc.perform(get("/api/vehicles/{vehicleId}/reminders", vehicleId)
                        .header("Authorization", bearer(ownerToken))
                        .param("status", "ACTIVE")
                        .param("dueFrom", LocalDate.now().plusDays(10).toString())
                        .param("dueTo", LocalDate.now().plusDays(25).toString())
                        .param("size", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].status").value("ACTIVE"));

        mockMvc.perform(get("/api/vehicles/{vehicleId}/reminders/{reminderId}", vehicleId, reminderId)
                        .header("Authorization", bearer(otherUserToken)))
                .andExpect(status().isNotFound());

        mockMvc.perform(put("/api/vehicles/{vehicleId}/reminders/{reminderId}", vehicleId, reminderId)
                        .header("Authorization", bearer(ownerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request("2026-12-20", "OIL_CHANGE", "Oil change", 16_000)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.type").value("OIL_CHANGE"));

        mockMvc.perform(patch("/api/vehicles/{vehicleId}/reminders/{reminderId}/status", vehicleId, reminderId)
                        .header("Authorization", bearer(ownerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"COMPLETED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"));

        mockMvc.perform(patch("/api/vehicles/{vehicleId}/reminders/{reminderId}/status", vehicleId, reminderId)
                        .header("Authorization", bearer(ownerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"ACTIVE\"}"))
                .andExpect(status().isConflict());

        mockMvc.perform(post("/api/vehicles/{vehicleId}/reminders/{reminderId}/reactivate", vehicleId, reminderId)
                        .header("Authorization", bearer(ownerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.activationVersion").value(2));

        mockMvc.perform(patch("/api/vehicles/{vehicleId}/reminders/{reminderId}/status", vehicleId, reminderId)
                        .header("Authorization", bearer(otherUserToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"CANCELLED\"}"))
                .andExpect(status().isNotFound());

        mockMvc.perform(delete("/api/vehicles/{vehicleId}/reminders/{reminderId}", vehicleId, reminderId)
                        .header("Authorization", bearer(otherUserToken)))
                .andExpect(status().isNotFound());

        mockMvc.perform(delete("/api/vehicles/{vehicleId}/reminders/{reminderId}", vehicleId, reminderId)
                        .header("Authorization", bearer(ownerToken)))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/vehicles/{vehicleId}/reminders/{reminderId}", vehicleId, reminderId)
                        .header("Authorization", bearer(ownerToken)))
                .andExpect(status().isNotFound());
    }

    @Test
    void detectsUpcomingAndOverdueRemindersOnceAndConsumesRabbitEventsIdempotently() throws Exception {
        String token = createUserToken();
        Long vehicleId = createVehicle(ownerId(token));
        LocalDate today = LocalDate.now();
        String upcomingId = createReminder(token, vehicleId, today.plusDays(3), "INSURANCE", null);
        String overdueId = createReminder(token, vehicleId, today.minusDays(2), "TECHNICAL_INSPECTION", null);
        String farFutureId = createReminder(token, vehicleId, today.plusDays(30), "TIRES", null);
        String cancelledSoonId = createReminder(token, vehicleId, today.plusDays(2), "OTHER", null);
        Reminder cancelledSoon = reminderRepository.findById(Long.valueOf(cancelledSoonId)).orElseThrow();
        cancelledSoon.changeStatus(ReminderStatus.CANCELLED);
        reminderRepository.saveAndFlush(cancelledSoon);

        int firstScan = notificationService.scanDueReminders();
        int secondScan = notificationService.scanDueReminders();

        assertEquals(2, firstScan);
        assertEquals(0, secondScan);
        assertEquals(1, notificationRepository.countByReminder_Id(Long.valueOf(upcomingId)));
        assertEquals(1, notificationRepository.countByReminder_Id(Long.valueOf(overdueId)));
        assertEquals(0, notificationRepository.countByReminder_Id(Long.valueOf(farFutureId)));
        assertEquals(0, notificationRepository.countByReminder_Id(Long.valueOf(cancelledSoonId)));

        ReminderNotification upcoming = notificationRepository
                .findAllByReminder_IdOrderByCreatedAtAsc(Long.valueOf(upcomingId)).getFirst();
        ReminderNotification overdue = notificationRepository
                .findAllByReminder_IdOrderByCreatedAtAsc(Long.valueOf(overdueId)).getFirst();
        assertEquals(ReminderNotificationKind.UPCOMING, upcoming.getKind());
        assertEquals(ReminderNotificationKind.OVERDUE, overdue.getKind());

        assertEquals(2, notificationPublisher.publishPending());
        awaitProcessed(List.of(upcoming.getId(), overdue.getId()));
        assertEquals(0, notificationPublisher.publishPending());

        mockMvc.perform(patch("/api/vehicles/{vehicleId}/reminders/{reminderId}/status", vehicleId, upcomingId)
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"COMPLETED\"}"))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/vehicles/{vehicleId}/reminders/{reminderId}/reactivate", vehicleId, upcomingId)
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activationVersion").value(2));

        assertEquals(1, notificationService.scanDueReminders());
        assertEquals(2, notificationRepository.countByReminder_Id(Long.valueOf(upcomingId)));
        ReminderNotification reactivatedEvent = notificationRepository
                .findAllByReminder_IdOrderByCreatedAtAsc(Long.valueOf(upcomingId)).getLast();
        assertEquals(2, reactivatedEvent.getActivationVersion());
        assertEquals(1, notificationPublisher.publishPending());
        awaitProcessed(List.of(reactivatedEvent.getId()));

        rabbitTemplate.convertAndSend(
                "",
                ReminderConfiguration.NOTIFICATION_QUEUE,
                new ReminderNotificationEvent(upcoming.getId()).toQueuePayload()
        );
        Thread.sleep(250);
        ReminderNotification duplicateDelivery = notificationRepository.findById(upcoming.getId()).orElseThrow();
        assertEquals(ReminderNotificationStatus.PROCESSED, duplicateDelivery.getStatus());
        assertEquals(1, userNotificationRepository.countBySource_Id(upcoming.getId()));
    }

    @Test
    void createsOwnedNotificationsSupportsReadStateAndRejectsOtherUsers() throws Exception {
        String ownerToken = createUserToken();
        Long vehicleId = createVehicle(ownerId(ownerToken));
        String reminderId = createReminder(ownerToken, vehicleId, LocalDate.now().plusDays(1), "SERVICE", null);
        notificationService.scanDueReminders();
        ReminderNotification technical = notificationRepository.findAllByReminder_IdOrderByCreatedAtAsc(Long.valueOf(reminderId)).getFirst();
        notificationPublisher.publishPending();
        awaitProcessed(List.of(technical.getId()));
        UserNotification userNotification = userNotificationRepository.findAll().stream()
                .filter(item -> item.getSource().getId().equals(technical.getId())).findFirst().orElseThrow();

        mockMvc.perform(get("/api/notifications/unread-count").header("Authorization", bearer(ownerToken)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.count").value(1));
        mockMvc.perform(get("/api/notifications").header("Authorization", bearer(ownerToken)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].title").value("Reminder"))
                .andExpect(jsonPath("$.content[0].vehicleId").value(vehicleId))
                .andExpect(jsonPath("$.content[0].read").value(false));

        String otherToken = createUserToken();
        mockMvc.perform(get("/api/notifications").header("Authorization", bearer(otherToken)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.content.length()").value(0));
        mockMvc.perform(patch("/api/notifications/{id}/read", userNotification.getId())
                        .header("Authorization", bearer(otherToken)))
                .andExpect(status().isNotFound());

        mockMvc.perform(patch("/api/notifications/{id}/read", userNotification.getId())
                        .header("Authorization", bearer(ownerToken)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.read").value(true));
        mockMvc.perform(get("/api/notifications/unread-count").header("Authorization", bearer(ownerToken)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.count").value(0));

        String nextReminderId = createReminder(ownerToken, vehicleId, LocalDate.now().plusDays(2), "INSURANCE", null);
        notificationService.scanDueReminders();
        ReminderNotification next = notificationRepository.findAllByReminder_IdOrderByCreatedAtAsc(Long.valueOf(nextReminderId)).getFirst();
        notificationPublisher.publishPending();
        awaitProcessed(List.of(next.getId()));
        mockMvc.perform(patch("/api/notifications/read-all").header("Authorization", bearer(ownerToken)))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/notifications/unread-count").header("Authorization", bearer(ownerToken)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.count").value(0));
    }

    @Test
    void discardsStaleReminderWithoutCreatingUserNotification() throws Exception {
        String token = createUserToken();
        Long vehicleId = createVehicle(ownerId(token));
        String reminderId = createReminder(token, vehicleId, LocalDate.now().plusDays(1), "SERVICE", null);
        notificationService.scanDueReminders();
        ReminderNotification technical = notificationRepository.findAllByReminder_IdOrderByCreatedAtAsc(Long.valueOf(reminderId)).getFirst();
        technical.markPublished(java.time.OffsetDateTime.now());
        notificationRepository.saveAndFlush(technical);
        Reminder staleReminder = reminderRepository.findById(Long.valueOf(reminderId)).orElseThrow();
        staleReminder.changeStatus(ReminderStatus.CANCELLED);
        reminderRepository.saveAndFlush(staleReminder);
        notificationProcessor.process(new ReminderNotificationEvent(technical.getId()));

        assertEquals(ReminderNotificationStatus.DISCARDED,
                notificationRepository.findById(technical.getId()).orElseThrow().getStatus());
        assertEquals(0, userNotificationRepository.countBySource_Id(technical.getId()));
    }

    @Test
    void retriesInvalidMessageThenRoutesItToDeadLetterQueue() {
        rabbitTemplate.convertAndSend("", ReminderConfiguration.NOTIFICATION_QUEUE, "not-a-notification-id");
        Object deadLetter = rabbitTemplate.receiveAndConvert(ReminderConfiguration.NOTIFICATION_DEAD_LETTER_QUEUE, 10_000);
        org.junit.jupiter.api.Assertions.assertEquals("not-a-notification-id", deadLetter);
    }

    @Test
    void migratesPendingMessagesFromThePreviouslyDeclaredQueue() throws Exception {
        String token = createUserToken();
        Long vehicleId = createVehicle(ownerId(token));
        String reminderId = createReminder(token, vehicleId, LocalDate.now().plusDays(1), "SERVICE", null);
        notificationService.scanDueReminders();
        ReminderNotification technical = notificationRepository.findAllByReminder_IdOrderByCreatedAtAsc(Long.valueOf(reminderId)).getFirst();
        technical.markPublished(java.time.OffsetDateTime.now());
        notificationRepository.saveAndFlush(technical);

        notificationConsumer.migrateLegacy(new ReminderNotificationEvent(technical.getId()).toQueuePayload());
        awaitProcessed(List.of(technical.getId()));
        assertEquals(1, userNotificationRepository.countBySource_Id(technical.getId()));
    }

    @Test
    void validatesReminderFieldsAndDateRange() throws Exception {
        String token = createUserToken();
        Long vehicleId = createVehicle(ownerId(token));

        mockMvc.perform(post("/api/vehicles/{vehicleId}/reminders", vehicleId)
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request(LocalDate.now().plusDays(2).toString(), "SERVICE", " ", null)))
                .andExpect(status().isBadRequest());

        mockMvc.perform(post("/api/vehicles/{vehicleId}/reminders", vehicleId)
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request("2020-01-01", "SERVICE", "Before purchase", null)))
                .andExpect(status().isBadRequest());

        mockMvc.perform(post("/api/vehicles/{vehicleId}/reminders", vehicleId)
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request(LocalDate.now().plusDays(2).toString(), "SERVICE", "Negative odo", -1)))
                .andExpect(status().isBadRequest());

        mockMvc.perform(get("/api/vehicles/{vehicleId}/reminders", vehicleId)
                        .header("Authorization", bearer(token))
                        .param("dueFrom", "2026-12-31")
                        .param("dueTo", "2026-01-01"))
                .andExpect(status().isBadRequest());
    }

    private void awaitProcessed(List<Long> notificationIds) throws InterruptedException {
        for (int attempt = 0; attempt < 60; attempt++) {
            boolean allProcessed = notificationIds.stream()
                    .map(id -> notificationRepository.findById(id).orElseThrow())
                    .allMatch(notification -> notification.getStatus() == ReminderNotificationStatus.PROCESSED);
            if (allProcessed) {
                return;
            }
            Thread.sleep(100);
        }
        throw new AssertionError("RabbitMQ reminder notifications were not processed in time.");
    }

    private String createReminder(
            String token,
            Long vehicleId,
            LocalDate dueDate,
            String type,
            Integer odometer
    ) throws Exception {
        String response = mockMvc.perform(post("/api/vehicles/{vehicleId}/reminders", vehicleId)
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request(dueDate.toString(), type, "Reminder", odometer)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        Matcher matcher = ID_PATTERN.matcher(response);
        if (!matcher.find()) {
            throw new AssertionError("Reminder response did not contain an id");
        }
        return matcher.group(1);
    }

    private Long createVehicle(Long ownerId) {
        User owner = userRepository.findById(ownerId).orElseThrow();
        Vehicle vehicle = new Vehicle(
                owner,
                "Toyota",
                "Corolla",
                2018,
                FuelType.PETROL,
                15_000,
                LocalDate.of(2022, 6, 15),
                new BigDecimal("12500.00")
        );
        return vehicleRepository.saveAndFlush(vehicle).getId();
    }

    private String createUserToken() {
        User owner = userRepository.saveAndFlush(new User(
                "reminder-" + UUID.randomUUID() + "@routecost.example",
                "test-hash",
                "Reminder",
                "Owner"
        ));
        String token = jwtService.createAuthResponse(owner).accessToken();
        ownerIdsByToken.put(token, owner.getId());
        return token;
    }

    private Long ownerId(String token) {
        return ownerIdsByToken.get(token);
    }

    private String request(String date, String type, String title, Integer odometer) {
        String odometerJson = odometer == null ? "null" : odometer.toString();
        return """
                {"type":"%s","title":"%s","description":null,"dueDate":"%s","odometer":%s}
                """.formatted(type, title, date, odometerJson);
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }
}
