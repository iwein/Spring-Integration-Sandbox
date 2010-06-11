package iwein.samples.barriers;

import org.springframework.integration.aggregator.ReleaseStrategy;


/**
 * @author Iwein Fuld
 */
public interface LockingReleaseStrategy extends ReleaseStrategy {
  /**
   * Clean up any locks for the given key.
   */
  void unlock(Object key);
}
