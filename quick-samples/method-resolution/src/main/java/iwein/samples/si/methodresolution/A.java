package iwein.samples.si.methodresolution;

import org.springframework.integration.core.Message;

/**
*
*/
public class A {
  public Message<String> myMethod(final Message<String> msg) {
    System.out.println("A is processing "+msg);
    return msg;
  }
}
