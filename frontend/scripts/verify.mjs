import { spawnSync } from 'node:child_process';

const npmCli = process.env.npm_execpath;
if (!npmCli) {
  console.error('Nie można odnaleźć npm CLI. Uruchom ten skrypt przez `npm run verify`.');
  process.exit(1);
}

const checks = [
  ['test', 'frontend tests'],
  ['lint', 'frontend lint'],
  ['build', 'frontend build'],
];

let failed = false;

for (const [script, label] of checks) {
  console.log(`\n=== ${label}: npm run ${script} ===`);
  const result = spawnSync(process.execPath, [npmCli, 'run', script], { stdio: 'inherit' });
  const succeeded = result.status === 0;
  console.log(`ROUTECOST_VERIFY_${script.toUpperCase()}=${succeeded ? 'SUCCESS' : 'FAILED'}`);

  if (!succeeded) {
    failed = true;
    if (result.error) {
      console.error(result.error.message);
    }
  }
}

process.exitCode = failed ? 1 : 0;
