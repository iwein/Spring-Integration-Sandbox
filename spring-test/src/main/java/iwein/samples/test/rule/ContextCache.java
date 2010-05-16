package iwein.samples.test.rule;

import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.test.context.support.GenericXmlContextLoader;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Replacement for org.springframework.test.context.ContextCache. This class is not visible to users and it's
 * convenient to have the cache create the context instead of returning null;
 *
 * @author Iwein Fuld
 */
class ContextCache {
  private final ConcurrentMap<String[], ConfigurableApplicationContext> locationsToContexts =
      new ConcurrentHashMap<String[], ConfigurableApplicationContext>();

  /**
   * Returns a cached context for the given locations (never null). If the context was not known before
   * a new {@link org.springframework.context.support.ClassPathXmlApplicationContext}  will be created.
   */
  public ConfigurableApplicationContext contextForLocations(String[] locations) throws Exception {
    ConfigurableApplicationContext context = locationsToContexts.get(locations);
    if (context == null) {
      ConfigurableApplicationContext newContext = new GenericXmlContextLoader().loadContext(locations);
      ConfigurableApplicationContext oldContext = locationsToContexts.putIfAbsent(locations, newContext);
      context = oldContext == null ? newContext : oldContext;
    }
    return context;
  }

  /**
   * Removes the context for the given key from the cache
   * (forcing recreation upon subsequent {@link #contextForLocations(String[]) requests} for this context)
   */
  public void markDirty(String... key) {
    locationsToContexts.remove(key);
  }

}
