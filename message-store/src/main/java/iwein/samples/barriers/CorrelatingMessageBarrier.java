package iwein.samples.barriers;

import org.springframework.integration.aggregator.CorrelationStrategy;
import org.springframework.integration.aggregator.ReleaseStrategy;
import org.springframework.integration.core.Message;
import org.springframework.integration.message.*;
import org.springframework.integration.store.MessageGroup;
import org.springframework.integration.store.MessageGroupStore;
import org.springframework.integration.store.SimpleMessageStore;

import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;

/**
 * @author Iwein Fuld
 */
public class CorrelatingMessageBarrier implements MessageHandler, MessageSource {

  private Queue<Object> correlationsInReservoir = new ArrayBlockingQueue<Object>(10000);
  private MessageGroupStore reservoir = new SimpleMessageStore(10000);
  private ReleaseStrategy releaseStrategy;
  private CorrelationStrategy correlationStrategy;

  @Override
  public void handleMessage(Message<?> message) throws MessageRejectedException, MessageHandlingException, MessageDeliveryException {
    Object correlationKey = correlationStrategy.getCorrelationKey(message);
    reservoir.addMessageToGroup(correlationKey, message);
    correlationsInReservoir.offer(correlationKey);
    System.out.println("handled message: "+ message);
  }

  @Override
  public Message receive() {
    for (Object key : correlationsInReservoir) {
      MessageGroup group = reservoir.getMessageGroup(key);
      if (releaseStrategy.canRelease(group)) {
        Message<?> nextMessage = group.getOne();
        if(nextMessage==null){
          correlationsInReservoir.remove(key);
          reservoir.removeMessageGroup(key);
        }
        return nextMessage;
      }
    }
    return null;
  }

  public void setReleaseStrategy(ReleaseStrategy releaseStrategy) {
    this.releaseStrategy = releaseStrategy;
  }

  public void setCorrelationStrategy(CorrelationStrategy correlationStrategy) {
    this.correlationStrategy = correlationStrategy;
  }
}
