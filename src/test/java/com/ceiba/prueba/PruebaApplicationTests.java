package com.ceiba.prueba;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.ceiba.prueba.domain.model.Doctor;
import com.ceiba.prueba.repository.DoctorRepository;

@SpringBootTest
class PruebaApplicationTests {

	@Autowired
	private DoctorRepository doctorRepository;

	@Test
	void contextLoads() {
	}

	@Test
	void startup_loadsTheThreeSeedDoctors() {
		assertThat(doctorRepository.findAll())
				.extracting(Doctor::getFullName)
				.containsExactlyInAnyOrder("Dra. María González", "Dr. Carlos Ruiz", "Dra. Ana López");
	}

}
