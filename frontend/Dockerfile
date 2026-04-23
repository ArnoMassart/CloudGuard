FROM nginx:alpine
# Verwijder standaard bestanden
RUN rm -rf /usr/share/nginx/html/*
# Kopieer de inhoud van de dist map (die we via SCP hebben gestuurd)
COPY dist /usr/share/nginx/html
EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
