package com.financialanalysis.reports;

import com.financialanalysis.data.User;
import com.financialanalysis.store.ReportStore;
import com.financialanalysis.store.UserStore;
import com.google.inject.Inject;
import lombok.extern.log4j.Log4j;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.activation.FileDataSource;
import javax.mail.BodyPart;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import java.io.File;
import java.util.List;
import java.util.Properties;

@Log4j
public class Emailer {
    private final ReportStore reportStore;
    private final UserStore userStore;

    private final static String FROM_EMAIL = "jasonyak1@gmail.com";
    private final static String PASS = "***";

    @Inject
    public Emailer(ReportStore reportStore, UserStore userStore) {
        this.reportStore = reportStore;
        this.userStore = userStore;
    }

    public void emailReports() {
        log.info("Emailing reports");
        Properties properties = System.getProperties();
        String host = "smtp.gmail.com";
        properties.put("mail.smtp.starttls.enable", "true");
        properties.put("mail.smtp.host", host);
        properties.put("mail.smtp.user", FROM_EMAIL);
        properties.put("mail.smtp.password", PASS);
        properties.put("mail.smtp.port", "587");
        properties.put("mail.smtp.auth", "true");

        try {
            Session session = Session.getDefaultInstance(properties);
            MimeMessage message = new MimeMessage(session);
            message.setFrom(new InternetAddress(FROM_EMAIL));
            for(User user : userStore.load()) {
                message.addRecipient(Message.RecipientType.TO, new InternetAddress(user.getEmail()));
            }
            message.setSubject("Daily Stock Update");

            List<File> reports = reportStore.loadFromDate(DateTime.now(DateTimeZone.forID("America/Toronto")));
            if(reports.isEmpty()) {
                message.setText("No patterns found for today");
            } else {
                message.setText("Stocks with patterns");
                Multipart multipart = new MimeMultipart();
                for(File file : reports) addAttachment(multipart, file);
                message.setContent(multipart);
            }

            Transport transport = session.getTransport("smtp");
            transport.connect(host, FROM_EMAIL, PASS);
            transport.sendMessage(message, message.getAllRecipients());
            transport.close();
        } catch (MessagingException me) {
            me.printStackTrace();
        }
    }

    private void addAttachment(Multipart multipart, File file) throws MessagingException {
        DataSource source = new FileDataSource(file);
        BodyPart messageBodyPart = new MimeBodyPart();
        messageBodyPart.setDataHandler(new DataHandler(source));
        messageBodyPart.setFileName(file.getName());
        multipart.addBodyPart(messageBodyPart);
    }
}
