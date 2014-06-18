package urouting.api;

import java.lang.annotation.*;

@Target( { ElementType.PARAMETER } )
@Retention( RetentionPolicy.RUNTIME )
public @interface QueryParam {
	String value();
}
