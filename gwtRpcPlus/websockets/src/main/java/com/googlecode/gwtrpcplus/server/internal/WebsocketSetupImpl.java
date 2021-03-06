package com.googlecode.gwtrpcplus.server.internal;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpSession;
import javax.websocket.DeploymentException;
import javax.websocket.HandshakeResponse;
import javax.websocket.server.HandshakeRequest;
import javax.websocket.server.ServerContainer;
import javax.websocket.server.ServerEndpointConfig;

import com.googlecode.gwtrpcplus.server.GwtRpcPlusWebsocketFilter;
import com.googlecode.gwtrpcplus.server.internal.util.Logger;
import com.googlecode.gwtrpcplus.server.servlet.GwtRpcPlusWebsocket;

public class WebsocketSetupImpl implements WebsocketSetup {
	private final static Logger logger = new Logger(GwtRpcPlusWebsocketFilter.class);

	@Override
	public boolean init(ServletContext servletContext, String websocketPath, RpcManagerServer manager) {
		boolean added = false;
		// Try initialize Websockets
		if (servletContext == null)
			logger.error("Websockets only works if you are using the GuiceServletContextListener to create your injector.");
		final ServerContainer serverContainer = (ServerContainer) servletContext.getAttribute("javax.websocket.server.ServerContainer");

		// @SuppressWarnings("unchecked")
		// Enumeration<String> attributeNames = servletContext.getAttributeNames();
		// while (attributeNames.hasMoreElements()) {
		// String attributeName = attributeNames.nextElement();
		// System.out.println(attributeName + " => " + servletContext.getAttribute(attributeName));
		// }

		if (serverContainer == null) {
			logger.warn("No JSR-356 Websocket-Support found for " + servletContext.getServerInfo());
		} else {
			try {
				serverContainer.setDefaultMaxTextMessageBufferSize(1000000);

				MyConfigurator configurator = new MyConfigurator(servletContext.getContextPath(), manager);

				ServerEndpointConfig cfg = ServerEndpointConfig.Builder.create(GwtRpcPlusWebsocket.class, websocketPath).configurator(configurator).build();
				serverContainer.addEndpoint(cfg);
				added = true;
			} catch (DeploymentException e) {
				logger.error("Error while deploying WebsocketEndpoint", e);
			}
			logger.info("Websocket-Support initialized.");
		}
		return added;
	}

	private static class MyConfigurator extends ServerEndpointConfig.Configurator {
		private final String contextPath;
		private final RpcManagerServer manager;

		public MyConfigurator(String contextPath, RpcManagerServer manager) {
			this.contextPath = contextPath;
			this.manager = manager;
		}

		@Override
		public void modifyHandshake(ServerEndpointConfig config, HandshakeRequest request, HandshakeResponse response) {
			HttpSession httpSession = (HttpSession) request.getHttpSession();
			config.getUserProperties().put(HttpSession.class.getName(), httpSession);
			config.getUserProperties().put(RpcManagerServer.class.getName(), manager);
			config.getUserProperties().put(CONTEXT_PATH_NAME, contextPath);
		}
	}
}
