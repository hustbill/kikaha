package kikaha.core.modules.security;

import java.util.*;
import io.undertow.security.api.NotificationReceiver;
import io.undertow.security.idm.*;
import io.undertow.security.idm.IdentityManager;
import io.undertow.server.HttpServerExchange;
import lombok.*;

/**
 * A per-request object that groups security-related information.
 */
@Getter
@RequiredArgsConstructor
public class DefaultSecurityContext implements SecurityContext {

	private static final String MSG_IMMUTABLE = "You can't change this immutable SecurityContext. See SecurityContextFactory for more details.";
	private static final String MSG_NO_MANUAL_LOGIN = "You can't perform a manual login.";
	private static final String MSG_NOT_SUPPORTED_BY_DEFAULT = "This operation is not supported by default.";

	private final boolean authenticationRequired = true;

	private AuthenticationMechanism currentAuthMechanism = null;
	private Session currentSession = null;
	private boolean authenticated = true;

	@NonNull private final AuthenticationRule rule;
	@NonNull private final HttpServerExchange exchange;
	@NonNull private final SessionStore store;

	@Override
	public boolean authenticate() {
		currentSession = store.createOrRetrieveSession(exchange);
		final Account account = performAuthentication();
		if ( account == null ){
			authenticated = false;
			sendAuthenticationChallenge();
		}
		currentSession.setAuthenticatedAccount( account );
		updateCurrentSession();
		return authenticated;
	}

	private Account performAuthentication() {
		final Iterator<AuthenticationMechanism> iterator = rule.mechanisms().iterator();
		Account account = currentSession.getAuthenticatedAccount();
		while ( account == null && iterator.hasNext() ) {
			currentAuthMechanism = iterator.next();
			account = currentAuthMechanism.authenticate( exchange, rule.identityManagers(), currentSession );
		}
		return account;
	}

	private void sendAuthenticationChallenge() {
		if ( !currentAuthMechanism.sendAuthenticationChallenge( exchange, currentSession ) )
			throw new IllegalStateException( "Cannot send authentication challenge" );
	}

	@Override
	public void logout() {
		if ( currentSession != null )
			store.invalidateSession(currentSession);
	}

	@Override
	public void updateCurrentSession() {
		if ( currentSession != null && currentSession.hasChanged() ) {
			try { store.flush( currentSession ); }
			finally { currentSession.flush(); }
		}
	}

	public void setCurrentSession( Session session ){
		this.currentSession = session;
	}

	@Override
	public Account getAuthenticatedAccount() {
		return currentSession != null ? currentSession.getAuthenticatedAccount() : null;
	}

	@Override
	public void setAuthenticatedAccount(Account account){
		if ( currentSession != null )
			currentSession.setAuthenticatedAccount(account);
	}

	@Override
	public void setAuthenticationRequired() {}

	@Override
	public void authenticationComplete( Account account, String mechanismName, boolean cachingRequired ) {
		throw new UnsupportedOperationException(MSG_NOT_SUPPORTED_BY_DEFAULT);
	}

	@Override
	public void authenticationFailed( String message, String mechanismName ) {
		throw new UnsupportedOperationException(MSG_NOT_SUPPORTED_BY_DEFAULT);
	}

	@Override
	public boolean login(String username, String password) {
		throw new UnsupportedOperationException(MSG_NO_MANUAL_LOGIN);
	}

	@Override
	public void registerNotificationReceiver(NotificationReceiver receiver) {
		throw new UnsupportedOperationException(MSG_IMMUTABLE);
	}

	@Override
	public void removeNotificationReceiver(NotificationReceiver receiver) {
		throw new UnsupportedOperationException(MSG_IMMUTABLE);
	}

	@Override
	public String getMechanismName() {
		throw new UnsupportedOperationException(MSG_NOT_SUPPORTED_BY_DEFAULT);
	}

	@Override
	public IdentityManager getIdentityManager() {
		throw new UnsupportedOperationException(MSG_NOT_SUPPORTED_BY_DEFAULT);
	}

	@Override
	public void addAuthenticationMechanism(
			io.undertow.security.api.AuthenticationMechanism mechanism) {
		throw new UnsupportedOperationException(MSG_IMMUTABLE);
	}

	@Override
	public List<io.undertow.security.api.AuthenticationMechanism> getAuthenticationMechanisms() {
		throw new UnsupportedOperationException(MSG_NOT_SUPPORTED_BY_DEFAULT);
	}
}
