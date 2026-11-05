"""Generate portable Insomnia v4 resources without credentials or real user IDs."""
from pathlib import Path
import json
from datetime import datetime, timezone

out = Path(__file__).resolve().parent
workspace = '__WORKSPACE_ID__'
base = '__BASE_ENVIRONMENT_ID__'
resources = [{'_id':workspace, '_type':'workspace', 'parentId':None, 'name':'Helpdesk Morales API',
              'description':'API completa. Completar credenciales en Local. Seguir docs/API.md. No ejecutar toda la colección contra datos reales.', 'scope':'collection'},
             {'_id':base,'_type':'environment','parentId':workspace,'name':'Base Environment','data':{}}]
env = {'base_url':'http://localhost:8080/api','usuario_id':0,'ticket_id':0,'tecnico_id':0,'notificacion_id':0,
       'area_id':0,'categoria_id':0,'prioridad_id':0,'estado_id':0,
       'usuario_email_prueba':'insomnia.prueba@example.test','usuario_password_prueba':'', 'confirmar_eliminacion':False}
for role in ['admin','jefatura','tecnico','solicitante']:
    for key in ['email','password','token']:
        env[f'{key}_{role}']=''
resources.append({'_id':'__env_local__','_type':'environment','parentId':base,'name':'Local','data':env})
def var(k): return '{{ _.%s }}' % k
def folder(key, name, description=''):
    fid='__folder_'+key+'__'
    resources.append({'_id':fid,'_type':'request_group','parentId':workspace,'name':name,'description':description,'environment':{},'metaSortKey':len(resources)})
    return fid
requests=[]
def req(parent,name,method,path,role='admin',body=None,code=200,save=None,description='',before=''):
    script=f"insomnia.test('HTTP {code}', () => insomnia.expect(insomnia.response.code).to.eql({code}));\n"
    if save:
        script+=f'if (insomnia.response.code === {code}) {{\n  const data = insomnia.response.json();\n'
        for key,expression in save.items(): script+=f'  insomnia.environment.set({json.dumps(key)}, {expression});\n'
        script+='}\n'
    r={'_id':f'__request_{len(requests)+1}__','_type':'request','parentId':parent,'name':name,
       'method':method,'url':var('base_url')+path,'parameters':[],
       'headers':[{'name':'Accept','value':'application/json'}],
       'authentication':{'type':'bearer','token':var('token_'+role)} if role else {},
       'body':{}, 'description':description+f'\nRespuesta esperada: HTTP {code}.',
       'metaSortKey':len(requests),'preRequestScript':before,'afterResponseScript':script,
       'settingSendCookies':False,'settingStoreCookies':False}
    if body is not None:
        content=json.dumps(body,ensure_ascii=False,indent=2)
        # Numeric variables must render as JSON numbers, not strings.
        for k in ['area_id','categoria_id','prioridad_id','estado_id','tecnico_id']:
            content=content.replace('"'+var(k)+'"',var(k))
        r['body']={'mimeType':'application/json','text':content}
        r['headers'].append({'name':'Content-Type','value':'application/json'})
    if role:
        r['preRequestScript']=f"if (!insomnia.environment.get('token_{role}')) throw new Error('Ejecuta primero Login {role} en el entorno Local');\n"+before
    requests.append(r);resources.append(r)

f=folder('auth','01 · Autenticación','Los tokens se guardan por rol; no se reemplazan entre sí.')
for role in ['admin','jefatura','tecnico','solicitante']:
    save={'token_'+role:'data.token'}
    if role=='tecnico': save['tecnico_id']='data.usuarioId'
    req(f,'Login '+role,'POST','/auth/login',None,{'email':var('email_'+role),'password':var('password_'+role)},save=save,
        before=f"insomnia.environment.set('token_{role}', '');\nif (!insomnia.environment.get('email_{role}') || !insomnia.environment.get('password_{role}')) throw new Error('Completa email_{role} y password_{role} en Local');",
        description='Introduce las credenciales actuales de una cuenta activa del rol indicado. Token, tipo, usuarioId, nombreCompleto, rol y expiraEnMs.')
    requests[-1]['afterResponseScript'] = requests[-1]['afterResponseScript'].replace(
        '  const data = insomnia.response.json();',
        f"  const data = insomnia.response.json();\n  if (data.rol !== 'rol_{role}') throw new Error('Esta cuenta no pertenece al rol {role}; revisa sus credenciales');")
