package com.project.HireConnect.service;

import com.project.HireConnect.model.Postulante;
import com.project.HireConnect.repository.PostulanteRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class PostulanteService {

    private final PostulanteRepository postulanteRepository;
    private final PasswordEncoder passwordEncoder;

    public PostulanteService(PostulanteRepository postulanteRepository, PasswordEncoder passwordEncoder) {
        this.postulanteRepository = postulanteRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public long contarPostulantes() {
        return postulanteRepository.count();
    }

    // Registro de Postulante
    public Postulante registrarPostulante(Postulante postulante) {
        postulante.setPassword(passwordEncoder.encode(postulante.getPassword()));
        return postulanteRepository.save(postulante);
    }

    // Buscar por ID - CAMBIADO: Long a String
    public Optional<Postulante> buscarPorId(String id) {
        return postulanteRepository.findById(id);
    }

    // Buscar por Email
    public Optional<Postulante> buscarPorEmail(String email) {
        return postulanteRepository.findByEmail(email);
    }

    // Guardar o actualizar postulante
    public Postulante guardar(Postulante postulante) {
        return postulanteRepository.save(postulante);
    }

    // Listar todos los postulantes
    public List<Postulante> listarTodos() {
        return postulanteRepository.findAll();
    }

    // Eliminar postulante por ID - CAMBIADO: Long a String
    public void eliminar(String id) {
        postulanteRepository.deleteById(id);
    }

    // Actualizar CV - CAMBIADO: Long a String
    public void actualizarCv(String id, String cvPath) throws RuntimeException {
        Postulante postulante = postulanteRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Postulante no encontrado con ID: " + id));

        postulante.setCv(cvPath);
        postulanteRepository.save(postulante);
    }

    // Actualizar Password - CAMBIADO: Long a String
    public boolean actualizarPassword(String id, String passwordActual, String nuevaPassword) {
        Optional<Postulante> postulanteOpt = postulanteRepository.findById(id);

        if (postulanteOpt.isEmpty()) {
            return false;
        }

        Postulante postulante = postulanteOpt.get();
        if (passwordEncoder.matches(passwordActual, postulante.getPassword())) {
            postulante.setPassword(passwordEncoder.encode(nuevaPassword));
            postulanteRepository.save(postulante);
            return true;
        } else {
            return false;
        }
    }

    public long contarPostulantesConCV() {
        return postulanteRepository.countByCvIsNotNull();
    }
}