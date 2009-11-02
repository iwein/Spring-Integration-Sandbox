package iwein.samples.test.concurrent;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.integration.channel.PollableChannel;
import org.springframework.integration.core.Message;
import org.springframework.integration.core.MessageChannel;
import org.springframework.integration.message.MessageBuilder;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.concurrent.CountDownLatch;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.assertThat;
import static org.mockito.BDDMockito.given;

@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
public class ConcurrentTest {

  @Autowired @Qualifier("in") MessageChannel in;

  @Autowired @Qualifier("out") PollableChannel out;

  @Autowired Service service;

  @Test(timeout = 5000)
  public void shouldGoThroughPipeline() throws Exception {
    //given
    final CountDownLatch serviceInvoked = new CountDownLatch(1);
    given(service.serve("test")).willAnswer(latchedAnswer("test", serviceInvoked));

    //when
    in.send(MessageBuilder.withPayload("test").build());
    serviceInvoked.await();

    //verify
    Message<?> message = out.receive();
    assertThat((String) message.getPayload(), is("test"));
  }

  private <T> Answer<T> latchedAnswer(final T returning, final CountDownLatch latch) {
    return new Answer(){
      public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
        latch.countDown();
        return returning;
      }
    };
  }

  //You'd have a class in main to mock, but an interface here will serve the example
  public static interface Service {
    public String serve(String input);
  }
}
