package com.ceiba.prueba.domain.model;

import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "patient")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Patient {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, length = 100)
	private String fullName;

	@Column(nullable = false, unique = true)
	private String documentNumber;

	@Column(nullable = false)
	private String phone;

	@Column(nullable = false)
	private String email;

	private LocalDate birthDate;

	public Patient(String fullName, String documentNumber, String phone, String email, LocalDate birthDate) {
		this.fullName = fullName;
		this.documentNumber = documentNumber;
		this.phone = phone;
		this.email = email;
		this.birthDate = birthDate;
	}

}
