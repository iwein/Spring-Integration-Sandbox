package iwein.samples.spring.integration;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.integration.channel.PollableChannel;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

/**
 *
 */
@ContextConfiguration("classpath:queueStrainer.xml")
@RunWith(SpringJUnit4ClassRunner.class)
public class QueueStrainerTest {

  @Autowired
  Operator operator;

  @Autowired
  @Qualifier("channel1")
  PollableChannel channel1;

  @Before
  public void setup() {
  }

  @Test(timeout = 10000)
  public void shouldUseQueue() {
    //eats the message for each one served
    for (int i = 0; i < 5; i++) {
      operator.connect(1);
      assertThat(channel1.receive(), is(notNullValue()));
    }
  }

  @Test
  //@Ignore
  public void shouldOverflowQueue() {
    //blocks indefinitely on the last message
    for (int i = 0; i < 6; i++) {
      operator.connect(1);
    }
  }

  

  @Test(timeout = 10000)
  public void shouldBlockQueueUntilCapacity() {
    receiveAsynchrounously(channel1, 500l);
    //blocks on the last message until released
    for (int i = 0; i < 6; i++) {
      System.out.println("Sending message");
      operator.connect(1);
    }
  }

  private void receiveAsynchrounously(final PollableChannel channel1, final long delay) {
    new Thread(new Runnable() {
      public void run() {
        try {
          System.out.println("Running to pick up message");
          Thread.sleep(delay);
          assertNotNull(channel1.receive(100));
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
        }
      }
    }).start();
  }
}
