import { spawnSync } from 'node:child_process';
import { fileURLToPath } from 'node:url';

/**
 * Run one reviewed Maven gate through the Windows wrapper.
 * @param {string} gate Either test or package; no arbitrary shell input is accepted.
 * @returns {number} Maven's exit status, or 1 if the process could not start.
 */
function runGate(gate) {
  const argumentsByGate = { test: ['test'], package: ['clean', 'package'] };
  const args = argumentsByGate[gate];
  if (!args) throw new Error('Expected gate: test or package');
  const root = fileURLToPath(new URL('../', import.meta.url));
  const result = spawnSync('cmd.exe', ['/d', '/c', 'mvnw.cmd', ...args], {
    cwd: root,
    stdio: 'inherit',
    windowsHide: true,
  });
  if (result.error) throw result.error;
  return result.status ?? 1;
}

process.exitCode = runGate(process.argv[2]);
