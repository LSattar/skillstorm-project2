package com.skillstorm.reserveone.services;

import java.time.format.DateTimeFormatter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import com.skillstorm.reserveone.models.Reservation;
import com.skillstorm.reserveone.models.User;

@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("MMMM d, yyyy");

    private final JavaMailSender mailSender;

    @Value("${mail.from}")
    private String fromEmail;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    /**
     * Sends a reservation confirmation email to the user.
     * 
     * @param reservation The reservation details
     * @param user The user who made the reservation
     */
    public void sendReservationConfirmation(Reservation reservation, User user) {
        String userEmail = user.getEmail();
        
        // Only send email if user has an email address
        if (userEmail == null || userEmail.isBlank()) {
            log.warn("Cannot send confirmation email: user {} has no email address", user.getUserId());
            return;
        }

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(userEmail);
            message.setSubject("Reservation Confirmation - " + reservation.getHotel().getName());

            String emailBody = buildReservationConfirmationEmail(reservation, user);
            message.setText(emailBody);

            mailSender.send(message);
            log.info("Reservation confirmation email sent successfully to {} for reservation {}", 
                    userEmail, reservation.getReservationId());

        } catch (Exception e) {
            // Log error but don't throw - we don't want email failures to break reservation creation
            log.error("Failed to send reservation confirmation email to {} for reservation {}: {}", 
                    userEmail, reservation.getReservationId(), e.getMessage(), e);
        }
    }

    private String buildReservationConfirmationEmail(Reservation reservation, User user) {
        StringBuilder body = new StringBuilder();
        
        // Greeting
        String guestName = formatGuestName(user);
        body.append("Dear ").append(guestName).append(",\n\n");
        
        // Confirmation message
        body.append("Thank you for your reservation! We're pleased to confirm your booking with us.\n\n");
        
        // Reservation details
        body.append("RESERVATION DETAILS:\n");
        body.append("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n");
        body.append("Reservation ID: ").append(reservation.getReservationId()).append("\n");
        body.append("Hotel: ").append(reservation.getHotel().getName()).append("\n");
        body.append("Address: ").append(formatAddress(reservation.getHotel())).append("\n");
        body.append("Phone: ").append(reservation.getHotel().getPhone()).append("\n\n");
        
        // Check-in/Check-out
        body.append("Check-in:  ").append(reservation.getStartDate().format(DATE_FORMATTER)).append("\n");
        body.append("Check-out: ").append(reservation.getEndDate().format(DATE_FORMATTER)).append("\n\n");
        
        // Room details
        body.append("Room: ").append(reservation.getRoom().getRoomNumber()).append("\n");
        body.append("Room Type: ").append(reservation.getRoomType().getName()).append("\n");
        body.append("Guests: ").append(reservation.getGuestCount()).append("\n\n");
        
        // Pricing
        if (reservation.getTotalAmount() != null && reservation.getTotalAmount().compareTo(java.math.BigDecimal.ZERO) > 0) {
            body.append("Total Amount: ").append(reservation.getCurrency()).append(" ")
                .append(reservation.getTotalAmount()).append("\n\n");
        }
        
        // Special requests
        if (reservation.getSpecialRequests() != null && !reservation.getSpecialRequests().isBlank()) {
            body.append("Special Requests: ").append(reservation.getSpecialRequests()).append("\n\n");
        }
        
        // Status
        body.append("Status: ").append(reservation.getStatus().name()).append("\n\n");
        
        // Closing
        body.append("We look forward to welcoming you!\n\n");
        body.append("If you have any questions or need to make changes to your reservation, ");
        body.append("please contact us at ").append(reservation.getHotel().getPhone()).append(".\n\n");
        body.append("Best regards,\n");
        body.append(reservation.getHotel().getName());
        
        return body.toString();
    }

    private String formatGuestName(User user) {
        String firstName = user.getFirstName();
        String lastName = user.getLastName();
        
        if (firstName != null && !firstName.isBlank() && lastName != null && !lastName.isBlank()) {
            return firstName + " " + lastName;
        } else if (firstName != null && !firstName.isBlank()) {
            return firstName;
        } else if (lastName != null && !lastName.isBlank()) {
            return lastName;
        } else {
            return "Guest";
        }
    }

    private String formatAddress(com.skillstorm.reserveone.models.Hotel hotel) {
        StringBuilder address = new StringBuilder();
        address.append(hotel.getAddress1());
        
        if (hotel.getAddress2() != null && !hotel.getAddress2().isBlank()) {
            address.append(", ").append(hotel.getAddress2());
        }
        
        address.append(", ").append(hotel.getCity())
               .append(", ").append(hotel.getState())
               .append(" ").append(hotel.getZip());
        
        return address.toString();
    }
}
