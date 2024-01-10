package com.hacknosis.backend.utils;

import com.google.api.services.gmail.model.Message;
import com.hacknosis.backend.dto.ZoomMeetingObject;
import com.hacknosis.backend.models.Appointment;
import com.hacknosis.backend.models.Patient;
import com.hacknosis.backend.services.ZoomService;
import org.apache.commons.codec.binary.Base64;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Session;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import javax.mail.util.ByteArrayDataSource;
import java.io.*;
import java.time.format.DateTimeFormatter;
import java.util.Properties;

@Component
public class EmailUtil {
    @Value("${email.user}")
    public String fromEmailAddress;

    @Autowired
    public ZoomService zoomService;
    private static final String confirmationIconSource = "https://w7.pngwing.com/pngs/537/407/png-transparent-verified-check-mark-confirmation-checkbox-passed-icon.png";
    public MimeMessage createConfirmationEmail(Patient patient, Appointment appointment)
            throws MessagingException, IOException {
        Properties props = new Properties();
        Session session = Session.getDefaultInstance(props, null);

        MimeMessage email = new MimeMessage(session);

        email.setFrom(new InternetAddress(fromEmailAddress));
        email.addRecipient(javax.mail.Message.RecipientType.TO, new InternetAddress(patient.getEmail()));
        email.setSubject("Appointment Confirmation");

        ZoomMeetingObject zoomMeetingObject = null;
        if (appointment.getRemote()) {
            zoomMeetingObject = zoomService.createMeeting(appointment);
            appointment.setMeetingId(zoomMeetingObject.getId());
            // zoomService.registerPatient(patient, zoomMeetingObject.getId());
        }

        FileReader fileReader = new FileReader("src/main/resources/email_templates/confirmation.html");
        BufferedReader bufferedReader = new BufferedReader(fileReader);

        StringBuilder htmlContent = new StringBuilder();
        String line;
        while ((line = bufferedReader.readLine()) != null) {
            htmlContent.append(line);
        }
        bufferedReader.close();
        fileReader.close();

        String content = htmlContent.toString();
        content = content.replace("{usn}", patient.getName());
        content = content.replace("{img_src}", confirmationIconSource);
        content = content.replace("{location}", appointment.getLocation());
        content = content.replace("{provider}", appointment.getMainProvider());
        content = content.replace("{time}", appointment.getAppointmentTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")).concat("UTC"));
        content = content.replace("{drn}", patient.getUser().getName());
        if (appointment.getRemote() && zoomMeetingObject != null) {
            content = content.replace("{extra_info}", String.format("Zoom Meeting ID: %s<br>Zoom Meeting Password: hacknosis<br>Consult customer service for assistance", zoomMeetingObject.getId()));
        }

        //email.setText(bodyText); // default mime type of text/plain
        Multipart multipart = new MimeMultipart();

        MimeBodyPart messageBodyPart = new MimeBodyPart();
        messageBodyPart.setContent(content, "text/html; charset=utf-8");
        multipart.addBodyPart(messageBodyPart);

        /*
        // Attach file
        MimeBodyPart attachmentBodyPart = buildAttachmentFromMultipartFile(null);
        multipart.addBodyPart(attachmentBodyPart);
        */

        // Set the Multipart object as the content of the email
        email.setContent(multipart);

        return email;
    }

    public MimeMessage createIssueEmail(MultipartFile screenshot,String issueDescription,String timestamp,String reporterID)
            throws MessagingException, IOException {
        Properties props = new Properties();
        Session session = Session.getDefaultInstance(props, null);

        MimeMessage email = new MimeMessage(session);

        email.setFrom(new InternetAddress(fromEmailAddress));
        email.addRecipient(javax.mail.Message.RecipientType.TO, new InternetAddress("gracexuwt1126@gmail.com"));
        email.setSubject("Issue Ticket");

        FileReader fileReader = new FileReader("src/main/resources/email_templates/issue.html");
        BufferedReader bufferedReader = new BufferedReader(fileReader);

        StringBuilder htmlContent = new StringBuilder();
        String line;
        while ((line = bufferedReader.readLine()) != null) {
            htmlContent.append(line);
        }
        bufferedReader.close();
        fileReader.close();

        String content = htmlContent.toString();
        content = content.replace("{issueDescription}", issueDescription);
        content = content.replace("{timestamp}", timestamp);
        content = content.replace("{reporterID}", reporterID);

        //email.setText(bodyText); // default mime type of text/plain
        Multipart multipart = new MimeMultipart();

        MimeBodyPart messageBodyPart = new MimeBodyPart();
        messageBodyPart.setContent(content, "text/html; charset=utf-8");
        multipart.addBodyPart(messageBodyPart);

        // Attach file
        MimeBodyPart attachmentBodyPart = buildAttachmentFromMultipartFile(screenshot);
        multipart.addBodyPart(attachmentBodyPart);

        // Set the Multipart object as the content of the email
        email.setContent(multipart);

        return email;
    }
    public MimeBodyPart buildAttachmentFromMultipartFile(MultipartFile attachment) throws MessagingException, IOException {
        if (!attachment.isEmpty()) {
            MimeBodyPart attachmentBodyPart = new MimeBodyPart();
            DataSource source = new ByteArrayDataSource(attachment.getBytes(), attachment.getContentType());
            attachmentBodyPart.setDataHandler(new DataHandler(source));
            attachmentBodyPart.setFileName(attachment.getOriginalFilename());
            return attachmentBodyPart;
        } else return null;
    }
    public Message createMessageWithEmail(MimeMessage emailContent)
            throws MessagingException, IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        emailContent.writeTo(buffer);
        byte[] bytes = buffer.toByteArray();
        String encodedEmail = Base64.encodeBase64URLSafeString(bytes);
        Message message = new Message();
        message.setRaw(encodedEmail);
        return message;
    }
}
