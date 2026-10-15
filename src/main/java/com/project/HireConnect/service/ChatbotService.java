package com.project.HireConnect.service;

import java.time.LocalDate;
import java.time.Period;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import com.project.HireConnect.enums.VacanteEstado;
import com.project.HireConnect.model.Educacion_postulante;
import com.project.HireConnect.model.Experiencia_postulante;
import com.project.HireConnect.model.Postulante;
import com.project.HireConnect.model.Vacante;
import com.project.HireConnect.repository.VacanteRepository;

@Service
public class ChatbotService {

    private static final Logger log = LoggerFactory.getLogger(ChatbotService.class);
    private static final String BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models/";
    private static final int MAX_VACANTES_EN_PROMPT = 15;
    private static final int MAX_TURNOS_HISTORIAL = 10;

    private final VacanteRepository vacanteRepository;
    private final RestClient restClient;
    private final String apiKey;
    private final List<String> modelos;

    // Si la clave no existe, la app ARRANCA igual y el chat avisa
    public ChatbotService(VacanteRepository vacanteRepository,
            @Value("${gemini.api.key:}") String apiKey,
            @Value("${gemini.model:gemini-3.5-flash}") String model,
            @Value("${gemini.model.fallback:gemini-3.6-flash}") String fallbackModel) {
        this.vacanteRepository = vacanteRepository;
        this.apiKey = apiKey == null ? "" : apiKey.trim();
        this.modelos = model.equals(fallbackModel) ? List.of(model) : List.of(model, fallbackModel);

        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5_000);
        factory.setReadTimeout(30_000);
        this.restClient = RestClient.builder().requestFactory(factory).build();

