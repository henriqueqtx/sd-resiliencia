package com.example.sdpersistencia.controller;

import com.example.sdpersistencia.OpcaoVoto;
import com.example.sdpersistencia.repository.OpcaoVotoRepository;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.timelimiter.annotation.TimeLimiter;
@RestController
@RequestMapping("/api/votacao")
@CrossOrigin(origins = "*")

public class VotacaoController {



    private final OpcaoVotoRepository repository;
    private final java.util.concurrent.Executor executor = java.util.concurrent.Executors.newFixedThreadPool(20);

    public VotacaoController(OpcaoVotoRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    @CircuitBreaker(name = "bancoDados", fallbackMethod = "fallbackVerPlacar")
    @TimeLimiter(name = "bancoDados", fallbackMethod = "fallbackVerPlacarCompletable")
    public CompletableFuture<List<OpcaoVoto>> verPlacar() {
        return CompletableFuture.supplyAsync(() -> repository.findAll(Sort.by("nomeDaOpcao")), executor);
    }

    // Fallback do TimeLimiter (precisa bater a assinatura com CompletableFuture)
    public CompletableFuture<List<OpcaoVoto>> fallbackVerPlacarCompletable(Exception e) {
        return CompletableFuture.completedFuture(fallbackVerPlacar(e));
    }

    // Fallback do CircuitBreaker
    public List<OpcaoVoto> fallbackVerPlacar(Exception e) {
        OpcaoVoto erro = new OpcaoVoto();
        erro.setNomeDaOpcao("Sistema Indisponível (Modo Resiliência)");
        erro.setQuantidadeVotos(0);
        return List.of(erro);
    }


    @PostMapping("/{nome}")
    @CircuitBreaker(name = "bancoDados", fallbackMethod = "fallbackVotar")
    public CompletableFuture<ResponseEntity<String>> votar(@PathVariable String nome) {
        return CompletableFuture.supplyAsync(() -> {
            OpcaoVoto opcao = repository.findById(nome).orElse(null);

            if (opcao == null) {
                opcao = new OpcaoVoto();
                opcao.setNomeDaOpcao(nome);
                opcao.setQuantidadeVotos(0);
            }

            opcao.setQuantidadeVotos(opcao.getQuantidadeVotos() + 1);
            repository.save(opcao);
            return ResponseEntity.ok("Voto computado com sucesso para: " + nome);
        }, executor);
    }

    public ResponseEntity<String> fallbackVotar(String nome, Exception e) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body("Banco de dados instável. Voto para " + nome + " não processado, tente novamente.");
    }
}
