package kikaha.mojo.generator;

import java.net.URI;

import javax.tools.SimpleJavaFileObject;

public class StringJavaSource extends SimpleJavaFileObject {

	private final String code;

	public StringJavaSource( String name, String code ) {
		super( URI.create( "string:///" + name.replace( '.', '/' ) + Kind.SOURCE.extension ), Kind.SOURCE );
		this.code = code;
	}

	@Override
	public CharSequence getCharContent( boolean ignoreEncodingErrors ) {
		return getCode();
	}

	public String getCode() {
		return code;
	}
}