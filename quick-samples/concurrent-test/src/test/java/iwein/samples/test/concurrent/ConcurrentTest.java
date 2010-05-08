package iwein.samples.test.concurrent;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.channel.PollableChannel;
import org.springframework.integration.core.Message;
import org.springframework.integration.core.MessageChannel;
import org.springframework.integration.message.MessageBuilder;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.concurrent.atomic.AtomicInteger;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

@ContextConfiguration(locations = {"classpath:context.xml"})
@RunWith(SpringJUnit4ClassRunner.class)
/**
 *  This is a test to reproduce http://jira.springframework.org/browse/INT-915
 */
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

  @Autowired Transformer transformer;

  private static final int NUMBER_OF_MESSAGES = 200;

  @Test(timeout = 200000)
  public void shouldGoThroughPipeline() throws Throwable {
    for (int i = 0; i < NUMBER_OF_MESSAGES; i++) {
      in.send(MessageBuilder.withPayload("Payload"+i).build());
    }

    int outputCount = 0;
    int errCount = 0;
    while (outputCount+errCount < NUMBER_OF_MESSAGES) {
      Message<?> received = out.receive(10);
      if (received != null) {
        outputCount++;
        System.out.println("received: "+received.getPayload());
      } else {
        Message<?> message = err.receive(10);
        if (message != null) {
          errCount++;
        }
      }
    }
    assertThat(outputCount+errCount, is(NUMBER_OF_MESSAGES));
    assertThat(transformer.timesInvoked.get()+service.timesInvoked.get()-errCount, is(NUMBER_OF_MESSAGES));
  }


  public static class Service {
    private final AtomicInteger timesInvoked = new AtomicInteger();

    @ServiceActivator
    public String serve(String input) {
      System.out.println("serving: "+input);
      assertNotNull(input);
      timesInvoked.incrementAndGet();
      if (Math.random() < 0.5) {
        throw new IllegalArgumentException("zoinks!");
      }
      return input;
    }
  }

  public static class Transformer {
    private final AtomicInteger timesInvoked = new AtomicInteger();

    @org.springframework.integration.annotation.Transformer
    public String transform(String input) {
      System.out.println("transforming: "+input);
      assertNotNull(input);
      timesInvoked.incrementAndGet();
      if (Math.random() < 0.5) {
        throw new IllegalArgumentException("zoinks!");
      }
      return input;
    }
  }
}
