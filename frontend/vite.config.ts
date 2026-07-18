import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import tailwindcss from '@tailwindcss/vite'
import path from 'path'
import fs from 'fs'

export default defineConfig({
  plugins: [
    react(),
    tailwindcss(),
    {
      name: 'clean-stale-assets',
      closeBundle() {
        const outDir = path.resolve(__dirname, '../backend/public/assets')
        const indexPath = path.resolve(__dirname, '../backend/public/index.html')
        if (!fs.existsSync(indexPath)) return

        const html = fs.readFileSync(indexPath, 'utf-8')
        const hashes = new Set<string>()
        for (const m of html.matchAll(/\/(assets\/[^"']+\.(?:js|css))/g)) {
          hashes.add(path.basename(m[1]))
        }

        if (fs.existsSync(outDir)) {
          for (const file of fs.readdirSync(outDir)) {
            if (file.endsWith('.js') || file.endsWith('.css')) {
              if (!hashes.has(file)) {
                fs.unlinkSync(path.join(outDir, file))
              }
            }
          }
        }
      },
    },
  ],
  base: './',
  publicDir: 'public',
  build: {
    outDir: '../backend/public',
    emptyOutDir: false,
    sourcemap: false,
  },
  resolve: {
    alias: { '@': path.resolve(__dirname, './src') },
  },
  server: {
    proxy: {
      '/api': 'http://localhost:8080',
    },
  },
})
