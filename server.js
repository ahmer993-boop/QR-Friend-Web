// Example fix for getApiBaseUrl to ensure it points to your actual backend
function getApiBaseUrl() {
  const isLocal = window.location.hostname === 'localhost' || window.location.hostname === '127.0.0.1';
  // Point to your production API server, NOT the GitHub Pages URL
  return isLocal ? 'http://localhost:3000' : 'https://your-actual-api-server.com';
}

// If you are fetching static files from the repo, ensure you include the extension
// fetch(`${apiBase}/api/visits.json`, { headers }) /**
 * Root Server Entry Point
 * Runs the Web Monitoring Dashboard HTTP server on port 3000
 */
require('./backend/server.js');
