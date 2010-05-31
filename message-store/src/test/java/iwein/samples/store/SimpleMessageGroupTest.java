package iwein.samples.store;

import org.junit.Test;
import org.springframework.integration.message.MessageBuilder;
import org.springframework.integration.store.MessageGroup;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

/**
 * @author Iwein Fuld
 */
public class SimpleMessageGroupTest {

  @Test (timeout = 5000)
  public void shouldNotThrowConcurrentModificationException() {
    final MessageGroup group = new SimpleMessageGroup("foo");
    group.add(MessageBuilder.withPayload("foo").build());
    final CountDownLatch acquired = new CountDownLatch(100);
    final CountDownLatch start = new CountDownLatch(1);
    for (int i = 0; i < 100; i++) {
      Executors.newSingleThreadExecutor().execute(new Runnable() {
        @Override
        public void run() {
          try {
            start.await();
          } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
          }
          group.add(MessageBuilder.withPayload("foo").build());          
          MessageGroup anotherGroup = new SimpleMessageGroup(group);
          assertThat((String) anotherGroup.getCorrelationKey(), is("foo"));
          System.out.print(" |");
          acquired.countDown();
        }
      });
    }
    start.countDown();
    try {
      acquired.await();
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }

}
