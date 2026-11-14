"""Build the academic PDF report, read-only Postman collection and evidence index."""

import hashlib
import json
import zipfile
from pathlib import Path
from xml.etree import ElementTree
from xml.sax.saxutils import escape

from PIL import Image
from reportlab.lib import colors
from reportlab.lib.enums import TA_CENTER
from reportlab.lib.pagesizes import A4
from reportlab.lib.styles import ParagraphStyle, getSampleStyleSheet
from reportlab.lib.utils import ImageReader
from reportlab.platypus import Flowable, Paragraph, SimpleDocTemplate, Spacer, Table, TableStyle, PageBreak

ROOT = Path(__file__).resolve().parents[1]
OUT = ROOT / "output"
EVIDENCE = OUT / "evidencias"
PDF = OUT / "pdf" / "Informe-Proyecto-MCP-Bancario.pdf"


def build_collection():
    """Create ordered JSON-RPC requests with session capture and read-only tools."""
    requests = [
        ("01 Inicializar sesión MCP", "initialize", {"protocolVersion": "2025-11-25", "capabilities": {},
         "clientInfo": {"name": "postman-laboratorio", "version": "1.0"}}),
        ("02 Notificar inicialización", "notifications/initialized", {}),
        ("03 Descubrir herramientas", "tools/list", {}),
        ("04 Consultar saldo 1001", "tools/call", {"name": "consultarSaldo", "arguments": {"numeroCuenta": "1001"}}),
        ("05 Consultar saldo 1002", "tools/call", {"name": "consultarSaldo", "arguments": {"numeroCuenta": "1002"}}),
        ("06 Consultar movimientos 1001", "tools/call", {"name": "listarMovimientos", "arguments": {"numeroCuenta": "1001"}}),
    ]
    items = []
    for i, (name, method, params) in enumerate(requests, 1):
        payload = {"jsonrpc": "2.0", "method": method, "params": params}
        if not method.startswith("notifications/"):
            payload["id"] = i
        headers = [{"key": "Content-Type", "value": "application/json"},
                   {"key": "Accept", "value": "application/json, text/event-stream"}]
        if i > 1:
            headers += [{"key": "Mcp-Session-Id", "value": "{{mcpSessionId}}"},
                        {"key": "MCP-Protocol-Version", "value": "2025-11-25"}]
        checks = ['pm.test("Respuesta HTTP MCP válida", () => pm.expect([200,202]).to.include(pm.response.code));']
        if i == 1:
            checks += ['pm.collectionVariables.set("mcpSessionId", pm.response.headers.get("Mcp-Session-Id"));',
                       'pm.test("Servidor bancario", () => pm.expect(pm.response.json().result.serverInfo.name).to.eql("bank-mcp-server"));']
        if method == "tools/list":
            checks += [
                'const raw = pm.response.text();',
                'const body = raw.trim().startsWith("{") ? JSON.parse(raw) : JSON.parse(raw.split(/\\r?\\n/).find(line => line.startsWith("data:")).slice(5).trim());',
                'pm.test("Cinco herramientas", () => pm.expect(body.result.tools).to.have.lengthOf(5));',
                'pm.test("Saldo descubierto", () => pm.expect(body.result.tools.map(t => t.name)).to.include("consultarSaldo"));'
            ]
        items.append({"name": name, "request": {"method": "POST", "header": headers,
                      "url": "{{mcpUrl}}", "body": {"mode": "raw", "raw": json.dumps(payload, ensure_ascii=False, indent=2),
                      "options": {"raw": {"language": "json"}}}},
                      "event": [{"listen": "test", "script": {"type": "text/javascript", "exec": checks}}]})
    collection = {"info": {"name": "MCP Bancario TdeA Consultas",
        "description": "Ejecutar en orden contra el servidor HTTP. Solo consultas: no consigna, transfiere ni solicita créditos. Las respuestas pueden usar SSE.",
        "schema": "https://schema.getpostman.com/json/collection/v2.1.0/collection.json"},
        "variable": [{"key": "mcpUrl", "value": "http://127.0.0.1:8080/mcp"},
                     {"key": "mcpSessionId", "value": ""}], "item": items}
    path = OUT / "postman" / "MCP-Bancario-Consultas.postman_collection.json"
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(collection, ensure_ascii=False, indent=2), encoding="utf-8")


