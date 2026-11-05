// Structural, OpenAPI route and script checks. Does not send mutating requests.
const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');
const assert = require('node:assert/strict');

async function main() {
  const doc = JSON.parse(fs.readFileSync(path.join(__dirname, 'helpdesk-insomnia.json'), 'utf8'));
  assert.equal(doc.__export_format, 4);
  const resources = doc.resources;
  const ids = new Set(resources.map(x => x._id));
  assert.equal(ids.size, resources.length);
  for (const r of resources) if (r.parentId) assert(ids.has(r.parentId), r.name);
  const env = {...resources.find(x => x._type === 'environment' && x.name === 'Local').data};
  const reqs = resources.filter(x => x._type === 'request');
  for (const r of reqs) {
    for (const match of JSON.stringify(r).matchAll(/\{\{ _\.(\w+) \}\}/g)) assert(match[1] in env, match[1]);
    for (const key of ['preRequestScript', 'afterResponseScript']) new vm.Script(r[key]);
    if (r.body.text) JSON.parse(r.body.text.replace(/\{\{ _\.(\w+) \}\}/g, (_, k) => String(env[k])));
  }
  for (const role of ['admin', 'jefatura', 'tecnico', 'solicitante']) {
    env['email_'+role] = role+'@example.test'; env['password_'+role] = 'Ficticia123!';
  }
  const api = {
    environment: {get: k => env[k], set: (k,v) => {env[k] = v;}},
    test: (_, f) => f(), expect: actual => ({to: {eql: expected => assert.deepEqual(actual,expected)}})
  };
  for (const role of ['admin', 'jefatura', 'tecnico', 'solicitante']) {
    const r = reqs.find(x => x.name === 'Login '+role);
    vm.runInNewContext(r.preRequestScript, {insomnia:api});
    api.response = {code:200, json:() => ({rol:'rol_'+role,token:'token-'+role,usuarioId:7})};
    vm.runInNewContext(r.afterResponseScript, {insomnia:api});
  }
  for (const role of ['admin', 'jefatura', 'tecnico', 'solicitante']) assert.equal(env['token_'+role], 'token-'+role);
  const worker = reqs.find(x => x.name === 'Login solicitante');
  vm.runInNewContext(worker.preRequestScript, {insomnia:api});
  api.response = {code:200, json:() => ({rol:'rol_admin',token:'incorrecto'})};
  assert.throws(() => vm.runInNewContext(worker.afterResponseScript, {insomnia:api}));
  assert.equal(env.token_solicitante, ''); assert.equal(env.token_admin, 'token-admin');
  const deletion = reqs.find(x => x.method === 'DELETE');
  assert.throws(() => vm.runInNewContext(deletion.preRequestScript, {insomnia:api}));
  const spec = await fetch(process.argv[2] || 'http://localhost:8080/v3/api-docs').then(r => {
    assert.equal(r.status,200); return r.json();
  });
  const covered = new Set();
  for (const r of reqs) {
    const route = '/api'+r.url.replace('{{ _.base_url }}','').replace(/\{\{ _\.\w+ \}\}/g,'{id}');
    const operation = spec.paths[route]?.[r.method.toLowerCase()];
    assert(operation, r.method+' '+route);
    covered.add(r.method+' '+route);
  }
  for (const [route, ops] of Object.entries(spec.paths)) for (const method of Object.keys(ops))
    if (['get','post','put','patch','delete'].includes(method)) assert(covered.has(method.toUpperCase()+' '+route), 'Missing '+method+' '+route);
  console.log(`${reqs.length} solicitudes válidas; ${covered.size} operaciones OpenAPI cubiertas. Scripts: tokens separados, rol verificado y eliminación protegida.`);
}
main().catch(e => {console.error(e.message); process.exitCode=1;});
