package kikaha.core;

import io.undertow.Handlers;
import io.undertow.Undertow;
import io.undertow.Undertow.Builder;
import io.undertow.server.HttpHandler;
import io.undertow.server.handlers.resource.FileResourceManager;
import io.undertow.server.handlers.resource.ResourceHandler;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.net.ssl.SSLContext;

import kikaha.core.api.DeploymentContext;
import kikaha.core.api.DeploymentHook;
import kikaha.core.api.RequestHook;
import kikaha.core.api.UndertowStandaloneException;
import kikaha.core.api.conf.Configuration;
import kikaha.core.impl.DefaultDeploymentContext;
import kikaha.core.impl.DefaultHttpRequestHandler;
import kikaha.core.impl.UndertowRoutedResourcesHook;
import kikaha.core.ssl.SSLContextFactory;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;
import lombok.extern.java.Log;
import trip.spi.ServiceProvider;
import trip.spi.ServiceProviderException;

import com.typesafe.config.Config;

@Log
@Getter
@Accessors(fluent = true)
@RequiredArgsConstructor
public class UndertowServer {

	private final ServiceProvider provider;
	private final Configuration configuration;
	private DeploymentContext deploymentContext;
	private Undertow server;

	public UndertowServer( final Configuration configuration ) {
		this.provider = newServiceProvider();
		this.configuration = configuration;
	}

	/**
	 * Start the Undertow Standalone Server.
	 *
	 * @throws UndertowStandaloneException
	 */
	public void start() throws UndertowStandaloneException {
		final long start = System.currentTimeMillis();
		bootstrap();
		this.server = createServer();
		this.server.start();
		final long elapsed = System.currentTimeMillis() - start;
		log.info("Server started in " + elapsed + "ms.");
		log.info("Server is listening at " + host() + ":"
				+ configuration().port());
		Runtime.getRuntime().addShutdownHook( new UndertowShutdownHook(this) );
	}

	/**
	 * Run all life cycle initialization routines of Undertow Standalone.
	 *
	 * @throws UndertowStandaloneException
	 */
	protected void bootstrap() throws UndertowStandaloneException {
		try {
			provideSomeDependenciesForFurtherInjections();
			final DefaultDeploymentContext deploymentContext = createDeploymentContext();
			runDeploymentHooks(deploymentContext);
			deployWebResourceFolder(deploymentContext);
			finishDeployment(deploymentContext);
			this.deploymentContext = deploymentContext;
		} catch (final ServiceProviderException cause) {
			throw new UndertowStandaloneException(cause);
		}
	}

	protected void provideSomeDependenciesForFurtherInjections() {
		provider.providerFor( Configuration.class, configuration );
		provider.providerFor( Config.class, configuration().config() );
	}

	protected DefaultDeploymentContext createDeploymentContext()
			throws ServiceProviderException {
		final Iterable<DeploymentHook> deploymentHooks = provider
			.loadAll( DeploymentHook.class );
		final Iterable<RequestHook> requestHooks = provider
			.loadAll( RequestHook.class );
		final List<RequestHook> mutableListOfHooks = mutableList(requestHooks);
		return new DefaultDeploymentContext(deploymentHooks, mutableListOfHooks);
	}

	protected void runDeploymentHooks(final DeploymentContext deploymentContext) {
		for (final DeploymentHook hook : deploymentContext.deploymentHooks()) {
			log.fine("Dispatching deployment hook: "
					+ hook.getClass().getCanonicalName());
			hook.onDeploy(deploymentContext);
		}
	}

	protected void deployWebResourceFolder(final DeploymentContext deploymentContext) {
		deploymentContext.fallbackHandler(createResourceManager());
	}

	protected ResourceHandler createResourceManager() {
		final File location = retrieveWebAppFolder();
		final FileResourceManager resourceManager = new FileResourceManager( location, 100 );
		log.info("Exposing resource files at " + location);
		return Handlers.resource(resourceManager)
				.setResourceManager(resourceManager)
				.setDirectoryListingEnabled( false )
				.setWelcomeFiles( configuration.welcomeFile() );
	}

	protected File retrieveWebAppFolder() {
		final File location = new File(configuration().resourcesPath());
		if (!location.exists())
			location.mkdir();
		return location;
	}

	protected void finishDeployment(final DefaultDeploymentContext deploymentContext) {
		final HttpHandler rootHandler = deploymentContext.rootHandler();
		final UndertowRoutedResourcesHook undertowRoutedResources = UndertowRoutedResourcesHook
				.wrap(rootHandler);
		deploymentContext.register(undertowRoutedResources);
	}

	protected Undertow createServer() {
		final Builder builder = Undertow.builder();
		final SSLContext sslContext = readConfiguredSSLContext();
		if ( sslContext == null )
			builder.addHttpListener( configuration().port(), host() );
		else
			builder.addHttpsListener( configuration().port(), host(), sslContext );
		return builder.setHandler(
			new DefaultHttpRequestHandler( this.deploymentContext() ) ).build();
	}

	SSLContext readConfiguredSSLContext() {
		try {
			final SSLContextFactory factory = provider.load( SSLContextFactory.class );
			return factory.createSSLContext();
		} catch ( IOException | ServiceProviderException cause ) {
			throw new RuntimeException( cause );
		}
	}

	private String host() {
		String host = configuration().host();
		if (host == "*")
			host = "0.0.0.0";
		return host;
	}

	public void stop() {
		this.server().stop();
		log.info("Server stopped!");
	}

	private ServiceProvider newServiceProvider() {
		final ServiceProvider serviceProvider = new ServiceProvider();
		return serviceProvider;
	}

	static <T> List<T> mutableList(final Iterable<T> immutable) {
		final ArrayList<T> mutableList = new ArrayList<T>();
		for (final T item : immutable)
			mutableList.add(item);
		return mutableList;
	}

	@RequiredArgsConstructor
	static class UndertowShutdownHook extends Thread {
		final UndertowServer server;

		@Override
		public void run() {
			server.stop();
		}
	}
}
