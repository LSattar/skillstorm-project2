import { defineConfig } from 'vite';

export default defineConfig({
  optimizeDeps: {
    include: ['zone.js'],
    exclude: [],
  },
});

