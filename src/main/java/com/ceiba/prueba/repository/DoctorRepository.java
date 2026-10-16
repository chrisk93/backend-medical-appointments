package com.ceiba.prueba.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ceiba.prueba.domain.model.Doctor;

public interface DoctorRepository extends JpaRepository<Doctor, Long> {
}
