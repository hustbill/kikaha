package kikaha.urouting;

import javax.inject.*;
import io.undertow.server.HttpServerExchange;

/**
 * A very tiny API to deal with Undertow's low-level API.
 */
@Singleton
public class UndertowHelper {

	@Inject RoutingMethodParameterReader parameterReader;
	@Inject RoutingMethodResponseWriter responseWriter;

	/**
	 * Create a simplified version of a {@link HttpServerExchange}.
	 *
	 * @param exchange
	 * @return the simplified version of {@link HttpServerExchange}.
	 */
	public SimpleExchange simplify(HttpServerExchange exchange) {
		return SimpleExchange.wrap( exchange, parameterReader, responseWriter );
	}
}
