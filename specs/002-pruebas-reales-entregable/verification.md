# Verificación de pruebas reales y entrega

Fecha: 2026-11-14. Revisión realizada por el mismo agente que agregó la configuración; no es revisión independiente.

## Evidencia ejecutada
- Suite aislada final: mvnw.cmd test, cwd raíz, 00:44:59 America/Bogota, exit 0: 11 tests, 0 fallos, 0 errores. Usa src/test/resources/application.properties con H2 en memoria.
- Build HTTP: mvnw.cmd -o -Daether.syncContext.named.factory=noop -Phttp -DskipTests package, exit 0 a las 00:23:01. Artefacto HTTP separado. Ese build omitió tests de forma explícita; la suite se ejecutó por separado.
- Stdio: Claude mostró servidor en ejecución y ejecutó cinco herramientas. Solicitud inicial interrumpida; continuación consultó historial antes de consignar. El usuario atendió permisos. Dos mensajes, ninguna compra de plan.
- HTTP visual: Postman12.34.0, MCP nativo, conexión/discovery, saldo e historial. Después de cerrar Claude, nuevo proceso HTTP sobre H2 en modo ACCESS_MODE_DATA=r y ddl-auto=validate devolvió saldo1001=1155000 y los movimientos de la sesión. No hubo escrituras nuevas desde Postman.
- Script HTTP de solo lectura: python scripts/verify_mcp_http.py, cwd raíz, exit 0. Siete comprobaciones; respuestas completas en [JSON](evidence/http-verificacion.json).
- PDF: siete páginas renderizadas e inspeccionadas; no se observaron textos cortados, tablas desbordadas ni solapamientos. [Copia revisada](evidence/informe.pdf).

## Criterio y evidencia
| Criterio | Evidencia | Alcance |
| --- | --- | --- |
| AC-01 | checks test/package; servicio JPA/H2; [Claude](evidence/claude.png) | Gates automatizados más interacción observada; no prueban concurrencia |
| AC-02 | Build HTTP, properties, [respuestas](evidence/http-verificacion.json) | Loopback, perfil opcional, base separada por defecto |
| AC-03 | [Claude](evidence/claude.png), transcripciones output/evidencias/claude-creditos.txt | Cinco herramientas, dos estados de crédito, rechazo y saldos |
| AC-04 | [Postman](evidence/postman.png), JSON | Lectura real tras cambio de proceso; escrituras cubiertas en Claude |
| AC-05 | [Informe](evidence/informe.pdf), README y colección | Revisión visual completa; colección no ejecutada en Runner |

## Incidencias y límites
- La primera ruta Roaming de configuración no era la usada por Claude instalado. Se corrigió el archivo efectivo con respaldo y preservando preferencias; no se copiaron datos privados al informe.
- Un primer empaquetado HTTP intentó reutilizar el JAR abierto de Claude y falló en rename. Se separó finalName y se reconstruyó Stdio antes de continuar.
- El control automático rechazó una consignación de 20000 preparada en Postman por cambio de monto y riesgo de duplicación. No se ejecutó. La continuación conservó Postman como cliente de consultas y persistencia.
- Primer script HTTP esperaba solo JSON; se corrigió para interpretar respuestas SSE y pasó al repetir las consultas.
- Mockito advierte autoacoplamiento de agente en JDK25; Spring AI emite warnings de escáner. No hubo fallos de tests.
- Sin Inspector, carga, concurrencia ni inyección de fallos de rollback. Importes double y Javadoc de accesores no auditado completamente. No es certificación bancaria ni validación de producción.

## Cierre
Los checks finales deben constar como PASSED en verification.json generado por verify. La auditoría documental no sustituye sus resultados ni las observaciones UI. Sin ramas, commits, publicación ni despliegue.

Para satisfacer los criterios funcionales con checks ejecutables, se añaden http-transport (build más JVM/H2 aislada y protocolo real) y delivery-artifacts (estructura de PDF/colección/evidencia). Las observaciones UI y revisión visual siguen siendo evidencia separada. Los resultados finales se registran en verification.json.

# Revisión documental posterior del 14 de noviembre de 2026

Por solicitud de Brahian Ospina se reemplazaron las capturas del informe con sus siete PNG manuales (21 a 27), se añadió su nombre al inicio y se incorporaron dos diagramas vectoriales: componentes y transferencia. El PDF vigente tiene ocho páginas. La evidencia y la atestación funcional anteriores se conservan como registro histórico de la ejecución original.

- Ejecutado: `scripts/build_delivery.py`, generación del PDF revisado, exit 0.
- Ejecutado: `scripts/check_delivery_artifacts.py`, exit 0; ocho páginas, autor, dos diagramas, imágenes, colección y evidencia HTTP válidos.
- Observado: las ocho páginas renderizadas en `tmp/pdfs/review-v2` se inspeccionaron visualmente; sin texto cortado ni desbordamientos. Las capturas se insertaron como recortes de PNG sin compresión JPEG; los originales completos están en el ZIP.
- Ejecutado: `build_package()`, ZIP de 19 archivos, CRC correcto e integridad SHA-256 comprobada para las 15 evidencias indexadas.
- Ejecutado: `omni-spec audit 002 --json`, valid=true, sin errores ni advertencias. Es una auditoría documental, no una nueva ejecución de las pruebas de aplicación.
- Alcance: esta revisión no invocó herramientas bancarias ni consultas adicionales a Claude. No se repitió la suite Maven, pues no cambió el comportamiento del servidor.

