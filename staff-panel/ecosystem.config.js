// PM2 process file — run with: pm2 start ecosystem.config.js
// Install PM2: npm install -g pm2
// Start:       pm2 start ecosystem.config.js
// Save:        pm2 save && pm2 startup  (auto-restart on reboot)
module.exports = {
  apps: [
    {
      name: 'staff-panel',
      script: 'node_modules/.bin/next',
      args: 'start -p 3000',
      cwd: __dirname,
      env: {
        NODE_ENV: 'production',
      },
      env_file: '.env.local',
    },
  ],
}
