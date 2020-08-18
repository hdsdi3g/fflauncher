package tv.hd3g.fflauncher.filtering;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class FilterArgumentTest {

	String key;
	String value;

	FilterArgument filterArgument;

	@BeforeEach
	void init() {
		key = String.valueOf(System.nanoTime());
		value = String.valueOf(System.nanoTime());
		filterArgument = new FilterArgument(key, value);
	}

	@Test
	void testFilterArgument() {
		filterArgument = new FilterArgument(key);
		assertEquals(key, filterArgument.getKey());
		assertNull(filterArgument.getValue());
	}

	@Test
	void testGetKey() {
		assertEquals(key, filterArgument.getKey());
	}

	@Test
	void testGetValue() {
		assertEquals(value, filterArgument.getValue());
	}

	@Test
	void testSetValue() {
		value = String.valueOf(System.nanoTime());
		filterArgument.setValue(value);
		assertEquals(value, filterArgument.getValue());
	}

	@Test
	void testToString() {
		assertEquals(key + "=" + value, filterArgument.toString());
		assertEquals(key, new FilterArgument(key, null).toString());
	}

	@Test
	void testHashCode() {
		assertEquals(filterArgument.hashCode(), new FilterArgument(key, value).hashCode());
	}

	@Test
	void testEqualsObject() {
		assertEquals(filterArgument, new FilterArgument(key, value));
	}
}
