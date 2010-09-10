package iwein.samples;

import org.springframework.integration.annotation.Header;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.util.Assert;

/**
 * @author Iwein Fuld
 */
public class Porter {

	@ServiceActivator
	public Visitor checkTickets(Visitor visitor, @Header Dossier dossier) {
		System.out.println("Invoked with dossier: " + dossier + " and visitor" + visitor);
		Assert.notNull(dossier);
		return visitor;
	}
}
