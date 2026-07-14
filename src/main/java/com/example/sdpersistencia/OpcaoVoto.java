package com.example.sdpersistencia;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

@Entity
public class OpcaoVoto {

    @Id
    private String nomeDaOpcao;
    private int quantidadeVotos;

    public OpcaoVoto() {}

    public int getQuantidadeVotos() {
        return quantidadeVotos;
    }

    public void setQuantidadeVotos(int quantidadeVotos) {
        this.quantidadeVotos = quantidadeVotos;
    }

    public String getNomeDaOpcao() {
        return nomeDaOpcao;
    }

    public void setNomeDaOpcao(String nomeDaOpcao) {
        this.nomeDaOpcao = nomeDaOpcao;
    }
}
