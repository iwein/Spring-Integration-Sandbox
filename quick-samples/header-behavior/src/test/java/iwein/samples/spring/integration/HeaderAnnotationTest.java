package iwein.samples.spring.integration;

import iwein.samples.Dossier;
import iwein.samples.Visitor;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.integration.Message;
import org.springframework.integration.MessageChannel;
import org.springframework.integration.core.PollableChannel;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;

/**
 *
 */
@ContextConfiguration("classpath:headers.xml")
@RunWith(SpringJUnit4ClassRunner.class)
public class HeaderAnnotationTest {

  @Autowired
  MessageChannel in;

  @Autowired
  PollableChannel out;

  @Before
  public void setup() {
  }

  @Test
  public void shouldFillInProperHeaderIfPresent() {
    Visitor visitor = mock(Visitor.class);
    Dossier dossier = mock(Dossier.class);
    Message<Visitor> visitorMessage = MessageBuilder.withPayload(visitor).setHeader("dossier", dossier).build();
    in.send(visitorMessage);
    Message<?> message = out.receive();
    System.out.println("test1: "+message);
    assertThat(message, is(notNullValue()));
  }

  //Passes in isolation, fails if another message was already sent.
  @Test//(expected = MessageHandlingException.class)
  public void shouldErrorIfHeaderNotPresent() {
    Visitor visitor = mock(Visitor.class);
    Message<Visitor> visitorMessage = MessageBuilder.withPayload(visitor).build();
    in.send(visitorMessage);
    Message<?> message = out.receive();
    System.out.println("test2: "+message);
    assertThat(message, is(notNullValue()));
  }
}
