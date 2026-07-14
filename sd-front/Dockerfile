# Puxa um servidor web Nginx bem leve
FROM nginx:alpine

# Copia o seu arquivo HTML para a pasta pública do servidor
COPY index.html /usr/share/nginx/html/index.html

# Copia as configurações de proxy reverso
COPY default.conf /etc/nginx/conf.d/default.conf


# A porta padrão do Nginx é a 80
EXPOSE 80