        if (this.apiKey.isBlank()) {
            log.warn("GEMINI_API_KEY no está configurada: el chatbot responderá con un aviso hasta que se defina.");
        }
    }

    @SuppressWarnings("unchecked")
        public String responder(Postulante postulante, String mensaje, List<Map<String, String>> historial) {
        if (apiKey.isBlank()) {
            return "El asistente no está configurado todavía (falta la clave GEMINI_API_KEY).";
        }

        List<Map<String, Object>> contents = new ArrayList<>();
        if (historial != null) {
            int desde = Math.max(0, historial.size() - MAX_TURNOS_HISTORIAL);
            for (Map<String, String> turno : historial.subList(desde, historial.size())) {
                String rol = "model".equals(turno.get("rol")) ? "model" : "user";
                String texto = turno.get("texto");
                if (texto != null && !texto.isBlank()) {
                    contents.add(Map.of("role", rol, "parts", List.of(Map.of("text", texto))));
                }
            }
        }
        contents.add(Map.of("role", "user", "parts", List.of(Map.of("text", mensaje))));

        Map<String, Object> body = Map.of(
                "systemInstruction", Map.of("parts", List.of(Map.of("text", construirInstrucciones(postulante)))),
                "contents", contents);

        for (String modelo : modelos) {
            try {
                return llamarGemini(modelo, body);
            } catch (RestClientResponseException e) {
                log.error("Gemini ({}) respondió {} : {}", modelo, e.getStatusCode(), e.getResponseBodyAsString());
                int code = e.getStatusCode().value();
                // 500/503 = saturado, 429 = cuota, 404 = modelo no disponible: probar el siguiente modelo
                if (code != 500 && code != 503 && code != 429 && code != 404) {
                    break;
                }
            } catch (Exception e) {
                log.error("Error llamando a Gemini ({})", modelo, e);
                break;
            }
        }
        return "El asistente no está disponible en este momento. Inténtalo de nuevo en unos minutos.";
    }

    @SuppressWarnings("unchecked")
    private String llamarGemini(String modelo, Map<String, Object> body) {
        Map<String, Object> respuesta = restClient.post()
                .uri(BASE_URL + modelo + ":generateContent")
                .header("x-goog-api-key", apiKey)
                .header("Content-Type", "application/json")
                .body(body)
                .retrieve()
                .body(Map.class);

        List<Map<String, Object>> candidates = (List<Map<String, Object>>) respuesta.get("candidates");
        if (candidates == null || candidates.isEmpty()) {
            return "No pude generar una respuesta para eso. ¿Puedes reformular tu pregunta?";
        }
        Map<String, Object> content = (Map<String, Object>) candidates.get(0).get("content");
        List<Map<String, Object>> parts = (List<Map<String, Object>>) content.get("parts");
        return parts.stream()
                .map(p -> (String) p.get("text"))
                .filter(t -> t != null)
                .collect(Collectors.joining());
    }

    private String construirInstrucciones(Postulante p) {
        StringBuilder sb = new StringBuilder();
        sb.append("""
                Eres el asistente de empleabilidad de HireConnect, una plataforma de empleo en Colombia.
                Tu trabajo: (1) ayudar al postulante a mejorar su perfil (habilidades, estudios, experiencia, CV)
                y (2) recomendarle vacantes de la lista de abajo según su perfil.
                Reglas:
                - Responde siempre en español, de forma cercana, clara y breve (máximo ~150 palabras salvo que pidan más).
                - Recomienda SOLO vacantes de la lista "VACANTES ACTIVAS". Nunca inventes vacantes ni empresas.
                - Al recomendar una vacante, enlázala así: [Título](/postulante/vacante/ID) y explica en una frase por qué encaja.
                - Si ninguna vacante encaja bien, dilo con honestidad y sugiere qué habilidades agregar al perfil.
                - Si el perfil está incompleto, dile exactamente qué falta agregar (habilidades, experiencia, educación, CV).
                - No des asesoría legal ni pidas datos sensibles (documentos, contraseñas). Si preguntan algo ajeno al empleo, redirige con amabilidad.
                - Puedes usar **negrita** y listas con guiones. No uses tablas ni encabezados.
                """);

        // Solo se envía a Gemini lo necesario: sin email, teléfono ni número de documento.
        sb.append("\nPERFIL DEL POSTULANTE:\n");
        sb.append("- Nombre: ").append(nvl(p.getNombre())).append('\n');
        sb.append("- Titulación: ").append(nvl(p.getTitulacion())).append('\n');
        if (p.getFechaNacimiento() != null) {
            sb.append("- Edad: ").append(Period.between(p.getFechaNacimiento(), LocalDate.now()).getYears()).append('\n');
        }
        Set<String> habilidades = p.getHabilidades();
        sb.append("- Habilidades: ")
                .append(habilidades == null || habilidades.isEmpty() ? "(ninguna registrada)" : String.join(", ", habilidades))
                .append('\n');

        sb.append("- Educación:\n");
        if (p.getEducacion() == null || p.getEducacion().isEmpty()) {
            sb.append("  (ninguna registrada)\n");
        } else {
            for (Educacion_postulante e : p.getEducacion()) {
                sb.append("  * ").append(nvl(e.getTitulo())).append(" — ").append(nvl(e.getInstitucion())).append('\n');
            }
        }

        sb.append("- Experiencia:\n");
        if (p.getExperiencia() == null || p.getExperiencia().isEmpty()) {
            sb.append("  (ninguna registrada)\n");
        } else {
            for (Experiencia_postulante x : p.getExperiencia()) {
                sb.append("  * ").append(nvl(x.getCargo())).append(" en ").append(nvl(x.getEmpresa()));
                if (x.getDescripcion() != null && !x.getDescripcion().isBlank()) {
                    sb.append(": ").append(x.getDescripcion());
                }
                sb.append('\n');
            }
        }
        sb.append("- Tiene CV cargado: ").append(p.getCv() != null && !p.getCv().isBlank() ? "sí" : "no").append('\n');

        sb.append("\nVACANTES ACTIVAS (las más afines al perfil primero):\n");
        List<Vacante> vacantes = vacantesAfines(habilidades);
        if (vacantes.isEmpty()) {
            sb.append("(no hay vacantes activas en este momento)\n");
        }
        for (Vacante v : vacantes) {
            sb.append("- ID: ").append(v.getId())
                    .append(" | ").append(nvl(v.getTitulo()))
                    .append(" | Empresa: ").append(v.getEmpresa() != null ? nvl(v.getEmpresa().getNombre()) : "N/D")
                    .append(" | Modalidad: ").append(seguro(() -> v.getModalidad().getDescripcion()))
                    .append(" | Contrato: ").append(seguro(() -> v.getTipoContrato().getDescripcion()))
                    .append(" | Experiencia: ").append(seguro(() -> v.getExperienciaRequerida().getDescripcion()))
                    .append(" | Salario: ").append(v.getSalario() != null && v.getSalario() > 0 ? "$" + v.getSalario().longValue() : "N/D")
                    .append(" | Habilidades requeridas: ").append(nvl(v.getHabilidadesRequeridas()))
                    .append('\n');
        }
        return sb.toString();
    }

    private List<Vacante> vacantesAfines(Set<String> habilidades) {
        List<Vacante> activas = vacanteRepository.findByEstado(VacanteEstado.ACTIVA);
        Set<String> skills = habilidades == null ? Set.of()
                : habilidades.stream().map(String::toLowerCase).collect(Collectors.toSet());

        return activas.stream()
                .sorted(Comparator.comparingLong((Vacante v) -> coincidencias(v, skills)).reversed())
                .limit(MAX_VACANTES_EN_PROMPT)
                .collect(Collectors.toList());
    }

    private long coincidencias(Vacante v, Set<String> skills) {
        String req = v.getHabilidadesRequeridas();
        if (req == null || skills.isEmpty()) {
            return 0;
        }
        String reqLower = req.toLowerCase();
        return skills.stream().filter(reqLower::contains).count();
    }

    private static String nvl(String s) {
        return s == null || s.isBlank() ? "N/D" : s;
    }

    private static String seguro(Supplier<String> s) {
        try {
            String v = s.get();
            return v == null ? "N/D" : v;
        } catch (Exception e) {
            return "N/D";
        }
    }
}