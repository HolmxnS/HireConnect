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

    public org.springframework.data.domain.Page<Empresa> paginaAdmin(String texto, String estado, String orden, int numero) {
        String consulta = texto == null ? "" : texto.trim();
        if (consulta.length() > 120) consulta = consulta.substring(0, 120);
        String patron = java.util.regex.Pattern.quote(consulta);
        org.springframework.data.domain.Sort.Direction direccion = "oldest".equals(orden)
                ? org.springframework.data.domain.Sort.Direction.ASC : org.springframework.data.domain.Sort.Direction.DESC;
        var pageable = org.springframework.data.domain.PageRequest.of(Math.max(0, numero), 20,
                org.springframework.data.domain.Sort.by(direccion, "fechaRegistro").and(org.springframework.data.domain.Sort.by(direccion, "id")));
        EmpresaEstado filtro = "APROBADA".equals(estado) ? EmpresaEstado.APROBADA
                : "SUSPENDIDA".equals(estado) ? EmpresaEstado.SUSPENDIDA : null;
        var pagina = filtro == null ? empresaRepository.buscarPaginaAdmin(patron, pageable)
                : empresaRepository.buscarPaginaAdminPorEstado(patron, filtro, pageable);
        if (pagina.getTotalPages() > 0 && pagina.getNumber() >= pagina.getTotalPages()) {
            var ultima = org.springframework.data.domain.PageRequest.of(pagina.getTotalPages() - 1, 20, pageable.getSort());
            return filtro == null ? empresaRepository.buscarPaginaAdmin(patron, ultima)
                    : empresaRepository.buscarPaginaAdminPorEstado(patron, filtro, ultima);
        }
        return pagina;
    }

    public java.util.Map<String, String> sectoresPorIds(java.util.Set<String> ids) {
        if (ids.isEmpty()) return java.util.Map.of();
        return empresaRepository.findAllById(ids).stream()
                .filter(e -> e.getSector() != null && !e.getSector().isBlank())
                .collect(java.util.stream.Collectors.toMap(Empresa::getId, e -> e.getSector().trim()));
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
