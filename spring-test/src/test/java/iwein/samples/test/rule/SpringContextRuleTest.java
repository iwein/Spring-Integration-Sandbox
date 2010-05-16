package iwein.samples.test.rule;

import org.junit.Rule;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

/**
 * @author Iwein Fuld
 */
public class SpringContextRuleTest {

  @Rule
  public TemporarySpringContext context = new TemporarySpringContext("context.xml");

  @Autowired
  ApplicationContext thisShouldBeWired;

  @Test
  public void shouldLoadContext() {
     assertThat(context.getContext(), is(notNullValue()));
  }

  @Test
  public void shouldAutowireTestFields() {
    assertNotNull(thisShouldBeWired);
  }

}