package kikaha.core.cdi;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

import javax.inject.Inject;

import org.junit.Test;

public class SuperclassInjectionTest {

	@Test
	public void ensureThatInjectedOnSuperclass() throws ServiceProviderException {
		final ServiceProvider provider = new DefaultServiceProvider();
		final MyPrintable printable = new MyPrintable();
		provider.provideOn( printable );
		assertNotNull( printable.printableWord );
		assertThat( printable.toString(), is( "My kikaha.core.cdi.PrintableWorld" ) );
	}
}

class AbstractPrintable implements Printable {

	@Inject
	PrintableWord printableWord;

	@Override
	public String toString() {
		return printableWord.toString();
	}
}

class MyPrintable extends AbstractPrintable {

	@Override
	public String toString() {
		return "My " + super.toString().replaceFirst( "@.*", "" );
	}
}