package com.example.weather.query.model;

import com.example.weather.common.InvalidRequestException;

import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

@ExtendWith(MockitoExtension.class)
public class StatTest {
	// Supported aggregate names parse to their constants.
	@Test
	void parsesWhitelistedNames() {
		assertEquals(Stat.AVG, Stat.parse("avg"));
		assertEquals(Stat.AVG, Stat.parse("AVG"));
		assertEquals(Stat.MAX, Stat.parse("Max"));
        assertEquals(Stat.MIN, Stat.parse("min"));
	}

	// Names outside the whitelist are rejected.
	@Test
	void rejectsMedian() {
		assertThrows(InvalidRequestException.class, () -> Stat.parse("median"));
	}
}
