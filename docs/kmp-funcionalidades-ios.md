# Relatórios, assistente, perfil e autenticação compartilhados

Android e iOS abrem o mesmo `SobraAiApp(services)`. Todas as telas completas,
apresentação e navegação estão em `sharedUi/commonMain`, preservando o front-end
Android. As antigas telas iOS simplificadas foram removidas; não recriá-las.
Veja [o mapa atual](ai-project-map.md) e [a política obrigatória](../rules.md).

| Funcionalidade | Implementação comum |
| --- | --- |
| Relatórios, comparação, categorias, gráficos e resumo anual | `feature/relatorios/RelatoriosScreen.kt`, `RelatoriosViewModel.kt` e componentes em `designSystem` |
| Compromissos parcelados | `feature/relatorios/InstallmentCommitmentsScreen.kt` e `InstallmentCommitmentsViewModel.kt`; abas, filtros, calendário, projeção e compras completas |
| Exportação | `feature/relatorios/export/ReportExportSheet.kt`, `ReportExportViewModel.kt`, `ReportExportModels.kt`; sete conteúdos, PDF/Excel/CSV, progresso, cancelamento e diálogo Abrir/Compartilhar |
| Assistente | `feature/assistant/AssistantScreen.kt` e `AssistantViewModel.kt`; nova conversa, histórico, abrir/excluir, cota e ditado revisado antes de enviar |
| Perfil | `feature/perfil/PerfilScreen.kt`; mesmo cabeçalho, foto, preview, remoção e opções do Android |
| Editar perfil, rendas e categorias | `feature/perfil/components/`; formulários e confirmações completos |
| Ajuda | `designSystem/.../feature/perfil/components/AjudaScreen.kt` |
| Login, cadastro e recuperação | `feature/login/`, `feature/register/` e `core/data/AuthViewModel.kt`; sessão e biometria em `AppController` |

A base dos caminhos de telas é
`sharedUi/src/commonMain/kotlin/com/example/appfinanceiro/`.
`UiServices` injeta os serviços; `NativeFeatures` e `ReportFiles` isolam galeria,
voz, arquivos e permissões. Adaptadores não definem telas ou navegação.

Android mantém streaming Retrofit/MediaStore e seleção de documento nos sistemas
antigos. Apple salva relatórios protegidos em Documents/SobraAi, abre preview
nativo e a folha de compartilhar. Fotos são orientadas e recodificadas em JPEG;
voz preenche o campo para revisão, sem envio automático. Face ID/Touch ID é
detectado pelo sensor e autentica de verdade. O AppIcon usa o logo Android.

Proteção Apple de envio de códigos e consulta de versão da loja dependem do
[backend externo](pendencias-backend-ios.md). Não há prova fictícia, remoção de
Play Integrity ou envio desprotegido. A unificação visual não encerra essa pendência.

`harnessVerify` valida a estrutura única, testes JVM, APK/Lint e fontes/testes
KLIB iOS. Build/link Swift/Xcode e execução real precisam do ambiente Apple;
comparação visual e testes de sensores são conduzidos pelo usuário. Não declarar
paridade visual comprovada apenas pela compilação.
