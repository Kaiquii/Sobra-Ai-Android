package br.com.sobraai.ui

class WorkspaceFeatures(val reports: ReportsPresenter, val assistant: AssistantPresenter, val profile: ProfilePresenter,
    val native: NativeFeatures) {
    fun open(route: FinanceRoute) {
        when (route) {
            FinanceRoute.Reports -> reports.reports.open()
            FinanceRoute.Commitments -> reports.commitments.open()
            FinanceRoute.Assistant -> assistant.history()
            FinanceRoute.Profile -> profile.open()
            FinanceRoute.Categories -> profile.categories.refresh()
            else -> Unit
        }
        if (route != FinanceRoute.Assistant) assistant.stopVoice()
    }
    fun invalidate() { reports.invalidate() }
}
