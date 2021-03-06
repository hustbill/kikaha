package kikaha.urouting;

import java.util.List;
import kikaha.core.cdi.helpers.TinyList;
import kikaha.urouting.EventDispatcher.*;
import lombok.RequiredArgsConstructor;

public class EventDispatcher<T> {

	final List<Entry<T>> listeners = new TinyList<>();

	public EventDispatcher<T> when(
		final Matcher<T> matcher, final Listener<T> listener ) {
		listeners.add( new Entry<>( matcher, listener ) );
		return this;
	}

	public boolean apply( final T target ) {
		for ( final Entry<T> entry : listeners )
			if ( entry.matcher.matches( target ) ) {
				entry.listener.onNewState( target );
				return true;
			}
		return false;
	}

	public interface Matcher<T> {
		boolean matches( final T object );
	}

	public interface Listener<T> {
		void onNewState( final T object );
	}
}

@RequiredArgsConstructor
class Entry<T> {
	final Matcher<T> matcher;
	final Listener<T> listener;
}