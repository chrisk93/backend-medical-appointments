package com.ceiba.prueba.repository.specification;

import static com.ceiba.prueba.repository.specification.AppointmentSpecifications.hasDoctorId;
import static com.ceiba.prueba.repository.specification.AppointmentSpecifications.hasPatientId;
import static com.ceiba.prueba.repository.specification.AppointmentSpecifications.hasStatus;
import static com.ceiba.prueba.repository.specification.AppointmentSpecifications.matching;
import static com.ceiba.prueba.repository.specification.AppointmentSpecifications.onOrAfter;
import static com.ceiba.prueba.repository.specification.AppointmentSpecifications.onOrBefore;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.data.jpa.domain.Specification;

import com.ceiba.prueba.domain.model.Appointment;
import com.ceiba.prueba.domain.model.AppointmentStatus;
import com.ceiba.prueba.domain.model.Doctor;
import com.ceiba.prueba.domain.model.Patient;
import com.ceiba.prueba.repository.AppointmentRepository;

/**
 * Uses the seed doctors (IDs 1 and 2): inserting doctors would shift the ID expected by DoctorRepositoryTest.
 */
@DataJpaTest
class AppointmentSpecificationsTest {

	private static final LocalDate MONDAY = LocalDate.of(2026, 10, 19);
	private static final LocalDate TUESDAY = LocalDate.of(2026, 10, 20);

	@Autowired
	private AppointmentRepository appointmentRepository;

	@Autowired
	private TestEntityManager entityManager;

	private Appointment anaMondayDoctor1;
	private Appointment anaTuesdayLastSlotDoctor2;
	private Appointment luisMondayDoctor1Cancelled;
	private Appointment luisWednesdayDoctor2;

	@BeforeEach
	void setUp() {
		Doctor doctor1 = entityManager.find(Doctor.class, 1L);
		Doctor doctor2 = entityManager.find(Doctor.class, 2L);
		Patient ana = entityManager.persist(
				new Patient("Ana Torres", "1020304050", "3001234567", "ana.torres@mail.com", null));
		Patient luis = entityManager.persist(
				new Patient("Luis Gómez", "5040302010", "3007654321", "luis.gomez@mail.com", null));

		anaMondayDoctor1 = entityManager.persist(new Appointment(ana, doctor1, MONDAY.atTime(8, 0)));
		anaTuesdayLastSlotDoctor2 = entityManager.persist(new Appointment(ana, doctor2, TUESDAY.atTime(17, 30)));
		Appointment cancelled = new Appointment(luis, doctor1, MONDAY.atTime(9, 0));
		cancelled.cancel(LocalDateTime.of(2026, 10, 18, 12, 0));
		luisMondayDoctor1Cancelled = entityManager.persist(cancelled);
		luisWednesdayDoctor2 = entityManager.persist(
				new Appointment(luis, doctor2, LocalDate.of(2026, 10, 21).atTime(8, 0)));
		entityManager.flush();
	}

	@Test
	void noFilters_returnsAllAppointments() {
		assertThat(find(Specification.allOf(hasDoctorId(null), hasPatientId(null), hasStatus(null),
				onOrAfter(null), onOrBefore(null))))
				.containsExactlyInAnyOrder(anaMondayDoctor1, anaTuesdayLastSlotDoctor2, luisMondayDoctor1Cancelled,
						luisWednesdayDoctor2);
	}

	@Test
	void hasDoctorId_returnsOnlyThatDoctor() {
		assertThat(find(hasDoctorId(1L))).containsExactlyInAnyOrder(anaMondayDoctor1, luisMondayDoctor1Cancelled);
	}

	@Test
	void hasPatientId_returnsOnlyThatPatient() {
		assertThat(find(hasPatientId(anaMondayDoctor1.getPatient().getId())))
				.containsExactlyInAnyOrder(anaMondayDoctor1, anaTuesdayLastSlotDoctor2);
	}

	@Test
	void hasStatus_returnsOnlyThatStatus() {
		assertThat(find(hasStatus(AppointmentStatus.CANCELLED))).containsExactly(luisMondayDoctor1Cancelled);
		assertThat(find(hasStatus(AppointmentStatus.SCHEDULED)))
				.containsExactlyInAnyOrder(anaMondayDoctor1, anaTuesdayLastSlotDoctor2, luisWednesdayDoctor2);
	}

	@Test
	void onOrAfter_includesAppointmentsFromStartOfStartDate() {
		assertThat(find(onOrAfter(TUESDAY))).containsExactlyInAnyOrder(anaTuesdayLastSlotDoctor2, luisWednesdayDoctor2);
	}

	@Test
	void onOrBefore_includesWholeEndDateAndExcludesNextDay() {
		// The 17:30 slot of the end date is included; Wednesday 08:00 is not.
		assertThat(find(onOrBefore(TUESDAY)))
				.containsExactlyInAnyOrder(anaMondayDoctor1, anaTuesdayLastSlotDoctor2, luisMondayDoctor1Cancelled);
	}

	@Test
	void sameStartAndEndDate_returnsOnlyThatDay() {
		assertThat(find(Specification.allOf(onOrAfter(MONDAY), onOrBefore(MONDAY))))
				.containsExactlyInAnyOrder(anaMondayDoctor1, luisMondayDoctor1Cancelled);
	}

	@Test
	void allFiltersCombined_returnsOnlyMatchingAppointment() {
		Long anaId = anaMondayDoctor1.getPatient().getId();

		assertThat(find(Specification.allOf(hasDoctorId(2L), hasPatientId(anaId),
				hasStatus(AppointmentStatus.SCHEDULED), onOrAfter(MONDAY), onOrBefore(TUESDAY))))
				.containsExactly(anaTuesdayLastSlotDoctor2);
	}

	@Test
	void filtersWithNoMatch_returnsEmpty() {
		assertThat(find(Specification.allOf(hasDoctorId(1L), hasStatus(AppointmentStatus.SCHEDULED),
				onOrAfter(TUESDAY)))).isEmpty();
	}

	@Test
	void matching_allNull_returnsAllAppointments() {
		assertThat(find(matching(null, null, null, null, null)))
				.containsExactlyInAnyOrder(anaMondayDoctor1, anaTuesdayLastSlotDoctor2, luisMondayDoctor1Cancelled,
						luisWednesdayDoctor2);
	}

	@Test
	void matching_allValues_appliesEveryFilter() {
		Long anaId = anaMondayDoctor1.getPatient().getId();

		assertThat(find(matching(2L, anaId, AppointmentStatus.SCHEDULED, MONDAY, TUESDAY)))
				.containsExactly(anaTuesdayLastSlotDoctor2);
	}

	@Test
	void matching_someValues_ignoresNullFilters() {
		assertThat(find(matching(1L, null, AppointmentStatus.SCHEDULED, null, null)))
				.containsExactly(anaMondayDoctor1);
	}

	private List<Appointment> find(Specification<Appointment> specification) {
		return appointmentRepository.findAll(specification);
	}

}
