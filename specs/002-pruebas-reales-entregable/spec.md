# [SPEC-002] Pruebas reales y entrega del servidor MCP bancario

Fecha: 2026-11-14. Tipo: feature.

## 0. Clarification and Assumptions
El usuario autoriza operar Claude Desktop y Postman, tomar capturas y preparar el informe del laboratorio. Limitar consultas a Claude por su plan gratuito. Mantener versiones actuales si funcionan. Trabajo local, sin publicación ni commits. Fuente: solicitud del usuario de esta sesión; guía PDF páginas 10 a 12 y 17.

## 1. Necesidad y alcance
Demostrar las cinco herramientas bancarias desde Claude por Stdio; desde Postman verificar conexión, catálogo, consultas y persistencia por Streamable HTTP. Las escrituras se ejecutan una sola vez desde Claude. Añadir exclusivamente la configuración de transporte necesaria, conservando el comportamiento bancario. Generar informe PDF con evidencia auténtica y material reproducible.

## 2. Acceptance Criteria and Non-Goals
- AC-01 [functional]: WHEN se construya sin perfil HTTP, el sistema SHALL conservar el servidor Stdio y las cinco herramientas existentes.
- AC-02 [functional]: WHEN se active el perfil Maven y Spring http, el sistema SHALL exponer MCP Streamable HTTP en 127.0.0.1:8080/mcp con una base H2 de demostración separada.
- AC-03 [interaction]: WHEN se pruebe desde Claude Desktop, la evidencia SHALL registrar invocaciones reales y sus resultados con una solicitud agrupada, sujeto a la cuota disponible y permisos aprobados por el usuario.
- AC-04 [interaction]: WHEN se pruebe desde Postman, la evidencia SHALL registrar conexión, descubrimiento y ejecución de herramientas MCP reales.
- AC-05 [functional]: WHEN se prepare la entrega, el informe SHALL distinguir resultados ejecutados de limitaciones, incluir capturas legibles y permitir reproducir las pruebas.

Non-Goals: cambios de reglas bancarias, nuevas versiones, despliegue público, autenticación remota, certificación financiera, compras de planes.

## 3. Aplicabilidad
Datos ficticios del laboratorio. HTTP limitado a loopback. N/A multiinquilino. Registros de movimientos existentes; no extraer secretos ni preferencias privadas al informe. Las dos bases separan demostraciones de Claude y Postman y evitan bloqueo de archivo H2.

## 4. Experiencia y límites
Sin UI propia. Se operan clientes existentes: estados desconectado, en ejecución, autorización y resultados. Capturas mediante computer-use. El usuario atiende permisos de herramientas. No prometer éxito si la cuota o un permiso impiden la prueba.

## 5. Trazabilidad
AC-01: build predeterminado y prueba MCP Stdio. AC-02: build HTTP y handshake. AC-03 y AC-04: capturas y transcripción observada. AC-05: inspección visual del PDF y revisión de afirmaciones.


## Ajuste de alcance observado
El 2026-11-14, después de responder Claude, el usuario delegó continuar con el entregable. Se optó por consultas de Postman sin nuevas escrituras. Después de cerrar Claude, HTTP abre secuencialmente la misma base con ACCESS_MODE_DATA=r y ddl-auto=validate para verificar persistencia. La base separada bankdb-http permanece como valor predeterminado del perfil. No se ejecutó la consignación de 20000 preparada y rechazada por revisión automática.