class Screenshot(Flowable):
    """Place an unaltered screenshot with an explicit rectangular viewport crop."""

    def __init__(self, filename, crop, width=495):
        super().__init__()
        self.path = EVIDENCE / filename
        self.crop = crop
        self.image = Image.open(self.path).crop(crop).convert("RGB")
        self.width = width
        self.scale = width / (crop[2] - crop[0])
        self.height = (crop[3] - crop[1]) * self.scale

    def draw(self):
        self.canv.drawImage(ImageReader(self.image), 0, 0, self.width, self.height)


class Architecture(Flowable):
    """Draw multi-layered architecture and sequence diagrams using PDF vector primitives."""

    def __init__(self, transfer=False):
        super().__init__()
        self.width, self.height = 495, 290 if transfer else 418
        self.transfer = transfer

    def draw(self):
        c = self.canv
        navy = colors.HexColor("#18354b")
        slate = colors.HexColor("#365c78")
        fill_zone = colors.HexColor("#f7f9fb")
        fill_box = colors.HexColor("#edf3f8")
        fill_accent = colors.HexColor("#dce8f2")
        fill_warn = colors.HexColor("#fdf2f2")
        stroke_warn = colors.HexColor("#9e3b3b")
        fill_ok = colors.HexColor("#eef7f1")
        stroke_ok = colors.HexColor("#2c6e49")
        muted = colors.HexColor("#4f6373")

        def zone(x, y, w, h, label):
            c.saveState()
            c.setFillColor(fill_zone)
            c.setStrokeColor(colors.HexColor("#b7c8d6"))
            c.setDash(3, 2)
            c.roundRect(x, y, w, h, 6, fill=1, stroke=1)
            c.setDash()
            c.setFillColor(slate)
            c.roundRect(x + 8, y + h - 13, len(label) * 4.5 + 14, 13, 3, fill=1, stroke=0)
            c.setFillColor(colors.white)
            c.setFont("Helvetica-Bold", 7)
            c.drawString(x + 15, y + h - 9.5, label)
            c.restoreState()

        def box(x, y, w, h, title, lines, bg=fill_box, bd=slate, tc=navy):
            c.saveState()
            c.setFillColor(bg)
            c.setStrokeColor(bd)
            c.setLineWidth(1)
            c.roundRect(x, y, w, h, 5, fill=1, stroke=1)
            c.setFillColor(tc)
            c.setFont("Helvetica-Bold", 8.8)
            top = y + h - 14
            c.drawCentredString(x + w / 2, top, title)
            c.setFillColor(muted)
            c.setFont("Helvetica", 7.3)
            for idx, line in enumerate(lines):
                c.drawCentredString(x + w / 2, top - 11 - idx * 9.5, line)
            c.restoreState()

        def arrow(x, y, xx, yy, label="", dashed=False, color=slate, lx=None, ly=None):
            import math
            c.saveState()
            c.setStrokeColor(color)
            c.setLineWidth(1.0)
            if dashed:
                c.setDash(3, 2)
            c.line(x, y, xx, yy)
            c.setDash()
            a = math.atan2(yy - y, xx - x)
            for delta in (-0.45, 0.45):
                c.line(xx, yy, xx - 6 * math.cos(a + delta), yy - 6 * math.sin(a + delta))
            if label:
                c.setFont("Helvetica-Bold", 7)
                tx = lx if lx is not None else (x + xx) / 2
                ty = ly if ly is not None else (y + yy) / 2 + 3
                tw = c.stringWidth(label, "Helvetica-Bold", 7) + 6
                c.setFillColor(colors.white)
                c.rect(tx - tw / 2, ty - 2, tw, 9.5, fill=1, stroke=0)
                c.setFillColor(color)
                c.drawCentredString(tx, ty, label)
            c.restoreState()

        if self.transfer:
            # Diagrama de secuencia: flujo transaccional de transferirDinero
            lifelines = [
                (58, "Cliente MCP", "Claude / Postman"),
                (182, "Servidor MCP", "Spring AI 2.0.1"),
                (312, "BankService", "@Tool + @Transactional"),
                (440, "JPA / H2", "Repositories + DB"),
            ]
            for lx, title, sub in lifelines:
                box(lx - 52, 248, 104, 34, title, [sub], bg=fill_accent)
                c.saveState()
                c.setStrokeColor(colors.HexColor("#9fb3c4"))
                c.setDash(3, 2)
                c.line(lx, 248, lx, 8)
                c.restoreState()

            arrow(58, 228, 182, 228, "1. tools/call transferirDinero")
            arrow(182, 206, 312, 206, "2. invoca método (inicia TX)")
            arrow(312, 184, 440, 184, "3. findByAccountNumber")
            arrow(440, 162, 312, 162, "4. cuentas origen/destino", dashed=True)

            # Bloque ALT
            c.saveState()
            c.setStrokeColor(slate)
            c.setDash(3, 2)
            c.roundRect(10, 16, 475, 132, 4, fill=0, stroke=1)
            c.setDash()
            c.setFillColor(slate)
            c.rect(10, 135, 30, 13, fill=1, stroke=0)
            c.setFillColor(colors.white)
            c.setFont("Helvetica-Bold", 7)
            c.drawString(16, 139, "ALT")
            c.setStrokeColor(colors.HexColor("#b7c8d6"))
            c.setDash(2, 2)
            c.line(10, 95, 485, 95)
            c.restoreState()

            c.saveState()
            c.setFillColor(stroke_warn)
            c.setFont("Helvetica-Bold", 7.2)
            c.drawString(46, 138, "[Cuenta inválida o saldo < monto]")
            c.restoreState()
            arrow(312, 114, 58, 114, "5a. Error: Fondos insuficientes (sin mutar BD)", dashed=True, color=stroke_warn, lx=185, ly=118)

            c.saveState()
            c.setFillColor(stroke_ok)
            c.setFont("Helvetica-Bold", 7.2)
            c.drawString(46, 82, "[Validación exitosa: débito y crédito atómicos]")
            c.restoreState()
            arrow(312, 66, 440, 66, "5b. save(1001,1002) + TX_OUT/IN", color=stroke_ok)
            arrow(440, 46, 312, 46, "6. COMMIT ACID", dashed=True, color=stroke_ok)
            arrow(312, 27, 58, 27, "7. Confirmación JSON-RPC 2.0 con nuevo saldo", dashed=True, color=stroke_ok, lx=185, ly=31)
        else:
            # Diagrama de arquitectura de componentes y transportes
            zone(0, 344, 495, 68, "CAPA 1 · CLIENTES MCP")
            box(14, 354, 215, 44, "Claude Desktop (Cliente Local)", ["Configuración mcpServers · Aprobación manual"], bg=fill_accent)
            box(266, 354, 215, 44, "Postman (Cliente HTTP MCP)", ["Conexión nativa 127.0.0.1:8080/mcp + Colección"])

            zone(0, 240, 495, 86, "CAPA 2 · TRANSPORTE Y PROTOCOLO JSON-RPC 2.0")
            box(14, 250, 215, 60, "Transporte Stdio (Predeterminado)", [
                "JAR: mcp-bank-tdea-0.0.1-SNAPSHOT.jar",
                "web-application-type=none | banner=off",
                "logging.threshold.console=OFF"
            ])
            box(266, 250, 215, 60, "Streamable HTTP (Perfil -Phttp)", [
                "JAR: mcp-bank-tdea-http-0.0.1-SNAPSHOT.jar",
                "spring-ai-starter-mcp-server-webmvc",
                "Endpoint /mcp · JSON + SSE en loopback"
            ])

            zone(0, 120, 495, 104, "CAPA 3 · CONTENEDOR SPRING BOOT 4.1.2 + SPRING AI 2.0.1")
            box(14, 162, 150, 44, "McpBankTdeaApplication", ["@Bean bankTools", "ToolCallbacks.from(bankService)"])
            box(14, 128, 150, 28, "DataInitializer", ["Seed 1001, 1002, 1003 si vacía"])
            box(178, 128, 303, 78, "BankService (@Service · Lógica de Dominio)", [
                "Lectura: consultarSaldo · listarMovimientos",
                "Escritura (@Transactional): consignarDinero · transferirDinero",
                "Crédito (@Transactional): solicitarCredito (<= 10M APPROVED)",
                "Auditoría inmutable: DEPOSIT, TRANSFER_OUT/IN, CREDIT_DISBURSEMENT"
            ], bg=fill_accent)

            zone(0, 0, 495, 104, "CAPA 4 · PERSISTENCIA SPRING DATA JPA Y H2 2.4.240")
            box(14, 56, 467, 34, "Repositorios Spring Data JPA (Hibernate 7)", [
                "AccountRepository (accounts)  ·  TransactionRepository (transactions)  ·  CreditRequestRepository (credit_requests)"
            ])
            box(14, 10, 224, 38, "H2 Archivo Stdio: ./data/bankdb", ["Uso exclusivo por proceso; consulta HTTP en solo lectura"], bg=fill_ok, bd=stroke_ok)
            box(257, 10, 224, 38, "H2 Archivo HTTP: ./data/bankdb-http", ["Base aislada por defecto para perfil -Phttp"])

            arrow(121, 354, 121, 310, "stdin / stdout", lx=154, ly=331)
            arrow(373, 354, 373, 310, "POST /mcp", lx=402, ly=331)
            arrow(121, 250, 240, 206, "MCP Sync", lx=182, ly=231)
            arrow(373, 250, 355, 206, "MCP WebMVC", lx=396, ly=231)
            arrow(164, 184, 178, 184)
            arrow(330, 128, 330, 90, "JPA", lx=344, ly=109)
            arrow(126, 56, 126, 48)
            arrow(369, 56, 369, 48)


