package com.github.jochenw.reqrec.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

public class SwappableOutputStream extends OutputStream {
	private final long swapSize;
	private long currentSize;
	private final Path path;
	private ByteArrayOutputStream baos = new ByteArrayOutputStream();
	private OutputStream os;

	public SwappableOutputStream(Path pPath, int pSwapSize) {
		swapSize = pSwapSize;
		currentSize = 0;
		path = pPath;
	}

	protected OutputStream getOutputStream(int pSize) throws IOException {
		if (baos == null) {
			return os;
		} else {
			if (currentSize + pSize > swapSize) {
				os = Files.newOutputStream(path);
				baos.writeTo(os);
				baos.close();
				baos = null;
				return os;
			} else {
				return baos;
			}
		}
	}
	@Override
	public void write(int pByte) throws IOException {
		final OutputStream os = getOutputStream(1);
		os.write(pByte);
		currentSize += 1;
	}

	@Override
	public void write(byte[] pBuffer) throws IOException {
		write(pBuffer, 0, pBuffer.length);
	}

	@Override
	public void write(byte[] pBuffer, int pOffset, int pLength) throws IOException {
		final OutputStream os = getOutputStream(pLength);
		os.write(pBuffer, pOffset, pLength);
		currentSize += pLength;
	}

	@Override
	public void close() throws IOException {
		Throwable t = null;
		if (os != null) {
			try {
				os.close();
			} catch (Throwable th) {
				t = th;
			}
		}
		if (baos != null) {
			try {
				baos.close();
			} catch (Throwable th) {
				if (t == null) {
					th = t;
				}
			}
		}
		if (t != null) {
			if (t instanceof IOException) {
				throw (IOException) t;
			}
			if (t instanceof RuntimeException) {
				throw (RuntimeException) t;
			}
			if (t instanceof Error) {
				throw (Error) t;
			}
		}
	}

	public boolean isSwapped() {
		return baos == null;
	}

	public Path getPath() {
		return path;
	}

	public byte[] getBytes() {
		if (baos == null) {
			return null;
		} else {
			return baos.toByteArray();
		}
	}
}
