# Host iOS SobraAi

Siga o [passo a passo de build no Mac](../README.md#build-ios-no-mac) no README
principal: preparação do JDK/SDK, API e assinatura, build pelo Xcode/Terminal e testes.

O scheme é `SobraAi`, no projeto `SobraAi.xcodeproj`. A interface das funcionalidades
fica em Compose Multiplatform, no módulo `:sharedUi`; o host SwiftUI integra UIKit
e ciclo de vida. Há targets para aparelho ARM64 e simulador Apple Silicon.

Configuração, escopo e validação: [guia de integração](../docs/kmp-compose-ios.md).
Contratos de edição e cenários: [rendas/despesas](../docs/kmp-rendas-despesas.md).
Demais funcionalidades: [relatórios, assistente, perfil e autenticação](../docs/kmp-funcionalidades-ios.md).
Integrações externas: [pendências do backend Apple](../docs/pendencias-backend-ios.md).
