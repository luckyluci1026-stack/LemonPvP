/** @type {import('next').NextConfig} */
const nextConfig = {
  experimental: {
    serverComponentsExternalPackages: ['better-sqlite3'],
  },
  env: {
    JWT_SECRET: process.env.JWT_SECRET || 'lemon-staff-panel-secret-key-change-in-production',
  },
}

module.exports = nextConfig
