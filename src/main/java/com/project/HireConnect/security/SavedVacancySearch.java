package com.project.HireConnect.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.web.savedrequest.HttpSessionRequestCache;
import org.springframework.web.util.UriComponentsBuilder;

/** Resume only the local vacancy search after candidate authentication. */
final class SavedVacancySearch {
    private SavedVacancySearch() {}

    static String destination(HttpServletRequest request, HttpServletResponse response) {
        var cache = new HttpSessionRequestCache();
        var saved = cache.getRequest(request, response);
        if (saved == null) return "/postulante/vacantes";
        String path;
        try {
            path = java.net.URI.create(saved.getRedirectUrl()).getRawPath();
        } catch (IllegalArgumentException error) {
            return "/postulante/vacantes";
        }
        String base = request.getContextPath() + "/postulante/vacantes";
        if ((request.getContextPath() + "/postulante/postular").equals(path)) {
            String[] ids = saved.getParameterValues("vacanteId");
            cache.removeRequest(request, response);
            if (ids != null && ids.length > 0 && ids[0] != null && ids[0].matches("[A-Za-z0-9_-]{1,128}"))
                return UriComponentsBuilder.fromPath("/postulante/vacante/{id}").encode().buildAndExpand(ids[0]).toUriString();
            return "/postulante/vacantes";
        }
        if (!base.equals(path) && !(base + "/listar").equals(path)) return "/postulante/vacantes";
        String[] values = saved.getParameterValues("filtro");
        if (values == null) values = saved.getParameterValues("query");
        cache.removeRequest(request, response);
        if (values == null || values.length == 0 || values[0] == null || values[0].isBlank())
            return "/postulante/vacantes";
        String text = values[0].trim();
        text = text.substring(0, Math.min(text.length(), 100));
        return UriComponentsBuilder.fromPath("/postulante/vacantes")
                .queryParam("filtro", "{text}").encode().buildAndExpand(text).toUriString();
    }
}
