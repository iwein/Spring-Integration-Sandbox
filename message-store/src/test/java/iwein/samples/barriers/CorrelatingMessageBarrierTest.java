package iwein.samples.barriers;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;
import org.springframework.beans.DirectFieldAccessor;
import org.springframework.integration.aggregator.CorrelationStrategy;
import org.springframework.integration.core.Message;
import org.springframework.integration.message.MessageBuilder;
import org.springframework.integration.message.MessageHandler;
import org.springframework.integration.store.MessageGroup;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class CorrelatingMessageBarrierTest {

    private CorrelatingMessageBarrier barrier;
    @Mock
    private CorrelationStrategy correlationStrategy;
    @Mock
    private LockingReleaseStrategy releaseStrategy;
    @Mock
    private Semaphore messagesUpperBound;

    @Before
    public void initializeBarrier() {
        barrier = new CorrelatingMessageBarrier();
        barrier.setCorrelationStrategy(correlationStrategy);
        barrier.setReleaseStrategy(releaseStrategy);

        new DirectFieldAccessor(barrier).setPropertyValue("messagesUpperBound", messagesUpperBound);
        when(correlationStrategy.getCorrelationKey(isA(Message.class))).thenReturn("foo");
        when(releaseStrategy.canRelease(isA(MessageGroup.class))).thenReturn(true);
    }

    @Test
    public void shouldRemoveKeyWithoutLockingOnEmptyQueue() throws InterruptedException {
        Message<?> message = testMessage();
        Message<?> message2 = testMessage();
        barrier.handleMessage(message);
        verify(correlationStrategy).getCorrelationKey(message);
        assertThat(barrier.receive(), is(notNullValue()));
        barrier.handleMessage(message2);
        assertThat(barrier.receive(), is(notNullValue()));
        assertThat(barrier.receive(), is(nullValue()));
    }

    @Test
    public void shouldRespectUpperBound() throws InterruptedException {
        barrier.setMessagesUpperBound(1);
        final CountDownLatch start = new CountDownLatch(1);
        final CountDownLatch sent = new CountDownLatch(1);
        final AtomicInteger sentCount = new AtomicInteger(0);
        handleAsynchronously(barrier, testMessage(), start, sent, sentCount);
        handleAsynchronously(barrier, testMessage(), start, sent, sentCount);
        handleAsynchronously(barrier, testMessage(), start, sent, sentCount);
        start.countDown();

        assertThat(sent.await(1, TimeUnit.SECONDS), is(true));
        //allow concurrent threads some time to try and send
        Thread.sleep(10);
        //but still there should be exactly one message in the barrier at a time
        assertThat("Wrong number of messages sent ", sentCount.get(), is(1));
        boolean allSent = false;
        int i = 1;
        while (i <= 3) {
            assertTrue(i-sentCount.get() < (2));
            Message message = barrier.receive();
            if (message != null) {
                i++;
            }
        }
        assertThat("Wrong number of messages sent ", sentCount.get(), is(3));
        Thread.sleep(1);
        assertThat(barrier.receive(), is(nullValue()));
    }

    @Test
    public void shouldShouldHaveCorrectUpperBoundAfterDrainingQueue() {
        barrier.setMessagesUpperBound(4);
        barrier.handleMessage(testMessage());
        while (barrier.receive()==null){}
        barrier.receive();
        DirectFieldAccessor accessor = new DirectFieldAccessor(barrier);
        Semaphore messagesUpperBound = (Semaphore) accessor.getPropertyValue("messagesUpperBound");
        assertThat(messagesUpperBound.drainPermits(), is(4));
    }


    private void handleAsynchronously(final MessageHandler handler, final Message<?> message, final CountDownLatch start, final CountDownLatch sent, final AtomicInteger sentCount) {
        Executors.newSingleThreadExecutor().execute(new Runnable() {
            @Override
            public void run() {
                try {
                    start.await();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                handler.handleMessage(message);
                sentCount.incrementAndGet();
                sent.countDown();
            }
        });
    }

    @Test(timeout = 2000)
    public void shouldAvoidEmptyMessagesWhenWaitingForCapacity() throws Exception {

        final CountDownLatch allowAcquire = new CountDownLatch(1);
        doAnswer(new Answer<Object>() {
            public Object answer(InvocationOnMock invocation) throws Throwable {
                allowAcquire.await();
                return null;
            }
        }).
                when(messagesUpperBound).acquire();

        final AtomicBoolean shouldFail = new AtomicBoolean(false);
        final CountDownLatch messageSentAsynchronously = new CountDownLatch(1);
        Executors.newSingleThreadExecutor().execute(new Runnable() {
            public void run() {
                try {
                    barrier.handleMessage(testMessage());

                } catch (Exception e) {
                    shouldFail.set(true);
                } finally {
                    messageSentAsynchronously.countDown();
                }
            }
        });
        try {
            Thread.sleep(50);
            barrier.receive();
            allowAcquire.countDown();
            messageSentAsynchronously.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        assertThat(shouldFail.get(), is(false));
    }

    /*
     * when this test fails it means that the sending thread blocked even though there is an aggressive receiver pool
     * to debug:
     *   - remove the timeout
     *   - turn on trace logging
     *   - run test and take a thread dump
     */
    @Test(timeout = 20000)
    public void shouldNotStarveWhenConcurrentlySendingAndReceiving() {
        barrier.setMessagesUpperBound(5);
        final CountDownLatch startReceiving = new CountDownLatch(1);
        ExecutorService executorService = Executors.newFixedThreadPool(10);
        final AtomicInteger receivedCount = new AtomicInteger();
        for (int i = 0; i < 30; i++) {
            executorService.execute(new Runnable() {
                public void run() {
                    try {
                        startReceiving.await();
                        while (true) {
                            if (barrier.receive() != null) {
                                Thread.sleep(3);
                            }
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }
            });
        }
        startReceiving.countDown();
        for (int i = 0; i < 1000; i++) {
            barrier.handleMessage(testMessage());
        }
    }

    private Message<?> testMessage() {
        return MessageBuilder.withPayload("foo").build();
    }

}
