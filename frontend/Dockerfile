FROM nginx:1.27-alpine
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY index.html login.html register.html profile.html post.html /usr/share/nginx/html/
COPY static /usr/share/nginx/html/static
EXPOSE 80
