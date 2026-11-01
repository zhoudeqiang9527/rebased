// Prepare the same pinned language server as the product build, for source runs and integration tests.
import {createHash} from 'node:crypto'
import {mkdir, readFile, writeFile} from 'node:fs/promises'
import {resolve} from 'node:path'
import {fileURLToPath} from 'node:url'
import {execFileSync} from 'node:child_process'

const root = fileURLToPath(new URL('../', import.meta.url))
const target = resolve(process.argv[2] ?? resolve(root, 'out/rebased-javascript'))
const manifest = await readFile(resolve(root, 'plugins/rebased.javascript/server-packages.properties'), 'utf8')
await mkdir(target, {recursive: true})
for (const line of manifest.split(/\r?\n/).filter(line => line && !line.startsWith('#'))) {
  const split = line.indexOf('=')
  const name = line.slice(0, split)
  const [version, integrity] = line.slice(split + 1).split(',')
  const response = await fetch(`https://registry.npmjs.org/${name}/-/${name}-${version}.tgz`)
  if (!response.ok) throw new Error(`Download failed: ${response.status} ${name}`)
  const bytes = Buffer.from(await response.arrayBuffer())
  if (createHash('sha512').update(bytes).digest('base64') !== integrity) throw new Error(`Checksum mismatch: ${name}`)
  const archive = resolve(target, `${name}-${version}.tgz`)
  await writeFile(archive, bytes)
  const packageDir = resolve(target, name)
  await mkdir(packageDir, {recursive: true})
  execFileSync('tar', ['-xzf', archive, '--strip-components=1', '-C', packageDir])
  console.log(`Prepared ${name} ${version}`)
}
console.log(`Server directory: ${target}`)
