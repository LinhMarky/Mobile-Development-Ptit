package com.homely.student14.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpStatus;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

class EmailServiceTest {
    @Test
    void verificationEmailUsesConfiguredSenderAndSendsCodeOnlyToRequestedRecipient() {
        JavaMailSender mailSender = mock(JavaMailSender.class);
        EmailService emailService = new EmailService(mailSender);
        ReflectionTestUtils.setField(emailService, "mailFrom", "sender@example.test");

        emailService.sendVerificationCode("student@example.test", "123456");

        ArgumentCaptor<SimpleMailMessage> messageCaptor =
                ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(messageCaptor.capture());
        SimpleMailMessage message = messageCaptor.getValue();
        assertEquals("sender@example.test", message.getFrom());
        assertEquals(1, message.getTo().length);
        assertEquals("student@example.test", message.getTo()[0]);
        assertTrue(message.getText().contains("123456"));
        assertNull(message.getCc());
        assertNull(message.getBcc());
    }

    @Test
    void smtpFailureReturnsSafeServiceUnavailableError() {
        JavaMailSender mailSender = mock(JavaMailSender.class);
        EmailService emailService = new EmailService(mailSender);
        ReflectionTestUtils.setField(emailService, "mailFrom", "sender@example.test");
        doThrow(new MailSendException("private SMTP connection details"))
                .when(mailSender)
                .send(any(SimpleMailMessage.class));

        ResponseStatusException exception =
                assertThrows(
                        ResponseStatusException.class,
                        () -> emailService.sendRecoveryCode("student@example.test", "123456"));

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, exception.getStatusCode());
        assertFalse(exception.getReason().contains("private SMTP"));
        assertNull(exception.getCause());
    }
}