f=folder('catalogs','02 · Catálogos')
for route,key,preferred in [('areas','area_id',None),('categorias','categoria_id',None),('prioridades','prioridad_id','alta'),('estados','estado_id','en_progreso')]:
    expression="(data.find(x => x.nombre === '%s') || data[0])?.id || 0" % preferred if preferred else 'data[0]?.id || 0'
    req(f,'Consultar '+route,'GET','/catalogos/'+route,'solicitante',save={key:expression},description='Lista de {id, nombre}. Guarda un ID válido en Local; puedes cambiarlo según la prueba.')
f=folder('users','03 · Usuarios','Solo ADMIN modifica; JEFATURA puede consultar. Usar la cuenta ficticia creada aquí.')
req(f,'Listar usuarios · admin','GET','/usuarios')
req(f,'Listar usuarios · jefatura','GET','/usuarios','jefatura')
req(f,'Técnicos activos','GET','/usuarios/tecnicos',save={'tecnico_id':'data[0]?.id || 0'},description='Elige un técnico activo. La variable también se guarda al iniciar sesión como técnico; debe coincidir con la cuenta usada en las pruebas de atención.')
payload={'nombres':'Usuario','apellidos':'Prueba Insomnia','email':var('usuario_email_prueba'),'password':var('usuario_password_prueba'),'rol':'rol_solicitante','areaId':var('area_id')}
req(f,'Crear usuario de prueba','POST','/usuarios',body=payload,code=201,save={'usuario_id':'data.id'},
    description='Cuenta activa por defecto. Rol: rol_admin, rol_jefatura, rol_tecnico o rol_solicitante. Contraseña mínima 6 caracteres. No devuelve token ni hash.',
    before="if (!insomnia.environment.get('usuario_password_prueba')) throw new Error('Completa usuario_password_prueba antes de crear la cuenta');")
req(f,'Consultar usuario por ID','GET','/usuarios/'+var('usuario_id'))
req(f,'Editar usuario de prueba','PUT','/usuarios/'+var('usuario_id'),body={**payload,'nombres':'Usuario editado','password':None},description='Contraseña null o vacía conserva la actual. areaId null elimina el área. El estado se cambia mediante PATCH separado.')
for state in ['inactivo','activo']:
    req(f,'Cambiar estado a '+state,'PATCH','/usuarios/'+var('usuario_id')+'/estado',body={'estado':state},description='Solo acepta activo/inactivo. No permite desactivar la cuenta autenticada.')
f=folder('tickets','04 · Tickets')
req(f,'Crear ticket · solicitante','POST','/tickets','solicitante',{'asunto':'Prueba de conexión desde Insomnia','descripcion':'Ticket ficticio para probar el flujo completo de atención.','categoriaId':var('categoria_id')},201,{'ticket_id':'data.id'},description='La identidad proviene del token. La prioridad la sugiere el backend y la asignación depende de ajustes.')
for role in ['solicitante','tecnico','jefatura','admin']:
    req(f,'Listar tickets · '+role,'GET','/tickets',role,description='Solicitante: propios. Técnico: asignados o propios. Administrador y jefatura: todos.')
