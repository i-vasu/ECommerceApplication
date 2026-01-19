import { defineConfig, devices } from '@playwright/test';

export default defineConfig({
    testDir: './specs',
    timeout: 30000,
    expect: {
        timeout: 5000
    },
    fullyParallel: true,
    forbidOnly: !!process.env.CI,
    retries: process.env.CI ? 2 : 0,
    workers: process.env.CI ? 1 : undefined,
    reporter: 'html',
    use: {
        baseURL: 'http://localhost:8080', // Using API Gateway Port 8080
        trace: 'on-first-retry',
    },
    projects: [
        {
            name: 'api',
            testMatch: /.*\.spec\.ts/,
        },
    ],
});
