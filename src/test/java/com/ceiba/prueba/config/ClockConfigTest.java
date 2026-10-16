package com.ceiba.prueba.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.ZoneId;

import org.junit.jupiter.api.Test;

class ClockConfigTest {

	@Test
	void clock_default_usesSystemDefaultZone() {
		// Given
		ClockConfig config = new ClockConfig();

		// When
		Clock clock = config.clock();

		// Then
		assertThat(clock.getZone()).isEqualTo(ZoneId.systemDefault());
	}

}
