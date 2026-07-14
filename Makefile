.PHONY: all build apply chaos-mesh run start-network-chaos stop-network-chaos start-pod-chaos stop-pod-chaos start-stress-chaos stop-stress-chaos port-forward service

# Comando principal que faz tudo
run: build apply chaos-mesh
	@echo "Deploy finalizado com sucesso! Use 'kubectl get pods' para ver o status."

# Faz o build das imagens diretamente dentro do Minikube (evita problemas de variáveis de ambiente no Windows)
build:
	@echo "Fazendo build da API direto no Minikube..."
	minikube image build -t api-votacao:latest .
	@echo "Fazendo build do Frontend direto no Minikube..."
	minikube image build -t frontend-votacao:latest ./sd-front

# Aplica os arquivos YAML no cluster
apply:
	@echo "Aplicando os manifests do Kubernetes..."
	kubectl apply -f k8k/postgres.yaml
	kubectl apply -f k8k/api.yaml
	kubectl apply -f k8k/hpa.yaml
	kubectl apply -f k8k/frontend.yaml
	@echo "Forçando os pods a usarem a imagem mais recente..."
	kubectl rollout restart deployment/api-votacao
	kubectl rollout restart deployment/frontend-votacao

# Instala o Chaos Mesh usando o Helm
chaos-mesh:
	@echo "Instalando Chaos Mesh via Helm..."
	helm repo add chaos-mesh https://charts.chaos-mesh.org
	helm repo update
	helm upgrade --install chaos-mesh chaos-mesh/chaos-mesh -n=chaos-mesh --create-namespace --version 2.8.3 --set chaosDaemon.runtime=docker --set chaosDaemon.socketPath=/var/run/docker.sock


# --- Comandos práticos para rodar os experimentos ---

start-network-chaos:
	@echo "Iniciando NetworkChaos (Lentidão Infinita no Banco de Dados)..."
	kubectl apply -f chaos-experiments/network-chaos.yaml

stop-network-chaos:
	@echo "Parando NetworkChaos..."
	kubectl delete -f chaos-experiments/network-chaos.yaml --ignore-not-found

start-pod-chaos:
	@echo "Iniciando PodChaos (Mata 1 Pod a cada 30 segundos)..."
	kubectl apply -f chaos-experiments/pod-chaos.yaml

stop-pod-chaos:
	@echo "Parando PodChaos..."
	kubectl delete -f chaos-experiments/pod-chaos.yaml --ignore-not-found

start-stress-chaos:
	@echo "Iniciando StressChaos (100% de CPU constante na API)..."
	kubectl apply -f chaos-experiments/stress-chaos.yaml

stop-stress-chaos:
	@echo "Parando StressChaos..."
	kubectl delete -f chaos-experiments/stress-chaos.yaml --ignore-not-found

port-forward:
	@echo "Abrindo túnel para a API na porta 8080 (cuidado: túneis quebram com instabilidade de rede)..."
	kubectl port-forward service/api-service 8080:8080

service:
	@echo "Abrindo o Site no navegador (via Frontend Service)..."
	minikube service frontend-service