req(f,'Consultar detalle · solicitante','GET','/tickets/'+var('ticket_id'),'solicitante')
req(f,'Asignar técnico · jefatura','PATCH','/tickets/'+var('ticket_id')+'/asignar','jefatura',{'tecnicoId':var('tecnico_id')},description='Debe ser un técnico activo. Usa el mismo ID del login técnico para probar la atención.')
req(f,'Cambiar prioridad · admin','PATCH','/tickets/'+var('ticket_id')+'/prioridad',body={'prioridadId':var('prioridad_id')})
req(f,'Cambiar estado · técnico','PATCH','/tickets/'+var('ticket_id')+'/estado','tecnico',{'estadoId':var('estado_id')},description='Primero en_progreso desde abierto/en_espera; luego resuelto desde en_progreso. Selecciona el ID en el catálogo de estados.')
req(f,'Cambiar estado · jefatura','PATCH','/tickets/'+var('ticket_id')+'/estado','jefatura',{'estadoId':var('estado_id')},description='Puede cerrar o reabrir. Configura estado_id con el catálogo de estados.')
f=folder('comments','05 · Comentarios')
for role in ['solicitante','admin','tecnico']:
    req(f,'Comentar · '+role,'POST','/tickets/'+var('ticket_id')+'/comentarios',role,{'contenido':'Mensaje de prueba enviado como '+role},201,description='Máximo 2000 caracteres. El autor se obtiene del token, nunca de un ID enviado en el cuerpo.')
req(f,'Consultar seguimiento','GET','/tickets/'+var('ticket_id')+'/comentarios','solicitante',description='Lista con id, contenido, autor y fechaCreacion.')
f=folder('notifications','06 · Notificaciones')
req(f,'Mi bandeja · solicitante','GET','/notificaciones','solicitante',save={'notificacion_id':'data.items[0]?.id || 0'},description='Devuelve items (últimas 50) y noLeidas (total pendiente). Solo de la cuenta autenticada.')
req(f,'Marcar una como leída','PATCH','/notificaciones/'+var('notificacion_id')+'/leida','solicitante')
req(f,'Marcar todas como leídas','POST','/notificaciones/leer-todas','solicitante')
f=folder('settings','07 · Configuración')
req(f,'Consultar ajustes','GET','/ajustes')
req(f,'Actualizar asignación automática','PUT','/ajustes',body={'asignacionAutomatica':True},description='Solo ADMIN. Guarda la preferencia en PostgreSQL. Afecta tickets nuevos.')
f=folder('negative','08 · Comprobaciones de permisos','Estas solicitudes deben fallar con el código indicado y no modificar datos.')
req(f,'Sin token → 401','GET','/tickets',None,code=401)
req(f,'Solicitante consulta usuarios → 403','GET','/usuarios','solicitante',code=403)
req(f,'Registro público bloqueado → 401','POST','/auth/registro',None,body=payload,code=401)
req(f,'Estado inválido → 400','PATCH','/usuarios/'+var('usuario_id')+'/estado',body={'estado':'bloqueado'},code=400)
f=folder('delete','99 · Eliminación MANUAL','Fuera del recorrido habitual. Configurar confirmar_eliminacion=true solo para eliminar la cuenta ficticia creada aquí.')
req(f,'ELIMINAR usuario de prueba','DELETE','/usuarios/'+var('usuario_id'),code=204,
    before="if (insomnia.environment.get('confirmar_eliminacion') !== true) throw new Error('Eliminación deshabilitada. Revisa usuario_id y activa confirmar_eliminacion solo para la cuenta de prueba');",
    description='Sin relaciones: elimina. Con tickets/comentarios/historial/notificaciones: desactiva y conserva el historial. Ambos casos devuelven 204; GET posterior permite distinguirlos.')

document={'_type':'export','__export_format':4,'__export_date':datetime.now(timezone.utc).isoformat(),'__export_source':'helpdesk-api','resources':resources}
(out/'helpdesk-insomnia.json').write_text(json.dumps(document,ensure_ascii=False,indent=2),encoding='utf-8')
print(f'Generadas {len(requests)} solicitudes')
