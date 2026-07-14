package com.example.sdpersistencia.controller;

import com.example.sdpersistencia.OpcaoVoto;
import com.example.sdpersistencia.service.VotacaoService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api/votacao")
@CrossOrigin(origins = "*")
public class VotacaoController {

    private final VotacaoService service;

    public VotacaoController(VotacaoService service) {
        this.service = service;
    }

    @GetMapping
    public CompletableFuture<List<OpcaoVoto>> verPlacar() {
        return service.verPlacar();
    }

    @PostMapping("/{nome}")
    public CompletableFuture<ResponseEntity<String>> votar(@PathVariable String nome) {
        return service.votar(nome);
    }
}
