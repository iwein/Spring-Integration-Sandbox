package iwein.samples.test.concurrent;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.channel.PollableChannel;
import org.springframework.integration.core.Message;
import org.springframework.integration.core.MessageChannel;
import org.springframework.integration.message.ErrorMessage;
import org.springframework.integration.message.MessageBuilder;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.io.File;

import static org.junit.Assert.assertNotNull;

@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
@Ignore //Remove this to reproduce http://jira.springframework.org/browse/INT-915
public class ConcurrentTest {

  @Autowired
  @Qualifier("in")
  MessageChannel in;

  @Autowired
  @Qualifier("out")
  PollableChannel out;

  @Autowired
  @Qualifier("errorChannel")
  PollableChannel err;

  @Autowired
  Service service;
  private static final int NUMBER_OF_MESSAGES = 400;

  @Test(timeout = 200000)
  public void shouldGoThroughPipeline() throws Throwable {

    //when
    for (int i = 0; i < NUMBER_OF_MESSAGES; i++) {
      File file = File.createTempFile("concurrent-test", "test");
      file.deleteOnExit();
      in.send(MessageBuilder.withPayload(file).build());
    }
    //verify
    int outputCount = 0;
    while (outputCount < NUMBER_OF_MESSAGES) {
      if (out.receive(10) != null) {
        outputCount += 1;
      } else {
        Message<?> message = err.receive(10);
        if (message != null) {
          Throwable payload = ((ErrorMessage) message).getPayload();
          if (!(payload.getCause() instanceof IllegalArgumentException)) {
            throw payload;
          }
          outputCount++;
        }
      }
    }
  }


  public static class Service {
    @ServiceActivator
    public File serve(File input) {
      assertNotNull(input);
      if (Math.random() < 0.5) {
        throw new IllegalArgumentException("zoinks!");
      }
      return input;
    }
  }

  public static class Transformer {
    @org.springframework.integration.annotation.Transformer
    public File transform(File input) {
      assertNotNull(input);
      if (Math.random() < 0.5) {
        throw new IllegalArgumentException("zoinks!");
      }
      return input;
    }
  }
}
