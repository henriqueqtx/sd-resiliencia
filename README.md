# 🛡️ Sistema de Votação Resiliente

Este projeto é uma demonstração prática de Arquitetura de Software com foco em **Resiliência e Sistemas Distribuídos**. O objetivo é simular um sistema de votação que continua operando de forma íntegra mesmo sob cenários de falhas catastróficas, como latência extrema, indisponibilidade do banco de dados e queda repentina de instâncias.

### 🛠️ Tecnologias Utilizadas
- **Backend:** Java 21, Spring Boot
- **Banco de Dados:** PostgreSQL
- **Infraestrutura & Orquestração:** Docker, Kubernetes (Minikube), HPA
- **Resiliência & Testes:** Resilience4j, Chaos Mesh (Chaos Engineering)

---

## 🌪️ Demonstração (Chaos Engineering)

O sistema foi submetido a experimentos reais de caos. Abaixo estão alguns dos comportamentos observados:

### 1. Isolamento de Falha (Circuit Breaker + Fallback)
Ao injetar latência de 500ms no banco de dados, o Resilience4j abre o circuito instantaneamente para evitar exaustão de threads. A interface exibe a mensagem de sistema indisponível sem travar o navegador do usuário.

<img width="1287" height="636" alt="Captura de tela 2026-07-14 220126" src="https://github.com/user-attachments/assets/386e3073-a161-4f8d-acf8-00c6ab808b79" />

### 2. Autoescalonamento (Stress Test)
Sob carga massiva de CPU via `StressChaos`, o Horizontal Pod Autoscaler (HPA) detecta o pico além dos 50% permitidos e escala a API automaticamente para dividir a carga.

<img width="844" height="160" alt="Captura de tela 2026-07-24 154940" src="https://github.com/user-attachments/assets/4c11fce6-0b68-4828-9b11-1bbd9550859d" />

## 🎯 Arquitetura de Resiliência

Para garantir a alta disponibilidade e evitar o Efeito Cascata, as seguintes técnicas foram implementadas no nível do código e da infraestrutura:

* **Circuit Breaker (Resilience4j):** Interrompe chamadas ao banco de dados se 50% das requisições passarem de 2 segundos, retornando `503` instantaneamente para proteger o banco de sobrecarga.
* **Fallback (Degradação Graciosa):** Se o banco cair, a leitura retorna uma interface de `"Modo Resiliência"` no Frontend, garantindo que o usuário não veja uma tela de erro em branco.
* **TimeLimiter:** Timeout dinâmico de 3 segundos para leitura de placares, abortando a requisição antes de travar a thread.
* **Pool de Threads Dedicado:** Isolamento de execução assíncrona com 20 threads nativas para transações de banco, mitigando o estrangulamento de CPU imposto pelo Kubernetes e baixando a latência basal.
* **Kubernetes Probes & HPA:** Uso de `startupProbe` e `readinessProbe` para garantir inicialização segura. O *Horizontal Pod Autoscaler (HPA)* clona a API automaticamente (de 3 a 10 instâncias) se o consumo de CPU passar de 50%.

---

## 🚀 Como testar na sua máquina (Developer Experience)

Para facilitar a avaliação, **as builds compiladas do Java (`.jar`) já estão inclusas**. Você não precisa ter JDK ou Gradle instalados, o Docker fará todo o trabalho.

**Pré-requisitos:** Docker Desktop, Minikube, kubectl, Helm e Make.

### 1. Subindo o Cluster
```bash
# Inicie o Minikube
minikube start --driver=docker

# Faça o deploy automatizado (API, Banco, Front, HPA e Chaos Mesh)
make run

# Exponha o serviço para acessar no navegador
make service
