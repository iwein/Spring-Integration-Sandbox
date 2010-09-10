package iwein.samples.si.methodresolution;

import org.springframework.integration.Message;

/**
*
*/
public class B extends A {
  public Message<String> myMethod(final Message<String> msg) {
        Message<String> outMsg = super.myMethod(msg);
        System.out.println("Do something additional.");

        return outMsg;  
  }
}
