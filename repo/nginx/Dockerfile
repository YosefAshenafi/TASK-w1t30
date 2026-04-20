FROM nginx:1.25-alpine
RUN apk add --no-cache curl openssl && \
    mkdir -p /etc/nginx/ssl && \
    openssl req -x509 -nodes -days 3650 -newkey rsa:2048 \
      -keyout /etc/nginx/ssl/meridian.key \
      -out /etc/nginx/ssl/meridian.crt \
      -subj "/C=US/ST=Local/L=Local/O=Meridian/CN=localhost" 2>/dev/null
COPY nginx.conf /etc/nginx/nginx.conf
COPY default.conf.template /etc/nginx/templates/default.conf.template
