# Sistema de Votação Resiliente

Este projeto é uma demonstração prática de Arquitetura de Software com foco em **Resiliência e Sistemas Distribuídos**. O objetivo é simular um sistema de votação que continua funcionando de forma íntegra mesmo sob cenários de falhas catastróficas (latência extrema de rede, indisponibilidade do banco de dados, picos de tráfego e queda de instâncias).

O projeto utiliza **Java 21, Spring Boot, PostgreSQL, Kubernetes (Minikube)** e **Chaos Mesh** para injeção de falhas.

---

## 🛠️ Tecnologias e Técnicas de Resiliência Implementadas

Abaixo estão detalhadas todas as técnicas implementadas para garantir a alta disponibilidade do sistema e por que elas foram necessárias.

### 1. Circuit Breaker (Resilience4j)
- **Onde:** `VotacaoController.java` e `application.properties`
- **Por que:** Quando o banco de dados fica extremamente lento ou inacessível, as requisições normais ficariam travadas esperando uma resposta, causando efeito cascata e derrubando toda a API.
- **Como funciona:** O Circuit Breaker monitora as falhas e lentidões. Se 50% das chamadas demorarem mais de 2 segundos (`SlowCallDurationThreshold`), ele "abre o circuito". Com o circuito aberto, a API para de tentar acessar o banco de dados e retorna o status `503 (Service Unavailable)` instantaneamente. Isso protege o banco de ser sobrecarregado enquanto ele tenta se recuperar.

### 2. TimeLimiter (Timeout Dinâmico)
- **Onde:** `VotacaoController.java` (no método GET de leitura)
- **Por que:** Evitar que o usuário fique esperando para sempre se o banco de dados travar.
- **Como funciona:** Ele impõe um limite estrito de 3 segundos para ler o placar. Se passar desse tempo, ele aborta a requisição e engatilha o Fallback. *(Nota: Essa técnica foi propositalmente removida das operações de gravação (POST) para evitar o problema clássico de "Escrita Fantasma", onde a API retorna timeout para o usuário, mas a Thread paralela acaba gravando no banco secretamente depois).*

### 3. Fallback (Degradação Graciosa)
- **Onde:** `VotacaoController.java`
- **Por que:** É melhor mostrar uma interface reduzida ou um aviso amigável do que uma tela de erro em branco.
- **Como funciona:** 
  - Na leitura (GET), se o banco estiver fora, a API retorna uma opção artificial dizendo `"Sistema Indisponível (Modo Resiliência)"`, mantendo a interface funcionando.
  - Na escrita (POST), retorna erro 503 com mensagem clara para o Frontend, que exibe o aviso para o usuário de forma amigável e impede spam de cliques corrompidos.

### 4. Execução Assíncrona e Pool de Threads Dedicado
- **Onde:** `VotacaoController.java` (`Executors.newFixedThreadPool(20)`)
- **Por que:** Em ambientes conteinerizados no Kubernetes, a limitação de CPU (ex: 1 Core) faz com que o Java limite os processamentos assíncronos a 1 Thread no Pool comum. Isso criava um funil onde todas as requisições ficavam na fila.
- **Como funciona:** Criamos um pool com 20 threads nativas dedicadas apenas às transações de banco. Assim, o sistema operacional do Linux escala as concorrências (Context Switch) em alta velocidade, baixando a latência basal de ~500ms para a casa dos ~20ms.

### 5. Kubernetes Probes (Startup e Readiness)
- **Onde:** `k8k/api.yaml`
- **Por que:** Quando um Pod (instância da API) nasce, o Spring Boot demora alguns segundos para carregar. Se o tráfego for enviado antes dele estar pronto, usuários recebem erro. Se o Kubernetes testar se ele está vivo muito cedo, ele mata o processo e a API nunca liga.
- **Como funciona:** A `startupProbe` dá ao Spring Boot até 150 segundos de "imunidade" para ele carregar no seu tempo, ideal para ambientes de dev limitados. Após carregar, a `readinessProbe` avisa ao balanceador de carga que esse pod já pode receber usuários.

