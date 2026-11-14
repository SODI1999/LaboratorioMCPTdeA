"""Verify live MCP discovery and persisted demo results without write tools.

Run while the HTTP server reads the Claude database. The only tools invoked are
consultarSaldo and listarMovimientos. A failed assertion exits unsuccessfully.
"""

import json
from datetime import datetime, timezone
from pathlib import Path
from urllib.request import Request, urlopen


def verify(expected_balances=("1,155,000.00", "655,000.00"), delivery_history=True,
           output_path=Path("output/evidencias/http-verificacion.json")):
    """Check live balances and discovery, optionally requiring the delivery history.

    expected_balances contains the formatted balances for accounts 1001 and 1002.
    delivery_history selects checks for the recorded Claude operations rather than
    a newly seeded database. output_path receives the complete read-only evidence.
    """
    endpoint = "http://127.0.0.1:8080/mcp"
    headers = {"Content-Type": "application/json", "Accept": "application/json, text/event-stream"}
    records = []

    def rpc(method, params, request_id=None):
        payload = {"jsonrpc": "2.0", "method": method, "params": params}
        if request_id is not None:
            payload["id"] = request_id
        request = Request(endpoint, json.dumps(payload).encode(), headers=headers, method="POST")
        with urlopen(request, timeout=20) as response:
            raw = response.read().decode()
            session_id = response.headers.get("Mcp-Session-Id")
            if session_id:
                headers["Mcp-Session-Id"] = session_id
            if response.headers.get("Content-Type", "").startswith("text/event-stream"):
                messages = [json.loads(line[5:].strip()) for line in raw.splitlines() if line.startswith("data:")]
                result = next((message for message in messages if message.get("id") == request_id), None)
                assert result is not None, "Missing JSON-RPC reply in event stream"
            else:
                result = json.loads(raw) if raw else None
            records.append({"request": payload, "status": response.status, "response": result})
            assert response.status in (200, 202)
            if result:
                assert "error" not in result, result
            return result

    init = rpc("initialize", {"protocolVersion": "2025-11-25", "capabilities": {},
                              "clientInfo": {"name": "read-only-delivery-check", "version": "1.0"}}, 1)
    headers["MCP-Protocol-Version"] = init["result"]["protocolVersion"]
    rpc("notifications/initialized", {})
    tools = rpc("tools/list", {}, 2)["result"]["tools"]
    assert {tool["name"] for tool in tools} == {
        "consultarSaldo", "consignarDinero", "transferirDinero", "solicitarCredito", "listarMovimientos"
    }

    def call(name, account, request_id):
        result = rpc("tools/call", {"name": name, "arguments": {"numeroCuenta": account}}, request_id)["result"]
        assert not result.get("isError", False)
        return "\n".join(item.get("text", "") for item in result["content"])

    saldo_1001 = call("consultarSaldo", "1001", 3)
    saldo_1002 = call("consultarSaldo", "1002", 4)
    movements = call("listarMovimientos", "1001", 5)
    assert expected_balances[0] in saldo_1001
    assert expected_balances[1] in saldo_1002
    if delivery_history:
        assert "CREDIT_DISBURSEMENT: $100,000.00" in movements
        assert "TRANSFER_OUT: $5,000.00" in movements
        assert "DEPOSIT: $10,000.00" in movements
    else:
        assert "DEPOSIT: $2,500,000.00" in movements
    assert "999,999,999.00" not in movements
    evidence = {"executedAt": datetime.now(timezone.utc).isoformat(), "endpoint": endpoint,
                "mode": "read-only-tools", "passed": True, "checks": [
                    "initialize", "initialized", "five-tools-discovered", "persisted-saldo-1001",
                    "persisted-saldo-1002", "deposit-transfer-credit-history" if delivery_history else "seeded-history",
                    "no-rejected-transfer-movement"
                ], "records": records}
    output = Path(output_path)
    output.parent.mkdir(parents=True, exist_ok=True)
    output.write_text(json.dumps(evidence, ensure_ascii=False, indent=2), encoding="utf-8")
    print("PASS: 7 comprobaciones MCP HTTP de solo lectura; saldos e historial coinciden con el escenario esperado.")


if __name__ == "__main__":
    verify()
