package com.github.jochenw.reqrec.model;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiConsumer;

import com.github.jochenw.reqrec.util.SwappableOutputStream;

public class RequestRegistry {
	/** This class provides a reduced part of the {@link Request}, which is considered to be publicly available.
	 */
	public static class RequestInfo {
		private final String id, method, requestUri, localAddr, remoteAddr;
		private final String[] headers;
		private final int localPort, remotePort;
		public RequestInfo(String pId, String pMethod, String pRequestUri, String pLocalAddr, String pRemoteAddr,
				           int pLocalPort, int pRemotePort, String[] pHeaders) {
			id = Objects.requireNonNull(pId, "Id");
			method = pMethod;
			requestUri = pRequestUri;
			headers = pHeaders;
			localAddr = pLocalAddr;
			localPort = pLocalPort;
			remotePort = pRemotePort;
			remoteAddr = pRemoteAddr;
		}
		public String getId() { return id; }
		public String getMethod() { return method; }
		public String getRequestUri() { return requestUri; }
		public String getLocalAddr() { return localAddr; }
		public String getRemoteAddr() { return remoteAddr; }
		public int getLocalPort() { return localPort; }
		public int getRemotePort() { return remotePort; }
		public void headers(BiConsumer<String,String> pConsumer) {
			if (headers != null) {
				for (int i = 0;  i < headers.length;  i += 2) {
					pConsumer.accept(headers[i], headers[i+1]);
				}
			}
		}
	}
	public static class Request {
		private final RequestInfo requestInfo;
		private final byte[] body;
		private Path path;
		Request(RequestInfo pRequestInfo, byte[] pBody) {
			requestInfo = pRequestInfo;
			body = pBody;
			path = null;
		}
		Request(RequestInfo pRequestInfo, Path pPath) {
			requestInfo = pRequestInfo;
			body = null;
			path = pPath;
		}
		public String getId() { return requestInfo.getId(); }
		public RequestInfo getRequestInfo() { return requestInfo; }
		public boolean hasBody() { return (body != null  &&  body.length > 0)  ||  path != null; } 

		public String getText(String pType) {
			if ("raw".equals(pType)) {
				if (body == null) {
					if (path == null) {
						return "";
					} else {
						final StringBuilder sb = new StringBuilder();
						try (final Reader br = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
							final char[] buffer = new char[8192];
							for (;;) {
								final int res = br.read(buffer);
								if (res == -1) {
									break;
								} else if (res > 0) {
									sb.append(buffer, 0, res);
								}
							}
						} catch (IOException e) {
							throw new UncheckedIOException(e);
						}
						return sb.toString();
					}
				} else {
					return new String(body, StandardCharsets.UTF_8);
				}
			} else {
				throw new IllegalStateException("Invalid type: " + pType);
			}
		}
	}

	public class RequestBuilder implements AutoCloseable {
		private final String id;
		private final List<String> headers = new ArrayList<>();
		private String method, requestUri, localAddr, remoteAddr;
		private int localPort, remotePort;
		private boolean immutable;
		private SwappableOutputStream sos;
		private boolean closed;

		RequestBuilder(String pId) {
			id = pId;
		}
		
		protected void assertMutable() {
			if (immutable) {
				throw new IllegalStateException("This object is no longer mutable.");
			}
		}

		protected void makeImmutable() {
			assertMutable();
			immutable = true;
		}

		@Override
		public void close() {
			if (sos != null) {
				try {
					sos.close();
					closed = true;
				} catch (IOException e) {
					throw new UncheckedIOException(e);
				}
			}
			makeImmutable();
			final RequestInfo ri = asRequestInfo();
			final Request r = asRequest(ri);
			add(r);
		}

		protected RequestInfo asRequestInfo() {
			final String[] hArray = headers.toArray(new String[headers.size()]);
			return new RequestInfo(id, method, requestUri, localAddr, remoteAddr, localPort, remotePort, hArray);
		}

		protected Request asRequest(RequestInfo pRi) {
			if (!closed) {
				throw new IllegalStateException("Request is not yet closed.");
			}
			if (sos.isSwapped()) {
				return new Request(pRi, sos.getPath());
			} else {
				return new Request(pRi, sos.getBytes());
			}
		}

		public RequestBuilder method(String pMethod) {
			assertMutable();
			method = pMethod;
			return this;
		}

		public RequestBuilder requestUri(String pURI) {
			assertMutable();
			requestUri = pURI;
			return this;
		}

		public RequestBuilder localAddr(String pAddr) {
			assertMutable();
			localAddr = pAddr;
			return this;
		}

		public RequestBuilder localPort(int pPort) {
			assertMutable();
			localPort = pPort;
			return this;
		}

		public RequestBuilder remoteAddr(String pAddr) {
			assertMutable();
			remoteAddr = pAddr;
			return this;
		}

		public RequestBuilder remotePort(int pPort) {
			assertMutable();
			remotePort = pPort;
			return this;
		}

		public RequestBuilder header(String pName, String pValue) {
			assertMutable();
			headers.add(pName);
			headers.add(pValue);
			return this;
		}

		public OutputStream getOutputStream() {
			if (sos == null) {
				sos = RequestRegistry.this.getOutputStream(id);
				return sos;
			} else {
				throw new IllegalStateException("Output stream already open");
			}
		}
	}

	private final Path requestDir;
	private long num;
	private final List<Request> requests = new ArrayList<>();
	private final Map<String,Request> requestsById = new HashMap<String,Request>();

	public RequestRegistry(Path pRequestDir) {
		requestDir = pRequestDir;
	}

	public Path getRequestDir(Path pPath) {
		return requestDir;
	}

	public void add(Request pRequest) {
		synchronized(requests) {
			requests.add(pRequest);
			requestsById.put(pRequest.getId(), pRequest);
		}
	}

	public List<RequestInfo> getRequestList() {
		synchronized(requests) {
			final List<RequestInfo> list= new ArrayList<RequestInfo>(requests.size());
			for (Request r : requests) {
				list.add(r.getRequestInfo());
			}
			return list;
		}
	}

	public Request getRequest(String pId) {
		synchronized(requests) {
			return requestsById.get(pId);
		}
	}
	
	public final RequestBuilder request() {
		final String id = newId();
		return new RequestBuilder(id);
	}

	protected String newId() {
		final long n;
		synchronized(requests) {
			n = num++;
		}
		return String.valueOf(n);
	}

	protected SwappableOutputStream getOutputStream(String pId) {
		final Path file = requestDir.resolve("out-" + pId + ".xml");
		return new SwappableOutputStream(file, 8192);
	}
}
