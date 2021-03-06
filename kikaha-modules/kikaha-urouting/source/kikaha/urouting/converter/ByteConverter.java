package kikaha.urouting.converter;

import kikaha.urouting.api.AbstractConverter;
import kikaha.urouting.api.ConversionException;

import javax.enterprise.inject.Typed;
import javax.inject.Singleton;

@Singleton
@Typed(  AbstractConverter.class )
public class ByteConverter extends AbstractConverter<Byte> {

	@Override
	public Byte convert( String value ) throws ConversionException {
		return Byte.valueOf( value );
	}

}
