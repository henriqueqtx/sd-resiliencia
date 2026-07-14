package com.example.sdpersistencia.service;

import com.example.sdpersistencia.OpcaoVoto;
import com.example.sdpersistencia.repository.OpcaoVotoRepository;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.timelimiter.annotation.TimeLimiter;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.CompletableFuture;

@Service
public class VotacaoService {

    private final OpcaoVotoRepository repository;
    private final java.util.concurrent.Executor executor = java.util.concurrent.Executors.newFixedThreadPool(20);

    public VotacaoService(OpcaoVotoRepository repository) {
        this.repository = repository;
    }

    @CircuitBreaker(name = "bancoDados", fallbackMethod = "fallbackVerPlacar")
    @TimeLimiter(name = "bancoDados", fallbackMethod = "fallbackVerPlacar")
    public CompletableFuture<List<OpcaoVoto>> verPlacar() {
        return CompletableFuture.supplyAsync(() -> repository.findAll(Sort.by("nomeDaOpcao")), executor);
    }

    public CompletableFuture<List<OpcaoVoto>> fallbackVerPlacar(Exception e) {
        OpcaoVoto erro = new OpcaoVoto();
        erro.setNomeDaOpcao("Sistema Indisponível (Modo Resiliência)");
        erro.setQuantidadeVotos(0);
        return CompletableFuture.completedFuture(List.of(erro));
    }

    @CircuitBreaker(name = "bancoDados", fallbackMethod = "fallbackVotar")
    @TimeLimiter(name = "bancoDados", fallbackMethod = "fallbackVotar")
    public CompletableFuture<ResponseEntity<String>> votar(String nome) {
        return CompletableFuture.supplyAsync(() -> {
            repository.computarVotoRapido(nome);
            return ResponseEntity.ok("Voto computado com sucesso para: " + nome);
        }, executor);
    }

    public CompletableFuture<ResponseEntity<String>> fallbackVotar(String nome, Exception e) {
        return CompletableFuture.completedFuture(
                ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                        .body("Banco de dados instável. Voto para " + nome + " não processado, tente novamente.")
        );
    }
}
