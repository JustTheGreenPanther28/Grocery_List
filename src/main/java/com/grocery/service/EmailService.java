package com.grocery.service;

import com.grocery.dto.EmailSendResponse;
import com.grocery.model.GroceryItem;
import com.grocery.model.SentMessage;
import com.grocery.repository.SentMessageRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class EmailService {

    @Value("${spring.mail.username:}")
    private String fromAddress;

    private final JavaMailSender mailSender;
    private final SentMessageRepository sentMessageRepository;

    public EmailService(JavaMailSender mailSender, SentMessageRepository sentMessageRepository) {
        this.mailSender = mailSender;
        this.sentMessageRepository = sentMessageRepository;
    }

    // automatic=false for a manual "Send by Email" tap, true when the scheduler fired it.
    public EmailSendResponse sendGroceryList(String username, String toEmail, LocalDate date,
                                              List<GroceryItem> items, boolean automatic) {
        String text = GroceryListMessageFormatter.build(date, items);
        boolean configured = fromAddress != null && !fromAddress.isBlank();

        EmailSendResponse response;
        if (!configured) {
            response = new EmailSendResponse(false,
                    "Gmail sending isn't configured on the server - set GMAIL_USERNAME and GMAIL_APP_PASSWORD.");
        } else {
            try {
                SimpleMailMessage message = new SimpleMailMessage();
                message.setFrom(fromAddress);
                message.setTo(toEmail);
                message.setSubject("Grocery List - " + date.format(DateTimeFormatter.ISO_LOCAL_DATE));
                message.setText(text);
                mailSender.send(message);
                response = new EmailSendResponse(true, "Email sent.");
            } catch (Exception ex) {
                response = new EmailSendResponse(false, "Could not send email: " + ex.getMessage());
            }
        }

        sentMessageRepository.save(new SentMessage(username, date, toEmail, "EMAIL", text,
                response.isSent(), automatic));
        return response;
    }
}
