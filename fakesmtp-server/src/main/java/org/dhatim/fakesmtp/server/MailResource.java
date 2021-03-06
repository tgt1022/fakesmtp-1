package org.dhatim.fakesmtp.server;

import com.dumbster.smtp.MailMessage;
import com.dumbster.smtp.SmtpServer;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import org.dhatim.fakesmtp.client.Mail;

@Path("/mails")
@Produces(MediaType.APPLICATION_JSON)
public class MailResource {

    private final DumbsterManager manager;

    public MailResource(DumbsterManager manager) {
        this.manager = manager;
    }

    @GET
    public List<Mail> list() {
        return Arrays.stream(getServer().getMessages()).map(MailResource::fromMailMessage).collect(Collectors.toList());
    }

    @GET
    @Path("count")
    public int count() {
        return getServer().getEmailCount();
    }

    @POST
    @Path("clear")
    public int clear() {
        int count = getServer().getEmailCount();
        getServer().clearMessages();
        return count;
    }

    private SmtpServer getServer() {
        return manager.getServer();
    }

    private static Mail fromMailMessage(MailMessage mail) {
        Iterable<String> it = () -> mail.getHeaderNames();
        HashMap<String, List<String>> hs = new HashMap<>();
        for (String name : it) {
            hs.put(name, Arrays.asList(mail.getHeaderValues(name)));
        }
        return new Mail(hs, mail.getBody(), decodeOrNull(mail));
    }

    private static String decodeOrNull(MailMessage mail) {
        String result;
        String contentTransferEncoding = getContentTransferEncoding(mail);
        if (contentTransferEncoding == null) {
            result = mail.getBody();
        } else {
            try {
                ByteArrayInputStream inputStream = new ByteArrayInputStream(mail.getBody().getBytes(StandardCharsets.US_ASCII));
                result = Decoding.decode(contentTransferEncoding, inputStream);
            } catch (IOException e) {
                result = null;
            }
        }
        return result;
    }

    private static String getContentTransferEncoding(MailMessage mail) {
        Iterator<String> it = mail.getHeaderNames();
        while (it.hasNext()) {
            String header = it.next();
            if ("content-transfer-encoding".equalsIgnoreCase(header)) {
                return mail.getFirstHeaderValue(header);
            }
        }
        return null;
    }

}
