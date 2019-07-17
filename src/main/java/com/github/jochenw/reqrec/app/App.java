package com.github.jochenw.reqrec.app;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LoggerContext;

import com.github.jochenw.reqrec.model.RequestRegistry;

public class App {
	private static final App INSTANCE = new App();
	public static App getInstance() {
		return INSTANCE;
	}
	private App() {
		// Does nothing
	}

	private RequestRegistry requestRegistry;
	private Logger logger;
	private LoggerContext loggerContext;
	private Properties properties;

	public Properties getProperties() {
		return properties;
	}
	public void setProperties(Properties pProperties) {
		properties = pProperties;
		requestRegistry = newRequestRegistry(properties);
	}
	public static Logger getLogger(Class<?> pType) {
		return getInstance().loggerContext.getLogger(pType.getName());
	}
	public static Logger getLogger(String pId) {
		return getInstance().loggerContext.getLogger(pId);
	}
	
	public LoggerContext getLoggerContext() {
		return loggerContext;
	}

	public void setLoggerContext(LoggerContext pLoggerContext) {
		loggerContext = pLoggerContext;
		logger = getLogger(App.class);
	}

	public RequestRegistry getRequestRegistry() {
		return requestRegistry;
	}

	protected RequestRegistry newRequestRegistry(Properties pProperties) {
		final String requestDir = getProperty(pProperties, "request.dir");
		if (requestDir == null  ||  requestDir.length() == 0) {
			throw new IllegalStateException("Missing, or empty, property: request.dir");
		}
		final String requestDirCreateStr = getProperty(pProperties, "request.dir.create");
		final boolean requestDirCreate = Boolean.parseBoolean(requestDirCreateStr);
		final Path requestPath = Paths.get(requestDir);
		logger.debug("Request directory is: " + requestDir);
		final Path dir = requestPath;
		if (dir != null  &&  !Files.isDirectory(dir)) {
			if (requestDirCreate) {
				logger.info("Creating request directory: {}", dir);
				try {
					Files.createDirectory(dir);
				} catch (IOException e) {
					throw new UncheckedIOException(e);
				}
			} else {
				throw new IllegalStateException("Request directory does not exist, and creation forbidden: " + dir);
			}
		}
		return new RequestRegistry(requestPath);
	}

	public String getProperty(Properties pProperties, String pKey) {
		final String value = pProperties.getProperty(pKey);
		final int varOffset = value.indexOf("${");
		if (varOffset == -1) {
			return value;
		}
		final int varEnd = value.indexOf('}', varOffset+2);
		if (varEnd == -1) {
			throw new IllegalStateException("Unable to parse property value for key " + pKey + ": " + value);
		}
		final String suffix = value.substring(0, varOffset);
		final String prefix = value.substring(varEnd+1);
		final String varName = value.substring(varOffset+2, varEnd);
		if (varName.startsWith("env.")) {
			final String systemPropertyName = varName.substring(4);
			final String systemPropertyValue = System.getProperty(systemPropertyName);
			return suffix + (systemPropertyValue == null ? "" : systemPropertyValue) + prefix;
		} else {
			return suffix + getProperty(pProperties, pKey);
		}
	}
}
