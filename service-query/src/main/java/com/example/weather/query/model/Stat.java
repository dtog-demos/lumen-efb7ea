package com.example.weather.query.model;

import com.example.weather.common.InvalidRequestException;
import java.util.Locale;

public enum Stat {
	MIN,
	MAX,
	SUM,
	AVG;

	public static Stat parse(String text) {
		try {
			return valueOf(text.trim().toUpperCase(Locale.ROOT));
		} catch (IllegalArgumentException e) {
			throw new InvalidRequestException("stat must be one of: min, max, sum, avg");
		}
	}
}
