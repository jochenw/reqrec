package com.github.jochenw.reqrec.servlet;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URL;
import java.util.Properties;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.ConfigurationSource;
import org.apache.logging.log4j.core.config.Configurator;

import com.github.jochenw.reqrec.app.App;


public class HttpRequestRecorderInitializer implements ServletContextListener {
	private Logger logger;

	@Override
	public void contextDestroyed(ServletContextEvent pEvent) {
		if (logger != null) {
			logger.info("contextDestroyed: ->");
			logger.info("contextDestroyed: <-");
		}
		System.out.println("HTTP Request Recorder: Shutdown");
	}

	@Override
	public void contextInitialized(ServletContextEvent pEvent) {
		final ServletContext sc = pEvent.getServletContext();
		String webAppName = sc.getContextPath();
		final int nameOffset = webAppName.lastIndexOf('/');
		if (nameOffset != -1) {
			webAppName = webAppName.substring(nameOffset+1);
		}
		initLogging(webAppName);
		initProperties(webAppName);
	}

	protected void initLogging(String pWebappName) {
		final String uri;
		if (pWebappName == null  ||  pWebappName.length() == 0) {
			uri = "log4j2.xml";
		} else {
			uri = pWebappName + "/log4j2.xml";
		}
		final ClassLoader cl = Thread.currentThread().getContextClassLoader();
		URL url = cl.getResource(uri);
		if (url == null) {
			final String defaultUri = "log4j2-default.xml";
			url = cl.getResource(defaultUri);
			if (url == null) {
				throw new IllegalStateException("Unable to locate URI: " + defaultUri);
			}
		}

		final LoggerContext loggerContext;
		try (InputStream in = url.openStream()) {
			final ConfigurationSource cs = new ConfigurationSource(in, url);
			loggerContext = Configurator.initialize(cl, cs);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
		logger = loggerContext.getLogger(getClass().getName());
		logger.info("Logging initialized from {}", url.toExternalForm());
		App.getInstance().setLoggerContext(loggerContext);
	}

	protected void initProperties(String pWebappName) {
		final Properties props = new Properties();
		final ClassLoader cl = Thread.currentThread().getContextClassLoader();
		final String factoryUri = "reqrec-factory.properties";
		final URL factoryUrl = cl.getResource(factoryUri);
		if (factoryUrl == null) {
			throw new IllegalStateException("Unable to locate URI: " + factoryUri);
		}
		final Properties factoryProperties = new Properties();
		logger.info("Loading factory properties from: {}", factoryUrl.toExternalForm());
		try (InputStream in = factoryUrl.openStream()) {
			factoryProperties.load(in);
			props.putAll(factoryProperties);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}

		final String uri;
		if (pWebappName == null  ||  pWebappName.length() == 0) {
			uri = "reqrec.properties";
		} else {
			uri = pWebappName + "/reqrec.properties";
		}
		URL url = cl.getResource(uri);
		if (url == null) {
			logger.warn("No instance properties found");
		} else {
			logger.info("Loading instance properties from: {}", url.toExternalForm());
			final Properties instanceProperties = new Properties();
			try (InputStream in = factoryUrl.openStream()) {
				instanceProperties.load(in);
				props.putAll(instanceProperties);
			} catch (IOException e) {
				throw new UncheckedIOException(e);
			}
		}

		App.getInstance().setProperties(props);
	}
}
