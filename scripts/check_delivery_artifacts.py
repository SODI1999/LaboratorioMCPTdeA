"""Validate report structure, evidence files and the read-only Postman collection."""

import json
from pathlib import Path

import pymupdf


def check():
    """Assert deliverable structure; visual quality remains a separate manual review."""
    root = Path(__file__).resolve().parents[1]
    with pymupdf.open(root / "output/pdf/Informe-Proyecto-MCP-Bancario.pdf") as document:
        assert len(document) == 8
        text = "\n".join(page.get_text() for page in document)
        assert "Claude Desktop" in text and "Postman" in text
        assert "1.155.000" in text and "655.000" in text
        assert "Brahian Ospina" in document[0].get_text()
        assert "Diagrama 1" in text and "Diagrama 2" in text
        assert document[1].get_drawings() and document[4].get_drawings()
        assert all(document[i].get_images() for i in (2, 3, 4, 5, 6))
    collection = json.loads((root / "output/postman/MCP-Bancario-Consultas.postman_collection.json").read_text(encoding="utf-8"))
    assert len(collection["item"]) == 6
    for item in collection["item"]:
        payload = json.loads(item["request"]["body"]["raw"])
        if payload["method"] == "tools/call":
            assert payload["params"]["name"] in ("consultarSaldo", "listarMovimientos")
    evidence = json.loads((root / "output/evidencias/http-verificacion.json").read_text(encoding="utf-8"))
    assert evidence["passed"] and len(evidence["checks"]) == 7
    print("PASS: PDF de 8 páginas, autor, dos diagramas vectoriales, imágenes, colección de 6 consultas y evidencia HTTP íntegros. Revisión visual separada.")


if __name__ == "__main__":
    check()
