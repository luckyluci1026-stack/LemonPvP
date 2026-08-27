import type { Config } from 'tailwindcss'

const config: Config = {
  content: [
    './src/pages/**/*.{js,ts,jsx,tsx,mdx}',
    './src/components/**/*.{js,ts,jsx,tsx,mdx}',
    './src/app/**/*.{js,ts,jsx,tsx,mdx}',
  ],
  theme: {
    extend: {
      colors: {
        lemon: {
          50:  '#fffef0',
          100: '#fffce0',
          200: '#fff8b3',
          300: '#fff280',
          400: '#ffe94d',
          500: '#FFD700',
          600: '#e6b800',
          700: '#cc9900',
          800: '#a37700',
          900: '#7a5900',
        },
        dark: {
          900: '#0a0a0f',
          800: '#12121a',
          700: '#1a1a2e',
          600: '#22223b',
          500: '#2d2d4a',
          400: '#3a3a5c',
        },
      },
      fontFamily: {
        sans: ['Inter', 'system-ui', 'sans-serif'],
        mono: ['JetBrains Mono', 'Fira Code', 'monospace'],
      },
    },
  },
  plugins: [],
}

export default config