def build_report():
    """Create the evidence-based report with user screenshots and vector diagrams."""
    PDF.parent.mkdir(parents=True, exist_ok=True)
    http = json.loads((EVIDENCE / "http-verificacion.json").read_text(encoding="utf-8"))
    assert http["passed"]
    suites = list((ROOT / "target" / "surefire-reports").glob("TEST-*.xml"))
    tests = sum(int(ElementTree.parse(path).getroot().get("tests", 0)) for path in suites)
    failures = sum(int(ElementTree.parse(path).getroot().get("failures", 0)) +
                   int(ElementTree.parse(path).getroot().get("errors", 0)) for path in suites)
    assert tests == 11 and failures == 0
    styles = getSampleStyleSheet()
    styles.add(ParagraphStyle(name="ReportTitle", fontName="Helvetica-Bold", fontSize=24, leading=28, spaceAfter=14, textColor=colors.black))
    styles.add(ParagraphStyle(name="ReportSub", fontName="Helvetica", fontSize=12, leading=16, spaceAfter=12))
    styles.add(ParagraphStyle(name="ReportBody", fontName="Helvetica", fontSize=10, leading=14, spaceAfter=8))
    styles.add(ParagraphStyle(name="ReportH", fontName="Helvetica-Bold", fontSize=15, leading=19, spaceBefore=8, spaceAfter=10, textColor=colors.black))
    styles.add(ParagraphStyle(name="ReportSmall", fontName="Helvetica", fontSize=8.5, leading=11, spaceAfter=6))
    styles.add(ParagraphStyle(name="ReportCode", fontName="Courier", fontSize=8, leading=11, spaceAfter=8, backColor=colors.HexColor("#f3f5f7"), borderPadding=8))
    story = []
    def p(text, style="ReportBody"):
        story.append(Paragraph(text, styles[style]))
    def h(text):
        p(text, "ReportH")
    def table(rows, widths):
        formatted = [[Paragraph(escape(str(cell)), styles["ReportSmall"]) for cell in row] for row in rows]
        t = Table(formatted, colWidths=widths, repeatRows=1, hAlign="LEFT")
        t.setStyle(TableStyle([("BACKGROUND", (0,0), (-1,0), colors.HexColor("#dce6ee")),
                               ("GRID", (0,0), (-1,-1), .5, colors.HexColor("#d9d9d9")),
                               ("VALIGN", (0,0), (-1,-1), "MIDDLE"),
                               ("LEFTPADDING", (0,0), (-1,-1), 8), ("RIGHTPADDING", (0,0), (-1,-1), 8),
                               ("TOPPADDING", (0,0), (-1,-1), 7), ("BOTTOMPADDING", (0,0), (-1,-1), 7)]))
        story.append(t)
        story.append(Spacer(1, 10))
    def shot(filename, crop, caption, width=495):
        story.append(Screenshot(filename, crop, width))
        story.append(Spacer(1, 6))
        p(caption, "ReportSmall")
    def new():
        story.append(PageBreak())

    p("Informe del servidor MCP bancario", "ReportTitle")
    p("Implementación y pruebas reales con Claude Desktop y Postman", "ReportSub")
    p("<b>Brahian Ospina</b>", "ReportSub")
    p("Tecnológico de Antioquia · Laboratorio MCP · 14 de noviembre de 2026", "ReportSmall")
    h("Resultado del laboratorio")
    p("Se implementó un servidor MCP bancario con Spring Boot y persistencia H2. Las cinco herramientas se invocaron desde Claude Desktop; Postman se conectó mediante Streamable HTTP y consultó los resultados persistidos después de cambiar el proceso servidor. Las 11 pruebas automatizadas terminaron sin fallos ni errores.")
    p("El proyecto corresponde al ejercicio bancario de la página 17 de la guía del profesor Diego José Luis Botia. El ejemplo de lista de compras sirve como referencia técnica; el dominio de la entrega es bancario. La guía fija la entrega para el 15 de noviembre de 2026.")
    h("Objetivo y alcance")
    p("Permitir consultas de saldo, consignaciones, transferencias, solicitudes de crédito e historial mediante herramientas MCP, con cambios guardados en una base de datos. Se usan cuentas y dinero ficticios de un laboratorio local.")
    table([["Requisito del ejercicio", "Resultado verificado"],
           ["Servidor MCP con Spring Boot", "Inicialización y descubrimiento de cinco herramientas"],
           ["Base de datos de libre elección", "H2 en archivo con repositorios Spring Data JPA"],
           ["Transacciones bancarias", "Consignación, transferencia y dos estados de crédito"],
           ["Conexión con Claude Desktop", "Invocaciones reales con autorización de herramientas"],
           ["Prueba HTTP según el procedimiento", "Postman conectado a 127.0.0.1:8080/mcp"],
           ["Persistencia", "Saldo e historial de Claude leídos desde un nuevo proceso HTTP"]], [210,285])
    new()
    h("Arquitectura del sistema")
    p("Los dos clientes acceden a las mismas cinco operaciones de BankService mediante transportes MCP diferentes. Los repositorios JPA guardan las entidades del dominio en H2.")
    story.append(Architecture())
    p("Diagrama 1. Arquitectura de componentes derivada del código y de los perfiles de ejecución del proyecto.", "ReportSmall")
    p("Los transportes se construyen en JAR separados. El perfil HTTP usa por defecto otra base de demostración. Para la comprobación final se cerró Claude y se abrió su base secuencialmente por HTTP, en modo de archivo de solo lectura.")
    p("El dibujo representa las alternativas de acceso. No implica abrir simultáneamente el mismo archivo H2 desde ambos procesos. La prueba de persistencia cambió de proceso conservando el archivo de datos.")

    new()
    h("Implementación del servidor")
    table([["Componente", "Versión o responsabilidad"], ["Java", "Target 21; ejecución observada con Temurin 25.0.4.1"],
           ["Spring Boot y Spring AI", "4.1.2 y 2.0.1; se conservaron las versiones del proyecto"],
           ["H2 y Spring Data JPA", "Cuentas, movimientos y solicitudes de crédito en archivo local"],
           ["Stdio", "JAR predeterminado para Claude; banner y logs de consola desactivados"],
           ["Streamable HTTP", "Perfil http con starter WebMVC; escucha solo en loopback"]], [160,335])
    table([["Herramienta", "Comportamiento"], ["consultarSaldo", "Devuelve titular, estado y saldo por número de cuenta"],
           ["consignarDinero", "Incrementa saldo y registra DEPOSIT"],
           ["transferirDinero", "Valida cuentas y fondos; registra TRANSFER_OUT y TRANSFER_IN"],
           ["solicitarCredito", "Hasta 10.000.000 COP: APPROVED y desembolso; por encima: PENDING"],
           ["listarMovimientos", "Devuelve el historial ordenado de movimientos por cuenta"]], [160,335])
    p("Las operaciones de escritura usan @Transactional. La suite comprueba las rutas normales y algunos rechazos; esta evidencia no constituye una prueba exhaustiva de concurrencia ni de rollback ante cualquier fallo intermedio.")
    shot("27-usuario-claude-servidor.png", (600,110,1575,635),
         "Figura 1. Captura tomada por el autor: bank-mcp-server en ejecución, comando Java y argumentos. Recorte del panel de desarrollo.")

    new()
    h("Pruebas reales en Claude Desktop")
    p("Se utilizaron dos mensajes: una solicitud agrupada inicial y una continuación tras interrumpir una espera. Antes de continuar se consultó el historial para evitar duplicar la consignación. El usuario atendió las aprobaciones de herramientas. Los saldos iniciales eran datos existentes: 1.050.000 COP en 1001 y 650.000 COP en 1002.")
    shot("25-usuario-claude-resultados.png", (675,305,1605,905),
         "Figura 2. Captura tomada por el autor de la tabla final en Claude. Se observan operaciones exitosas, rechazo esperado, crédito pendiente y ambos saldos finales.")
    p("Comprobación aritmética: 1.050.000 + 10.000 - 5.000 + 100.000 = 1.155.000 COP. En la cuenta destino: 650.000 + 5.000 = 655.000 COP.", "ReportSmall")

    new()
    h("Flujo de transferencia y herramientas")
    story.append(Architecture(transfer=True))
    p("Diagrama 2. Flujo de BankService para una transferencia. La ruta exitosa y el rechazo por fondos insuficientes se observaron en las pruebas. No se inyectaron fallos intermedios para demostrar rollback.", "ReportSmall")
    shot("26-usuario-claude-herramientas.png", (675,290,1605,665),
         "Figura 3. Captura tomada por el autor del listado de invocaciones reales en Claude. Las transcripciones adjuntas conservan los resultados de las herramientas.")
    p("Los importes y cuentas son ficticios. Los identificadores 15 y 16 corresponden a esta base ya utilizada; una base nueva puede producir otros identificadores. Los anexos incluyen la transcripción de créditos y las capturas originales sin recorte.", "ReportSmall")

    new()
    h("Pruebas reales en Postman")
    p("Se creó una solicitud MCP nativa en Postman, se seleccionó HTTP y se conectó a http://127.0.0.1:8080/mcp. El cliente descubrió las cinco herramientas y ejecutó consultarSaldo y listarMovimientos. No se ejecutaron nuevas consignaciones, transferencias ni créditos desde Postman.")
    p("La primera fase utilizó bankdb-http, la base independiente del perfil. Para comprobar persistencia se cerró Claude, se reinició HTTP con bankdb en modo ACCESS_MODE_DATA=r y se validó el esquema sin modificarlo. Después de reconectar Postman, la cuenta 1001 devolvió el mismo saldo final de Claude.")
    shot("23-usuario-postman-saldo-preview.png", (360,85,1905,540),
         "Figura 4. Postman: endpoint local, herramienta consultarSaldo, argumento 1001 y solicitud tools/call.")
    shot("24-usuario-postman-saldo-json.png", (820,545,1905,865),
         "Figura 5. Respuesta JSON en Postman: saldo de 1.155.000 COP, isError=false y conexión activa. Los PNG originales se incluyen completos en el paquete.")
    table([["Comprobación HTTP", "Resultado"], ["Inicialización JSON-RPC", "HTTP 200; servidor bank-mcp-server"],
           ["Catálogo MCP", "Cinco herramientas disponibles"], ["Saldo persistido 1001", "1.155.000 COP"],
           ["Saldo persistido 1002", "655.000 COP; comprobado también por script de solo lectura"]], [225,270])

    new()
    h("Persistencia e historial bancario")
    p("El historial leído en Postman conserva los tres movimientos exitosos de esta sesión. La transferencia rechazada no aparece como un movimiento por 999.999.999 COP. Los registros previos del 1 de noviembre se mantuvieron.")
    shot("22-usuario-postman-historial-preview.png", (360,85,1905,540),
         "Figura 6. Postman: endpoint, listarMovimientos, cuenta 1001 y solicitud MCP.")
    shot("22-usuario-postman-historial-preview.png", (820,545,1905,775),
         "Figura 7. Historial persistido en Preview. Depósito, transferencia y desembolso con fecha y saldo posterior. Recorte de la captura del autor.")
    table([["Hora local del 14 de noviembre", "Movimiento", "Monto COP", "Saldo posterior COP"],
           ["00:30:11", "DEPOSIT", "10.000", "1.060.000"],
           ["00:32:20", "TRANSFER_OUT", "5.000", "1.055.000"],
           ["00:32:28", "CREDIT_DISBURSEMENT", "100.000", "1.155.000"]], [130,150,90,125])
    p("Además de la interacción visual, scripts/verify_mcp_http.py ejecutó siete comprobaciones de solo lectura: inicialización, notificación de sesión, catálogo de cinco herramientas, ambos saldos, historial y ausencia del monto rechazado. Las solicitudes y respuestas completas están en http-verificacion.json.")

    new()
    h("Reproducción y validación")
    p("Desde la raíz del proyecto, construir el servidor Stdio y ejecutar las pruebas:")
    p(".\\mvnw.cmd clean package<br/>.\\mvnw.cmd test", "ReportCode")
    p("Las pruebas tienen una configuración H2 en memoria exclusiva para impedir que sus rutinas de limpieza borren los datos de demostración. Resultado de la suite: <b>11 pruebas, 0 fallos y 0 errores</b>.")
    p("Para HTTP, construir y ejecutar:")
    p(".\\mvnw.cmd -Phttp -DskipTests package<br/>java -jar target/mcp-bank-tdea-http-0.0.1-SNAPSHOT.jar<br/>&nbsp;&nbsp;--spring.profiles.active=http", "ReportCode")
    p("El comando java anterior se escribe en una sola línea. El perfil HTTP crea un artefacto separado y usa bankdb-http por defecto. README.md contiene el comando completo para consultar secuencialmente la base de Claude en modo de solo lectura.")
    p("Importar output/postman/MCP-Bancario-Consultas.postman_collection.json y ejecutar en orden: initialize, notifications/initialized, tools/list, saldos 1001 y 1002, e historial 1001. Es una colección de consultas; la prueba visual de esta sesión se realizó con el cliente MCP nativo de Postman.")
    h("Alcance de las conclusiones")
    p("La evidencia respalda el ejercicio funcional local y la conexión con ambos clientes. No se realizaron pruebas de carga, concurrencia, exactitud monetaria exhaustiva ni seguridad de una exposición pública. El código usa double para importes y no se ha certificado para uso bancario real. No se verificó que todos los accesores públicos tengan Javadoc completo. MCP Inspector no fue ejecutado en esta sesión.")
    p("Se conservaron versiones distintas de las del ejemplo del PDF porque los dos transportes funcionaron en ejecución. La documentación oficial vigente consultada corresponde a Spring AI 2.0.2; la compatibilidad del proyecto 2.0.1 se sustentó en sus propios builds y respuestas reales.")
    h("Fuentes y anexos")
    p("1. Guia-LaboratorioMCPTDEA.pdf, Prof. Diego José Luis Botia, TdeA 2026: páginas 7 a 12 y 17.<br/>2. Código del proyecto: pom.xml, BankService.java, modelos JPA y application.properties.<br/>3. Spring AI, MCP Server Boot Starter, consultado el 14 de noviembre de 2026: <link href='https://docs.spring.io/spring-ai/reference/api/mcp/mcp-server-boot-starter-docs.html' color='#174b73'>documentación oficial de transportes MCP</link>.<br/>4. Capturas, transcripciones, resultados JUnit y respuestas HTTP incluidas en el paquete de evidencias.", "ReportSmall")
    p("El servidor demuestra las operaciones bancarias solicitadas, conserva sus resultados en H2 y permite consumirlas desde Claude Desktop por Stdio y desde Postman por Streamable HTTP. Las pruebas y sus límites quedan documentados para la revisión del laboratorio.")

    def footer(canvas, doc):
        canvas.setFont("Helvetica", 8)
        canvas.setFillColor(colors.HexColor("#555555"))
        canvas.drawString(50, 26, "LaboratorioMCPTdeA · Pruebas del 14 de noviembre de 2026")
        canvas.drawRightString(A4[0] - 50, 26, str(doc.page))
    SimpleDocTemplate(str(PDF), pagesize=A4, leftMargin=50, rightMargin=50,
                      topMargin=40, bottomMargin=43, title="Informe del servidor MCP bancario",
                      author="Brahian Ospina").build(story, onFirstPage=footer, onLaterPages=footer)


