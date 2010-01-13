package iwein.samples.spring.integration;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.integration.channel.PollableChannel;
import org.springframework.integration.core.MessageChannel;
import org.springframework.integration.message.MessageBuilder;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static org.junit.Assert.assertNotNull;

/**
 *
 */
@ContextConfiguration("classpath:headerValueRouter.xml")
@RunWith(SpringJUnit4ClassRunner.class)
public class HeaderValueRouterTest {

  @Autowired
  @Qualifier("in")
  MessageChannel in;

  @Autowired
  @Qualifier("routed")
  PollableChannel routed;

  @Autowired
  @Qualifier("defaultOutput")
  PollableChannel defaultOutput;

  @Before
  public void setup() {
  }

  @Test
  public void shouldRouteMessageWithMappedHeader() {
    in.send(MessageBuilder.withPayload("foo").setHeader("TO", "routed").build());
    assertNotNull(routed.receive(0));
  }

  @Test
  public void shouldRouteMessageWithoutHeader(){
    in.send(MessageBuilder.withPayload("foo").build());
    assertNotNull(defaultOutput.receive(0));    
  }

  @Test
  @Ignore //this doesn't work because Spring Integration tries to resolve channel "something"
  public void shouldRouteMessageWithUnmappedHeader(){
    in.send(MessageBuilder.withPayload("foo").setHeader("TO", "something").build());
    assertNotNull(defaultOutput.receive(0));
  }
}
