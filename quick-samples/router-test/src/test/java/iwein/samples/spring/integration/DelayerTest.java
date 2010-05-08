package iwein.samples.spring.integration;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.integration.channel.PollableChannel;
import org.springframework.integration.core.Message;
import org.springframework.integration.core.MessageChannel;
import org.springframework.integration.message.MessageBuilder;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

/**
 *  @author Iwein Fuld
 */
@ContextConfiguration(locations = "/delayer.xml")
@RunWith(SpringJUnit4ClassRunner.class)
public class DelayerTest {
  @Autowired
  private MessageChannel in;

  @Autowired
  private PollableChannel out;

  @Test
  public void shouldLoadContext() {
    //no errors here proves that the context can be loaded by Spring
  }

  @Test
  public void shouldSendMessageThroughDelayer() {
    Message<?> testMessage = MessageBuilder.withPayload("test").build();
    in.send(testMessage);
    assertThat(out.receive(10), is(nullValue()));
    assertThat(out.receive(), is(notNullValue()));
  }

  public static class RetryFilter {
    public boolean doRetry(Message m){
      return true;
    }
  }
}
