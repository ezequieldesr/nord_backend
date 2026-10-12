package com.nord.nord.controller;

import com.nord.nord.config.CatalogImages;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.time.Duration;

@RestController
@RequestMapping("/api/catalogo/imagens")
public class CatalogImageController {
    @GetMapping("/{arquivo}")
    public ResponseEntity<Resource> imagem(@PathVariable String arquivo) {
        // Lista fechada: nunca aceita um caminho de arquivo arbitrário do cliente.
        if (!CatalogImages.ALLOWED_FILES.contains(arquivo)) return ResponseEntity.notFound().build();
        Resource resource = new ClassPathResource("catalogo/imagens/" + arquivo);
        if (!resource.exists()) return ResponseEntity.notFound().build();
        return ResponseEntity.ok().contentType(MediaType.IMAGE_PNG)
                .cacheControl(CacheControl.maxAge(Duration.ofDays(7)).cachePublic())
                .body(resource);
    }
}