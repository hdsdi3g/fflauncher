package tv.hd3g.fflauncher.enums;

import tv.hd3g.commons.IORuntimeException;

public enum SourceNotFoundPolicy {
	ERROR,
	REMOVE_OUT_STREAM;

	public static class SourceNotFoundException extends IORuntimeException {
		public SourceNotFoundException(final String message) {
			super(message);
		}
	}
}
