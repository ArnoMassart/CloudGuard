# We just need a web server now, no Node.js required!
FROM nginx:alpine

# Copy the dist folder that GitLab sent over into Nginx's public directory
COPY dist /usr/share/nginx/html

# Expose port 80
EXPOSE 80

# Start Nginx
CMD ["nginx", "-g", "daemon off;"]
