package tv.hd3g.fflauncher;

import java.io.IOException;

public class IORuntimeException extends RuntimeException {

	public IORuntimeException(final String message, final IOException cause) {
		super(message, cause);
	}

	public IORuntimeException(final String message) {
		super(message);
	}

}
