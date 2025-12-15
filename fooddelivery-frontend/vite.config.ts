import { defineConfig } from 'vite';

export default defineConfig({
  server: {
    allowedHosts: [
      'unperturbedly-intermesenteric-lourie.ngrok-free.dev'
    ],
    host: true,
    port: 4200
  }
});
