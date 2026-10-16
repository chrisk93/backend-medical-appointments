package com.ceiba.prueba.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.ceiba.prueba.dto.request.AppointmentFilter;
import com.ceiba.prueba.dto.request.AppointmentRequest;
import com.ceiba.prueba.dto.response.AppointmentResponse;
import com.ceiba.prueba.service.AppointmentService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/appointments")
@RequiredArgsConstructor
public class AppointmentController {

	private final AppointmentService appointmentService;

	/** RF-03: schedules an appointment. */
	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public AppointmentResponse schedule(@Valid @RequestBody AppointmentRequest request) {
		return appointmentService.schedule(request);
	}

	/** RF-05: cancels a scheduled appointment. */
	@PostMapping("/{id}/cancellation")
	public AppointmentResponse cancel(@PathVariable Long id) {
		return appointmentService.cancel(id);
	}

	/** RF-06: lists appointments; every query parameter is optional. */
	@GetMapping
	public List<AppointmentResponse> search(@ModelAttribute AppointmentFilter filter) {
		return appointmentService.search(filter);
	}

}
