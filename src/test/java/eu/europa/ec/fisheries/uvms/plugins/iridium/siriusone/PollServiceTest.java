package eu.europa.ec.fisheries.uvms.plugins.iridium.siriusone;

import java.util.UUID;
import javax.mail.MessagingException;
import org.junit.Test;

public class PollServiceTest {

    @Test
    public void test() throws MessagingException {
        PollService pollService = new PollService();
//        byte[] request = pollService.createPositionReportRequest();
        byte[] request = pollService.createSetIntervalRequest(3600);
        pollService.sendRequest("1234", request, UUID.randomUUID().toString(), "server", "587", "mail@mail", "mail@mail", "uvms", "XXXX");
    }
    
}
