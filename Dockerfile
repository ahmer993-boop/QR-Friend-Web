# Production Dockerfile for QR Friend Web Dashboard & API Server
FROM node:20-alpine

WORKDIR /app

# Copy dependency specifications
COPY package*.json ./

# Install production dependencies
RUN npm install --production || true

# Copy all project files (frontend dashboard + backend server)
COPY . .

# Set environment
ENV NODE_ENV=production
ENV PORT=3000

# Expose default application port
EXPOSE 3000

# Start server
CMD ["node", "server.js"]
