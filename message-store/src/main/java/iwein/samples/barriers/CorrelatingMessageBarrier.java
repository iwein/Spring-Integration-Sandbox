package iwein.samples.barriers;

import iwein.samples.store.SimpleMessageStore;
import org.springframework.integration.aggregator.CorrelationStrategy;
import org.springframework.integration.aggregator.ReleaseStrategy;
import org.springframework.integration.core.Message;
import org.springframework.integration.message.MessageDeliveryException;
import org.springframework.integration.message.MessageHandler;
import org.springframework.integration.message.MessageHandlingException;
import org.springframework.integration.message.MessageSource;
import org.springframework.integration.store.MessageGroup;
import org.springframework.integration.store.MessageGroupStore;

import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;

/**
 * @author Iwein Fuld
 */
public class CorrelatingMessageBarrier implements MessageHandler, MessageSource {

  private ConcurrentHashMap<Object, Object> correlationLocks = new ConcurrentHashMap<Object, Object>();
  private MessageGroupStore reservoir = new SimpleMessageStore(10000);
  private ReleaseStrategy releaseStrategy;
  private CorrelationStrategy correlationStrategy;

  //TODO use this upper bound
  private Semaphore messagesUpperBound;

  @Override
  public void handleMessage(Message<?> message) throws MessageHandlingException, MessageDeliveryException {
    if (messagesUpperBound != null) {
      try {
        messagesUpperBound.acquire();
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      }
    }
    Object correlationKey = correlationStrategy.getCorrelationKey(message);
    Object lock = getLock(correlationKey);
    synchronized (lock) {
      reservoir.addMessageToGroup(correlationKey, message);
    }
    System.out.println("handled message: " + message);
  }

  @Override
  public Message receive() {
    for (Object key : correlationLocks.keySet()) {
      Object lock = getLock(key);
      synchronized (lock) {
        MessageGroup group = reservoir.getMessageGroup(key);
        //group might be removed by another thread
        if (group != null) {
          if (releaseStrategy.canRelease(group)) {
            Message<?> nextMessage = null;
            //this should be aQueue.poll() maybe?
            Iterator<Message<?>> unmarked = group.getUnmarked().iterator();
            if (unmarked.hasNext()) {
              nextMessage = unmarked.next();
              //TODO remove cast when MessageGroup supports this
              ((SimpleMessageStore) reservoir).markSingleMessage(key, nextMessage);

              messagesUpperBound.release();
            } else {
              remove(key);
            }
            return nextMessage;
          }
        }
      }
    }
    return null;
  }

  private void remove(Object key) {
    correlationLocks.remove(key);
    reservoir.removeMessageGroup(key);
  }

  private Object getLock(Object correlationKey) {
    Object existingLock = correlationLocks.putIfAbsent(correlationKey, correlationKey);
    return existingLock == null ? correlationKey : existingLock;
  }

  public void setReleaseStrategy(ReleaseStrategy releaseStrategy) {
    this.releaseStrategy = releaseStrategy;
  }

  public void setCorrelationStrategy(CorrelationStrategy correlationStrategy) {
    this.correlationStrategy = correlationStrategy;
  }

  public void setMessagesUpperBound(int messagesUpperBound) {
    if (messagesUpperBound > 0) {
      this.messagesUpperBound = new Semaphore(messagesUpperBound);
    }
  }
}
