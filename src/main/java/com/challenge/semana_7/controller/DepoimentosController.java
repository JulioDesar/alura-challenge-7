package com.challenge.semana_7.controller;

import com.challenge.semana_7.dto.GetDepoimentosDto;
import com.challenge.semana_7.dto.UploadDepoimentosDto;
import com.challenge.semana_7.service.DepoimentoService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/depoimentos")
public class DepoimentosController {

    @Autowired
    private DepoimentoService service;

    @GetMapping
    public ResponseEntity<Iterable<GetDepoimentosDto>> getAll() {
        return ResponseEntity.ok(service.getAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<GetDepoimentosDto> getById(@PathVariable Long id) {
        return ResponseEntity.ok(service.getById(id));
    }

    @PostMapping("/upload")
    public ResponseEntity<UploadDepoimentosDto> save(@RequestParam("depoimentos") String depoimentos, @RequestParam("foto") MultipartFile foto) throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        UploadDepoimentosDto depoimentosDto = mapper.readValue(depoimentos, UploadDepoimentosDto.class);

        return ResponseEntity.created(null).body(service.save(depoimentosDto, foto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<UploadDepoimentosDto> update(@PathVariable Long id, @RequestParam("depoimentos") String depoimentos, @RequestParam("foto") MultipartFile foto) throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        UploadDepoimentosDto depoimentosDto = mapper.readValue(depoimentos, UploadDepoimentosDto.class);

        return ResponseEntity.ok(service.update(id, depoimentosDto, foto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        return service.delete(id);
    }

}
