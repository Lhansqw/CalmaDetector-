import XCTest
@testable import CalmaCore

final class PairingCodeManagerTests: XCTestCase {
    private var now: Int64 = 1_000_000
    private lazy var manager = PairingCodeManager(clock: { [unowned self] in self.now }, ttlMs: 60_000, maxAttempts: 3)

    func testGeneraCodigosDe6Digitos() {
        for _ in 0..<200 {
            let code = manager.newCode().code
            XCTAssertEqual(code.count, 6)
            XCTAssertTrue(code.allSatisfy { ("0"..."9").contains($0) })
        }
    }

    func testCodigoCorrectoVinculaUnaSolaVez() {
        let code = manager.newCode()
        XCTAssertEqual(manager.verify(code.formatted), .success) // acepta "123 456"
        XCTAssertEqual(manager.verify(code.code), .noActiveCode)
    }

    func testCodigoCaducadoSeRechaza() {
        let code = manager.newCode()
        now += 60_000
        XCTAssertEqual(manager.verify(code.code), .expired)
        XCTAssertNil(manager.active)
    }

    func testDemasiadosIntentosInvalidanElCodigo() {
        let code = manager.newCode()
        let wrong = code.code == "000000" ? "111111" : "000000"
        XCTAssertEqual(manager.verify(wrong), .wrongCode(attemptsLeft: 2))
        XCTAssertEqual(manager.verify(wrong), .wrongCode(attemptsLeft: 1))
        XCTAssertEqual(manager.verify(wrong), .tooManyAttempts)
        XCTAssertEqual(manager.verify(code.code), .noActiveCode)
    }

    func testCodigoNuevoReiniciaLosIntentos() {
        manager.newCode()
        _ = manager.verify("x")
        _ = manager.verify("x")
        let code = manager.newCode()
        XCTAssertEqual(manager.verify(code.code), .success)
    }

    func testNormalizeQuitaEspaciosYGuiones() {
        XCTAssertEqual(PairingCodeManager.normalize(" 482-913 "), "482913")
    }

    func testFormatoConEspacio() {
        XCTAssertEqual(ActiveCode(code: "482913", expiresAtMs: 0).formatted, "482 913")
    }

    func testTokenEsBase64UrlDe43Caracteres() {
        let token = PairingCodeManager.newToken()
        XCTAssertEqual(token.count, 43)
        XCTAssertFalse(token.contains("+") || token.contains("/") || token.contains("="))
        XCTAssertNotEqual(token, PairingCodeManager.newToken())
    }
}

final class PairingProtocolTests: XCTestCase {
    func testRequestIdaYVuelta() {
        let r = PairingProtocol.Request(code: "123456", phoneName: "iPhone")
        XCTAssertEqual(PairingProtocol.decode(PairingProtocol.Request.self, from: PairingProtocol.encode(r)), r)
    }

    func testResultConCamposOpcionales() {
        let r = PairingProtocol.Result(ok: false, error: "Código incorrecto", attemptsLeft: 2)
        XCTAssertEqual(PairingProtocol.decode(PairingProtocol.Result.self, from: PairingProtocol.encode(r)), r)
    }

    func testDatosInvalidosDevuelvenNil() {
        XCTAssertNil(PairingProtocol.decode(PairingProtocol.Request.self, from: Data("basura".utf8)))
    }
}

final class PairingStoreTests: XCTestCase {
    func testGuardaCargaYBorra() {
        let defaults = UserDefaults(suiteName: "calma-tests-\(UUID().uuidString)")!
        let store = PairingStore(defaults: defaults)
        XCTAssertNil(store.load())
        let device = PairedDevice(name: "Apple Watch", token: "abc", pairedAtMs: 42)
        store.save(device)
        XCTAssertEqual(store.load(), device)
        store.clear()
        XCTAssertNil(store.load())
    }
}