### 6. Autoescalonamento Horizontal (HPA)
- **Onde:** `k8k/hpa.yaml`
- **Por que:** Necessário para aguentar picos repentinos de acessos.
- **Como funciona:** O HPA monitora o consumo de CPU. Se passar de 50%, ele clona a API, podendo criar de 3 até 10 instâncias simultâneas dividindo o tráfego.

### 7. Isolamento de Falhas (Backend x Frontend)
- O sistema é dividido entre o Front-end em Nginx puro (HTML/JS) e a API em Java. Se a API e o banco de dados morrerem completamente, o Frontend continua renderizando no navegador do usuário e exibindo as mensagens do Fallback, garantindo que o usuário nunca veja o site "cair".

---

## 🚀 Como Rodar o Projeto (Instruções para Avaliação)

Para facilitar a avaliação deste projeto em outros computadores, **as builds compiladas do Java (arquivos `.jar`) já estão inclusas no repositório GitHub**. Isso significa que você **não precisa ter o Java, JDK ou Gradle instalados na sua máquina para compilar o código**. O Docker criará a imagem diretamente a partir da build que já subimos, economizando dezenas de minutos de compilação.

### 📋 Pré-requisitos
Tudo o que você precisa ter instalado no seu computador é:
1. [Docker Desktop](https://www.docker.com/products/docker-desktop/) rodando.
2. [Minikube](https://minikube.sigs.k8s.io/docs/start/)
3. [kubectl](https://kubernetes.io/docs/tasks/tools/)
4. [Helm](https://helm.sh/docs/intro/install/) (Opcional, para instalar o Chaos Mesh)
5. `Make` (Nativo no Linux/Mac. No Windows, pode usar via Git Bash, ou o Chocolatey: `choco install make`).

### ⚙️ Subindo o Sistema

1. Abra o terminal na raiz do projeto.
2. Inicie o cluster Minikube:
   ```bash
   minikube start --driver=docker
   ```
3. Rode o comando mágico que fará o deploy automático de toda a arquitetura no Kubernetes:
   ```bash
   make run
   ```
   *(Este comando usará a imagem Docker embutida na pasta, aplicará as configurações da API, Banco de Dados, Frontend, HPA e instalará o Chaos Mesh).*

4. Verifique se os Pods subiram e estão prontos (`1/1 Ready`):
   ```bash
   kubectl get pods -w
   ```
   *(Aguarde até os pods da API ficarem `Ready`)*

5. Exponha o sistema para poder acessá-lo pelo seu navegador:
   ```bash
   make service
   ```

### 🌪️ Testando a Resiliência (Experimentos de Caos)

O projeto usa o **Chaos Mesh** para provar que a arquitetura funciona sob estresse e falhas.
Você pode rodar os seguintes experimentos usando os atalhos do `Makefile`:

- **Derrubar Pods Aleatoriamente:**
  ```bash
  make start-pod-chaos
  ```
  *Observação: o Chaos vai matar uma instância da API. Você notará que o sistema continua funcionando (Alta Disponibilidade), pois as outras instâncias assumem imediatamente.*

- **Injetar Latência Extrema no Banco de Dados:**
  ```bash
  make start-network-chaos
  ```
  *Observação: O acesso ao banco vai demorar 500ms a mais. O Circuit Breaker vai detectar as chamadas lentas e vai agir para não engarrafar a rede, retornando uma mensagem de instabilidade pro Frontend.*

- **Saturar a CPU (Stress Test):**
  ```bash
  make start-stress-chaos
  ```
  *Observação: A CPU vai fritar em 100%. Você poderá ver o HPA criando novas instâncias da API para aguentar o tranco (Use `kubectl get hpa -w` para assistir).*

Para parar qualquer experimento, use os comandos equivalentes de `stop`:
```bash
make stop-pod-chaos
make stop-network-chaos
make stop-stress-chaos
```

---
