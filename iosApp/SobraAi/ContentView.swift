import SwiftUI
import UIKit
import SobraAiUI

@MainActor
final class AppRuntime: ObservableObject {
    let app: IosApp?

    init() {
        let raw = Bundle.main.object(forInfoDictionaryKey: "ApiBaseURL") as? String ?? ""
        let parts = URLComponents(string: raw)
        if let parts, parts.scheme == "https", let host = parts.host, !host.isEmpty,
           parts.user == nil, parts.password == nil, parts.query == nil, parts.fragment == nil {
            app = IosApp(apiBaseUrl: raw, sessionService: Bundle.main.bundleIdentifier! + ".session")
        } else {
            app = nil
        }
    }
}

private struct ComposeView: UIViewControllerRepresentable {
    let runtime: IosApp
    func makeUIViewController(context: Context) -> UIViewController { runtime.viewController() }
    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}

@main
struct SobraAiApplication: App {
    @StateObject private var runtime = AppRuntime()
    @Environment(\.scenePhase) private var phase

    var body: some Scene {
        WindowGroup {
            ZStack {
                if let app = runtime.app {
                    ComposeView(runtime: app).ignoresSafeArea(.all)
                } else {
                    VStack(spacing: 16) {
                        Text("Configuração necessária").font(.title2)
                        Text("Configure o endereço HTTPS da API no projeto iOS para entrar no SobraAi.")
                    }.padding()
                }
                // Hide credentials and financial data from the app switcher and inactive scenes.
                if phase != .active {
                    Color(uiColor: .systemBackground).ignoresSafeArea()
                    Text("SobraAi").font(.title).accessibilityLabel("Aplicativo protegido")
                }
            }
            .onChange(of: phase) { next in
                if next == .background { runtime.app?.enterBackground() }
                if next == .active { runtime.app?.enterForeground() }
            }
        }
    }
}
