# [PLAN-002] Transporte HTTP de demostración y evidencia

## Evidencia y arquitectura
pom.xml usa starter MCP Stdio 2.0.1, Boot 4.1.2 y JPA. application.properties desactiva web; BankService expone cinco herramientas. La guía PDF páginas 10 a 12 pide starter webmvc y protocolo STREAMABLE. Claude abierto usa Local/Claude-Data/claude_pzs8sxrjxfjjc y no el archivo Roaming revisado inicialmente.

## Cambio propuesto
Sustituir el artifactId del starter por propiedad Maven. Valor predeterminado spring-ai-starter-mcp-server; perfil Maven http selecciona spring-ai-starter-mcp-server-webmvc. application-http.properties activa servlet, desactiva Stdio, fija STREAMABLE y loopback:8080/mcp, base data/bankdb-http. Mantener reglas y Java intactos. Crear colección Postman reproducible, pruebas de protocolo y reporte con capturas.

## Contratos y compatibilidad
Stdio permanece predeterminado. HTTP usa el mismo JSON-RPC y herramientas; inicialización, notificación initialized, tools/list y tools/call. Mantener versiones existentes autorizadas. No contratos activos según CLI contracts --active. Bases independientes, sin migraciones.

## Observabilidad
Logs target/mcp-bank-http.log y target/mcp-bank-claude.log. Evidencia JSON de respuestas, códigos HTTP y capturas UI en output/evidencias. No copiar configuración privada al informe.

## Tecnología y documentación
Fuente oficial consultada 2026-11-14: https://docs.spring.io/spring-ai/reference/api/mcp/mcp-server-boot-starter-docs.html tabla WebMVC y sección Server Protocols. Confirma starter webmvc y STREAMABLE; la página vigente corresponde a 2.0.2, por lo que compatibilidad con 2.0.1 se comprobará en ejecución. Se conserva BOM 2.0.1. Dependencia de transporte autorizada por petición expresa de probar como guía. Javadoc y Conventional Commits se conservan; no se harán commits.

## UI e interacción
N/A diseño de interfaz propia. Se revisarán interfaces reales de Claude y Postman y capturas finales legibles. Solicitar al usuario aprobación de permisos que computer-use no puede atender.

## Verificación
AC-01: 11 pruebas sobre H2 en memoria y handshake Stdio. AC-02: empaquetado HTTP y pruebas JSON-RPC locales. AC-03 y AC-04: observación manual con capturas auténticas. AC-05: renderizar e inspeccionar todas las páginas del PDF. No confundir audit documental con ejecución.

## Rollback / recovery
Los perfiles son optativos. Reconstruir sin -Phttp restaura JAR Stdio. No borrar bases ni alterar datos previos. Respaldar configuración de Claude antes de agregar servidor. Al finalizar dejar JAR predeterminado para Claude y JAR HTTP separado para reproducción.


Ajuste ejecutado: perfil HTTP genera target/mcp-bank-tdea-http-0.0.1-SNAPSHOT.jar para no sobrescribir el JAR de Claude. Prueba final de persistencia usa base bankdb en modo de archivo de solo lectura, con Claude cerrado. Postman ejecuta consultarSaldo y listarMovimientos; las cinco herramientas y escrituras se prueban desde Claude. Sin cambios en lógica bancaria.


Para proteger las bases durante la verificación final se agrega configuración H2 en memoria exclusiva de src/test/resources/application.properties. Se actualiza el ejemplo de Claude con rutas de datos y logs estables y se agrega README de reproducción. No hay cambio de APIs ni de reglas bancarias.

