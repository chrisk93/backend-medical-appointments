package com.ceiba.prueba.controller;

import java.time.LocalDate;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.format.annotation.DateTimeFormat.ISO;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.ceiba.prueba.dto.request.DoctorRequest;
import com.ceiba.prueba.dto.response.DoctorResponse;
import com.ceiba.prueba.dto.response.TimeSlotResponse;
import com.ceiba.prueba.service.AvailabilityService;
import com.ceiba.prueba.service.DoctorService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/doctors")
@RequiredArgsConstructor
public class DoctorController {

	private final DoctorService doctorService;

	private final AvailabilityService availabilityService;

	/** RF-01: registers a doctor. */
	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public DoctorResponse create(@Valid @RequestBody DoctorRequest request) {
		return doctorService.create(request);
	}

	/** RF-04: free 30-minute slots of a doctor; both dates are ISO 8601 ({@code yyyy-MM-dd}) and inclusive. */
	@GetMapping("/{doctorId}/availability")
	public List<TimeSlotResponse> getAvailability(@PathVariable Long doctorId,
			@RequestParam @DateTimeFormat(iso = ISO.DATE) LocalDate startDate,
			@RequestParam @DateTimeFormat(iso = ISO.DATE) LocalDate endDate) {
		return availabilityService.getAvailableSlots(doctorId, startDate, endDate);
	}

}
