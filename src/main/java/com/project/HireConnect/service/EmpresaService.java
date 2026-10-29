package com.project.HireConnect.service;

import java.util.List;
import java.util.Optional;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.project.HireConnect.enums.EmpresaEstado;
import com.project.HireConnect.model.Empresa;
import com.project.HireConnect.repository.EmpresaRepository;

@Service
public class EmpresaService {

    private final EmpresaRepository empresaRepository;
    private final PasswordEncoder passwordEncoder;

    public EmpresaService(EmpresaRepository empresaRepository, PasswordEncoder passwordEncoder) {
        this.empresaRepository = empresaRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public long contarEmpresas() {
        return empresaRepository.count();
    }

    
    public Empresa registrarEmpresa(Empresa empresa) {
        empresa.setPassword(passwordEncoder.encode(empresa.getPassword()));
        return empresaRepository.save(empresa);
    }

    public Optional<Empresa> obtenerPorId(String id) {
        return empresaRepository.findById(id);
    }

    public Empresa guardar(Empresa empresa) {
        return empresaRepository.save(empresa);
    }

    public List<Empresa> listarTodas() {
        return empresaRepository.findAll();
    }

    public void eliminar(String id) {
        empresaRepository.deleteById(id);
    }

    public Optional<Empresa> buscarPorEmailEmpresa(String email) {
        return empresaRepository.findByEmail(email);
    }

    public Optional<Empresa> buscarPorrut(String rut) {
        return empresaRepository.findByRut(rut);
    }

    public boolean actualizarPassword(String id, String passwordActual, String nuevaPassword) {
        Optional<Empresa> emprOptional = empresaRepository.findById(id);

        if (emprOptional.isEmpty()) {
            return false;
        }

        Empresa empresa = emprOptional.get();
        if (passwordEncoder.matches(passwordActual, empresa.getPassword())) {
            empresa.setPassword(passwordEncoder.encode(nuevaPassword));
            empresaRepository.save(empresa);
            return true;
        } else {
            return false;
        }
    }

    public int contarEmpresasActivas() {
        return empresaRepository.countByEstado(EmpresaEstado.APROBADA);
    }

    public int contarEmpresasDeshabilitadas() {
        return empresaRepository.countByEstado(EmpresaEstado.SUSPENDIDA);
    }
}