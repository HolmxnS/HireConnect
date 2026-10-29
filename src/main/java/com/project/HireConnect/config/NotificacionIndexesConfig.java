package com.project.HireConnect.config;

import com.project.HireConnect.model.NotificacionPostulante;
import java.time.Duration;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.Index;

@Configuration
public class NotificacionIndexesConfig {

    @Bean
    public ApplicationRunner crearIndiceExpiracionNotificaciones(MongoTemplate mongoTemplate) {
        // El TTL se registra explícitamente porque la creación automática de índices está desactivada.
        return args -> mongoTemplate.indexOps(NotificacionPostulante.class).ensureIndex(
                new Index().on("fecha", Sort.Direction.ASC)
                        .named("notificaciones_fecha_ttl_30d")
                        .expire(Duration.ofDays(30)));
    }
}
