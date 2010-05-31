package iwein.samples.barriers;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.integration.aggregator.CorrelationStrategy;
import org.springframework.integration.aggregator.ReleaseStrategy;
import org.springframework.integration.channel.PollableChannel;
import org.springframework.integration.core.Message;
import org.springframework.integration.core.MessageChannel;
import org.springframework.integration.message.MessageBuilder;
import org.springframework.integration.store.MessageGroup;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.concurrent.*;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

/**
 * @author Iwein Fuld
 */
@ContextConfiguration("/context.xml")
@RunWith(SpringJUnit4ClassRunner.class)
public class CorrelatingMessageBarrierIntegrationTest {
  @Autowired
  MessageChannel in;

  @Autowired
  PollableChannel out;

  @Autowired
  TrackingReleaseStrategy releaseStrategy;

  @Before
  public void releaseLocks(){
    releaseStrategy.releaseAll();
  }


  @Test
  public void shouldPassSingleMessage() {
    in.send(testMessage("foo"));
    assertThat((String) (out.receive()).getPayload(), is("foo"));
  }

  @Test
  public void shouldNotReleaseMessagesForSameKey() {
    in.send(testMessage("foo"));
    in.send(testMessage("foo"));
    assertThat((String) (out.receive()).getPayload(), is("foo"));
    System.out.println("Received first message");
    Message<?> message = out.receive(10l);
    System.out.println("Received second (null) message");
    assertThat(message, is(nullValue()));
    releaseStrategy.release("foo");
    System.out.println("Looking for real second message");
    assertThat((String) (out.receive()).getPayload(), is("foo"));
  }

  @Test
  public void shouldNeverDropMessage() {
    final CountDownLatch start = new CountDownLatch(1);
    for (int i =0; i<20; i++){
      sendAsynchronously(in, testMessage("foo"), start);
    }
    start.countDown();
    
    assertThat((String) (out.receive()).getPayload(), is("foo"));
    for (int i =0; i<19; i++){
      releaseStrategy.release("foo");
      assertThat((String) (out.receive()).getPayload(), is("foo"));
    }
  }

  private void sendAsynchronously(final MessageChannel in, final Message<?> message, final CountDownLatch start) {
    Executors.newSingleThreadExecutor().execute(new Runnable(){
      @Override
      public void run() {
        try {
          start.await();
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
        }
        in.send(message);
      }
    });

  }


  private Message<?> testMessage(String payload) {
    return MessageBuilder.withPayload(payload).build();
  }

  public static class PayloadCorrelator implements CorrelationStrategy {

    @Override
    public Object getCorrelationKey(Message<?> message) {
      return message.getPayload();
    }
  }

  public static class TrackingReleaseStrategy implements ReleaseStrategy {
    private final ConcurrentMap<Object, Semaphore> keyLocks = new ConcurrentHashMap<Object, Semaphore>();

    @Override
    public boolean canRelease(MessageGroup messageGroup) {
      //System.out.println("Trying to release group: " + messageGroup + "\n to thread: " + Thread.currentThread());
      Object correlationKey = messageGroup.getCorrelationKey();
      Semaphore lock = lockForKey(correlationKey);
      System.out.println(Thread.currentThread()+" got lock: "+lock);
      //TODO allow timeout
      return lock.tryAcquire();
    }

    private Semaphore lockForKey(Object correlationKey) {
      Semaphore semaphore = keyLocks.get(correlationKey);
      if (semaphore == null) {
        keyLocks.putIfAbsent(correlationKey, new Semaphore(1));
        semaphore = keyLocks.get(correlationKey);
      }
      return semaphore;
    }

    public void release(String correlationKey) {
      Semaphore lock = keyLocks.get(correlationKey);
      if (lock != null) {
        lock.release();
      }
    }

    public void releaseAll() {
      for (Semaphore semaphore : keyLocks.values()) {
        semaphore.release();
      }
    }
  }
}
