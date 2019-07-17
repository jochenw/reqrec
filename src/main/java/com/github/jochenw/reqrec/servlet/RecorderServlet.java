package com.github.jochenw.reqrec.servlet;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.util.Enumeration;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.logging.log4j.Logger;

import com.github.jochenw.reqrec.app.App;
import com.github.jochenw.reqrec.model.RequestRegistry;
import com.github.jochenw.reqrec.model.RequestRegistry.RequestBuilder;

public class RecorderServlet extends HttpServlet {
	private static final long serialVersionUID = 4593746755421994625L;
	private static Logger logger;

	@Override
	public void init(ServletConfig config) throws ServletException {
		super.init(config);
		logger = App.getLogger(getClass());
		logger.info("Initialized");
	}



	@Override
	protected void service(HttpServletRequest pReq, HttpServletResponse pRes) throws ServletException, IOException {
		logger.debug("->");
		final RequestRegistry rr = App.getInstance().getRequestRegistry();
		try (final RequestBuilder r = rr.request()) {
			r.method(pReq.getMethod()).requestUri(pReq.getRequestURI()).localAddr(pReq.getLocalAddr())
			.localPort(pReq.getLocalPort()).remoteAddr(pReq.getRemoteAddr()).remotePort(pReq.getRemotePort());
			for (Enumeration<?> headerNames = pReq.getHeaderNames();  headerNames.hasMoreElements();  ) {
				final String headerName = (String) headerNames.nextElement();
				for (final Enumeration<?> values = pReq.getHeaders(headerName);  values.hasMoreElements();  ) {
					final String value = (String) values.nextElement();
					r.header(headerName, value);
				}
			}
			try (InputStream in = pReq.getInputStream();
					OutputStream os = r.getOutputStream()) {
				final byte[] buffer = new byte[8192];
				for (;;) {
					final int res = in.read(buffer);
					if (res == -1) {
						break;
					} else if (res > 0) {
						os.write(buffer, 0, res);
					}
				}
			} catch (IOException e) {
				throw new UncheckedIOException(e);
			}
		}
		pRes.setStatus(HttpServletResponse.SC_OK);
		pRes.getOutputStream().close();
		logger.debug("<-");
	}
}
