package iwein.samples.si.methodresolution;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.integration.core.PollableChannel;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

/**
 *
 */
@ContextConfiguration(locations = "/service-activator-around-overridden-method.xml")
@RunWith(SpringJUnit4ClassRunner.class)
public class TestOverridingMethods {

  @Autowired
  @Qualifier("output")
  PollableChannel output;

  @Test
  public void shouldWork() throws InterruptedException {
    assertThat(output.receive(100), is(notNullValue()));
    Thread.sleep(1000);
  }

}
