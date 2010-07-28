package iwein.samples.spring.task;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * @author Iwein Fuld
 */
@ContextConfiguration(locations = "/task-executor.xml")
@RunWith(SpringJUnit4ClassRunner.class)
public class ThreadPoolTaskExecutorWiringTest {
    @Autowired
    org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor executor;


    @Test
    public void shouldWireTerminator() {
        //when wired as an object, this line prints the class (which is org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor)
        System.out.println("executor type"+ executor.getClass());
    }
}
