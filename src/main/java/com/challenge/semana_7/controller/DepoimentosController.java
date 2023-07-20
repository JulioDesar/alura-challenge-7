package com.challenge.semana_7.controller;

import com.challenge.semana_7.model.Depoimentos;
import com.challenge.semana_7.service.DepoimentoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api")
public class DepoimentosController {

    @Autowired
    private DepoimentoService service;

    @GetMapping
    public ResponseEntity<Iterable<Depoimentos>> getAll() {
        return ResponseEntity.ok(service.getAll());
    }

    @PostMapping("/v1/depoimentos")
    public ResponseEntity<Depoimentos> save(@RequestParam("nome") String nome, @RequestParam("depoimentos") String depoimentos, @RequestParam("foto") MultipartFile foto) {
        Depoimentos depoimento = new Depoimentos(null, nome, depoimentos, null);
        return ResponseEntity.ok(service.save(depoimento, foto));
    }
}
