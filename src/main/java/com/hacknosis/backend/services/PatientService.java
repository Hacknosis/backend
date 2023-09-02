package com.hacknosis.backend.services;

import com.hacknosis.backend.exceptions.ResourceNotFoundException;
import com.hacknosis.backend.models.Appointment;
import com.hacknosis.backend.models.Patient;
import com.hacknosis.backend.repositories.AppointmentRepository;
import com.hacknosis.backend.repositories.PatientRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import javax.security.auth.login.AccountNotFoundException;

@Service
@AllArgsConstructor
public class PatientService {
    private PatientRepository patientRepository;
    private UserService userService;
    private AppointmentRepository appointmentRepository;

    public void updatePatient(Patient patient) throws AccountNotFoundException {
        if (patientRepository.findById(patient.getId()).isEmpty()) {
            throw new AccountNotFoundException("The provided patient entity does not exist");
        }
        if (patient.getUser() == null) {
            Patient oldPatient = patientRepository.findById(patient.getId()).get();
            patient.setUser(oldPatient.getUser());
        }
        patientRepository.save(patient);
    }

    public Appointment upsertAppointment(Appointment appointment, long patientId, String username) throws AccountNotFoundException {
        if (!userService.usernameExist(username)) {
            throw new AccountNotFoundException("The authenticated Doctor account does not exist");
        } else if (!patientRepository.existsById(patientId)) {
            throw new ResourceNotFoundException(String.format("The patient with id - %d does not exist", patientId));
        }
        Patient patient = patientRepository.getReferenceById(patientId);
        appointment.setPatient(patient);
        return appointmentRepository.save(appointment);
    }
    public void deleteAppointment(long appointmentId, String username) throws AccountNotFoundException, ResourceNotFoundException {
        if (!userService.usernameExist(username)) {
            throw new AccountNotFoundException("The authenticated Doctor account does not exist");
        } else if (!appointmentRepository.existsById(appointmentId)) {
            throw new ResourceNotFoundException(String.format("The appointment with id - %d does not exist", appointmentId));
        }
        appointmentRepository.deleteById(appointmentId);
    }
}