def build_index():
    """Write a portable index with hashes for the selected deliverables."""
    entries = []
    for path in delivery_evidence():
        entries.append({"file": path.name, "bytes": path.stat().st_size,
                        "sha256": hashlib.sha256(path.read_bytes()).hexdigest()})
    (EVIDENCE / "indice-evidencias.json").write_text(json.dumps({"date": "2026-11-14", "files": entries},
                                                             ensure_ascii=False, indent=2), encoding="utf-8")


def delivery_evidence():
    """Select clear user captures and original functional evidence for delivery."""
    names = ["claude-transcripcion.txt", "claude-consignacion.txt", "claude-creditos.txt",
             "claude-transferencias.txt", "http-verificacion.json", "resumen-verificacion-final.json",
             "com.tdea.mcp_bank_tdea.BankServiceTest.txt",
             "com.tdea.mcp_bank_tdea.McpBankTdeaApplicationTests.txt"]
    paths = sorted(EVIDENCE.glob("2[1-7]-usuario-*.png")) + [EVIDENCE / name for name in names]
    assert len(paths) == 15 and all(path.is_file() for path in paths)
    return paths


def build_package():
    """Package the revised report and verify the archive and evidence hashes."""
    paths = [PDF, ROOT / "README.md", OUT / "postman/MCP-Bancario-Consultas.postman_collection.json",
             EVIDENCE / "indice-evidencias.json", *delivery_evidence()]
    archive = OUT / "Entrega-MCP-Bancario.zip"
    with zipfile.ZipFile(archive, "w", zipfile.ZIP_DEFLATED) as package:
        for path in paths:
            package.write(path, path.relative_to(OUT) if path.is_relative_to(OUT) else path.name)
    with zipfile.ZipFile(archive) as package:
        assert package.testzip() is None
        for entry in json.loads((EVIDENCE / "indice-evidencias.json").read_text(encoding="utf-8"))["files"]:
            assert hashlib.sha256(package.read("evidencias/" + entry["file"])).hexdigest() == entry["sha256"]
    print(f"ZIP verificado: {len(paths)} archivos, {archive.stat().st_size} bytes")


if __name__ == "__main__":
    build_collection()
    build_report()
    build_index()
    build_package()
    print(PDF)
