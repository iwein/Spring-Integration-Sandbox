package iwein.samples.test.rule;

import org.junit.rules.TestWatchman;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.Statement;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * @author Iwein Fuld
 */
public class TemporarySpringContext extends TestWatchman {
  /**
   * Cache of Spring application contexts. This needs to be static, as tests
   * may be destroyed and recreated between running individual test methods,
   * for example with JUnit.
   */
  static final ContextCache contextCache = new ContextCache();

  private ConfigurableApplicationContext context;

  public TemporarySpringContext(String... contextLocations) {
    try {
      context = contextCache.contextForLocations(contextLocations);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public Statement apply(Statement base, FrameworkMethod method, Object target) {
    context.getAutowireCapableBeanFactory().autowireBean(target);
    return super.apply(base, method, target);
  }

  public ConfigurableApplicationContext getContext() {
    return context;
  }
}
