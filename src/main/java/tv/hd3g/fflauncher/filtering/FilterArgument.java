package tv.hd3g.fflauncher.filtering;

import java.util.Objects;

public class FilterArgument {

	private final String key;
	private String value;

	public FilterArgument(final String key, final String value) {
		this.key = key;
		this.value = value;
	}

	public FilterArgument(final String key) {
		this.key = key;
	}

	public String getKey() {
		return key;
	}

	/**
	 * @return can be null
	 */
	public String getValue() {
		return value;
	}

	public void setValue(final String value) {
		this.value = value;
	}

	@Override
	public String toString() {
		if (value != null) {
			return key + "=" + value;
		} else {
			return key;
		}
	}

	/**
	 * Only use key
	 */
	@Override
	public int hashCode() {
		return Objects.hash(key);
	}

	/**
	 * Only use key
	 */
	@Override
	public boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		final var other = (FilterArgument) obj;
		return Objects.equals(key, other.key);
	}

}
