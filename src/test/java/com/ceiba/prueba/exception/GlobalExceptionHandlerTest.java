package com.ceiba.prueba.exception;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

@WebMvcTest(GlobalExceptionHandlerTest.ThrowingController.class)
@Import(GlobalExceptionHandlerTest.ThrowingController.class)
class GlobalExceptionHandlerTest {

	private static final String PROBLEM_JSON = MediaType.APPLICATION_PROBLEM_JSON_VALUE;

	@Autowired
	private MockMvc mockMvc;

	@Test
	void resourceNotFound_returns404WithMessage() throws Exception {
		mockMvc.perform(get("/test/not-found"))
				.andExpect(status().isNotFound())
				.andExpect(content().contentTypeCompatibleWith(PROBLEM_JSON))
				.andExpect(jsonPath("$.status").value(404))
				.andExpect(jsonPath("$.title").value("Not Found"))
				.andExpect(jsonPath("$.detail").value("Doctor 99 not found"));
	}

	@Test
	void errorResponse_doesNotIncludeInstance() throws Exception {
		mockMvc.perform(get("/test/not-found"))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.instance").doesNotExist());
	}

	@Test
	void validationErrorResponse_doesNotIncludeInstance() throws Exception {
		mockMvc.perform(post("/test/body").contentType(MediaType.APPLICATION_JSON).content("{\"name\": \" \"}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.instance").doesNotExist())
				.andExpect(jsonPath("$.errors[0].field").value("name"));
	}

	@Test
	void conflict_returns409WithMessage() throws Exception {
		mockMvc.perform(get("/test/conflict"))
				.andExpect(status().isConflict())
				.andExpect(content().contentTypeCompatibleWith(PROBLEM_JSON))
				.andExpect(jsonPath("$.status").value(409))
				.andExpect(jsonPath("$.title").value("Conflict"))
				.andExpect(jsonPath("$.detail").value("Time slot already taken"));
	}

	@Test
	void businessRule_returns400WithMessage() throws Exception {
		mockMvc.perform(get("/test/business-rule"))
				.andExpect(status().isBadRequest())
				.andExpect(content().contentTypeCompatibleWith(PROBLEM_JSON))
				.andExpect(jsonPath("$.status").value(400))
				.andExpect(jsonPath("$.title").value("Bad Request"))
				.andExpect(jsonPath("$.detail").value("Appointment must start at a 30-minute slot"));
	}

	@Test
	void invalidBody_returns400WithFieldErrors() throws Exception {
		mockMvc.perform(post("/test/body")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"name": " ", "date": "2026-10-19"}
						"""))
				.andExpect(status().isBadRequest())
				.andExpect(content().contentTypeCompatibleWith(PROBLEM_JSON))
				.andExpect(jsonPath("$.status").value(400))
				.andExpect(jsonPath("$.errors[0].field").value("name"))
				.andExpect(jsonPath("$.errors[0].message").isNotEmpty());
	}

	@Test
	void malformedJson_returns400() throws Exception {
		mockMvc.perform(post("/test/body")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"name\": "))
				.andExpect(status().isBadRequest())
				.andExpect(content().contentTypeCompatibleWith(PROBLEM_JSON))
				.andExpect(jsonPath("$.status").value(400));
	}

	@Test
	void nonIsoDate_returns400() throws Exception {
		mockMvc.perform(post("/test/body")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"name": "Ana", "date": "15/10/2000"}
						"""))
				.andExpect(status().isBadRequest())
				.andExpect(content().contentTypeCompatibleWith(PROBLEM_JSON))
				.andExpect(jsonPath("$.status").value(400));
	}

	@Test
	void paramTypeMismatch_returns400() throws Exception {
		mockMvc.perform(get("/test/param").param("number", "abc"))
				.andExpect(status().isBadRequest())
				.andExpect(content().contentTypeCompatibleWith(PROBLEM_JSON))
				.andExpect(jsonPath("$.status").value(400))
				.andExpect(jsonPath("$.detail").value("Invalid value for parameter 'number'."))
				.andExpect(content().string(not(containsString("abc"))));
	}

	@Test
	void missingParam_returns400() throws Exception {
		mockMvc.perform(get("/test/param"))
				.andExpect(status().isBadRequest())
				.andExpect(content().contentTypeCompatibleWith(PROBLEM_JSON))
				.andExpect(jsonPath("$.status").value(400));
	}

	@Test
	void constraintViolation_returns400() throws Exception {
		mockMvc.perform(get("/test/constraint-violation"))
				.andExpect(status().isBadRequest())
				.andExpect(content().contentTypeCompatibleWith(PROBLEM_JSON))
				.andExpect(jsonPath("$.status").value(400))
				.andExpect(jsonPath("$.detail").value("Invalid request parameters."));
	}

	@Test
	void dataIntegrityViolation_returns409WithoutDriverMessage() throws Exception {
		mockMvc.perform(get("/test/data-integrity"))
				.andExpect(status().isConflict())
				.andExpect(content().contentTypeCompatibleWith(PROBLEM_JSON))
				.andExpect(jsonPath("$.status").value(409))
				.andExpect(jsonPath("$.detail").value("The operation conflicts with existing data."))
				.andExpect(content().string(not(containsString("1234567"))));
	}

	@Test
	void unexpectedException_returns500WithGenericMessage() throws Exception {
		mockMvc.perform(get("/test/unexpected"))
				.andExpect(status().isInternalServerError())
				.andExpect(content().contentTypeCompatibleWith(PROBLEM_JSON))
				.andExpect(jsonPath("$.status").value(500))
				.andExpect(jsonPath("$.title").value("Internal Server Error"))
				.andExpect(jsonPath("$.detail").value("An unexpected error occurred."))
				.andExpect(content().string(not(containsString("secret internal detail"))))
				.andExpect(content().string(not(containsString("IllegalStateException"))));
	}

	@Test
	void unsupportedMethod_keeps405InsteadOf500() throws Exception {
		mockMvc.perform(post("/test/not-found"))
				.andExpect(status().isMethodNotAllowed())
				.andExpect(jsonPath("$.status").value(405));
	}

	@RestController
	@RequestMapping("/test")
	static class ThrowingController {

		record Body(@NotBlank String name, LocalDate date) {
		}

		@GetMapping("/not-found")
		void notFound() {
			throw new ResourceNotFoundException("Doctor 99 not found");
		}

		@GetMapping("/conflict")
		void conflict() {
			throw new ConflictException("Time slot already taken");
		}

		@GetMapping("/business-rule")
		void businessRule() {
			throw new BusinessRuleException("Appointment must start at a 30-minute slot");
		}

		@PostMapping("/body")
		void body(@Valid @RequestBody Body body) {
		}

		@GetMapping("/param")
		void param(@RequestParam Integer number) {
		}

		@GetMapping("/constraint-violation")
		void constraintViolation() {
			throw new ConstraintViolationException("invalid", Set.of());
		}

		@GetMapping("/data-integrity")
		void dataIntegrity() {
			throw new DataIntegrityViolationException("Unique index violation: DOCUMENT_NUMBER = '1234567'");
		}

		@GetMapping("/unexpected")
		void unexpected() {
			throw new IllegalStateException("secret internal detail");
		}

	}

}
