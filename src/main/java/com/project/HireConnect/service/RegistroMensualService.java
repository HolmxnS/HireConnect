package com.project.HireConnect.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.project.HireConnect.dto.RegistroMensualDTO;
import com.project.HireConnect.repository.EmpresaRepository;
import com.project.HireConnect.repository.PostulanteRepository;

@Service
public class RegistroMensualService {

        @Autowired
        private EmpresaRepository empresaRepo;

        @Autowired
        private PostulanteRepository postulanteRepo;

        public List<RegistroMensualDTO> registrosDelMes() {
                List<RegistroMensualDTO> registros = new ArrayList<>();

                List<RegistroMensualDTO> postulantes = postulanteRepo.findPostulantesRegistradosMesActual()
                                .stream()
                                .map(p -> new RegistroMensualDTO(
                                                p.getNombre() + " " + p.getApellido(),
                                                "Postulante",
                                                p.getEmail(),
                                                p.getFechaRegistro(),
                                                p.getFoto()))
                                .collect(Collectors.toList());

                List<RegistroMensualDTO> empresas = empresaRepo.findEmpresasRegistradasMesActual()
                                .stream()
                                .map(e -> new RegistroMensualDTO(
                                                e.getNombreLegal(),
                                                "Empresa",
                                                e.getEmail(),
                                                e.getFechaRegistro(),
                                                e.getFoto()))
                                .collect(Collectors.toList());

                registros.addAll(postulantes);
                registros.addAll(empresas);

                return registros;
        }

        public Map<String, List<Integer>> registrosPorMes() {
                List<Integer> empresa = new ArrayList<>(java.util.Collections.nCopies(12, 0));
                List<Integer> postulante = new ArrayList<>(java.util.Collections.nCopies(12, 0));

                empresaRepo.findAll().forEach(e -> {
                        if (e.getFechaRegistro() != null) {
                                int mes = e.getFechaRegistro().getMonthValue() - 1; // 0-indexado
                                empresa.set(mes, empresa.get(mes) + 1);
                        }
                });

                postulanteRepo.findAll().forEach(p -> {
                        if (p.getFechaRegistro() != null) {
                                int mes = p.getFechaRegistro().getMonthValue() - 1;
                                postulante.set(mes, postulante.get(mes) + 1);
                        }
                });

                Map<String, List<Integer>> map = new HashMap<>();
                map.put("empresa", empresa);
                map.put("postulante", postulante);
                return map;
        }
}