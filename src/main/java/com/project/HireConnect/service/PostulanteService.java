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

    public Postulante registrarPostulante(Postulante postulante) {
        postulante.setPassword(passwordEncoder.encode(postulante.getPassword()));
        return postulanteRepository.save(postulante);
    }

    public Optional<Postulante> buscarPorId(String id) {
        return postulanteRepository.findById(id);
    }

    public Optional<Postulante> buscarPorEmail(String email) {
        return postulanteRepository.findByEmail(email);
    }

    public Postulante guardar(Postulante postulante) {
        return postulanteRepository.save(postulante);
    }

    public List<Postulante> listarTodos() {
        return postulanteRepository.findAll();
    }

    public void eliminar(String id) {
        postulanteRepository.deleteById(id);
    }

    public void actualizarCv(String id, String cvPath) throws RuntimeException {
        Postulante postulante = postulanteRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Postulante no encontrado con ID: " + id));

        postulante.setCv(cvPath);
        postulanteRepository.save(postulante);
    }

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