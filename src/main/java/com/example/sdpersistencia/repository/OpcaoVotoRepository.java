package com.example.sdpersistencia.repository;

import com.example.sdpersistencia.OpcaoVoto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

public interface OpcaoVotoRepository extends JpaRepository<OpcaoVoto, String> {

    @Modifying
    @Transactional
    @Query(value = "INSERT INTO opcao_voto (nome_da_opcao, quantidade_votos) VALUES (:nome, 1) " +
                   "ON CONFLICT (nome_da_opcao) DO UPDATE SET quantidade_votos = opcao_voto.quantidade_votos + 1", 
           nativeQuery = true)
    void computarVotoRapido(String nome);
}
