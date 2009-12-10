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

import java.io.File;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
public class ConcurrentTest {

  @Autowired
  @Qualifier("in")
  MessageChannel in;

  @Autowired
  @Qualifier("out")
  PollableChannel out;

  @Autowired
  Service service;

  @Test(timeout = 200000)
  public void shouldGoThroughPipeline() throws Exception {

    //when
    for (int i = 0; i < 1000; i++) {
      File file = File.createTempFile("concurrent-test", "test");
      file.deleteOnExit();
      in.send(MessageBuilder.withPayload(file).build());
    }
    //verify

    for (int i = 0; i < 1000; i++) {
      Message<?> message = out.receive();
      assertThat(message.getPayload(), is(notNullValue()));
    }
  }


  public static class Service {
    @ServiceActivator
    public File serve(File input) {
      return input;
    }
  }

  public static class Transformer {
    @org.springframework.integration.annotation.Transformer
    public File transform(File input) {
      return input;
    }
  }
}
