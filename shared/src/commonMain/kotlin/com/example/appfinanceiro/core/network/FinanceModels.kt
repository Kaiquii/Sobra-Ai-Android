package com.example.appfinanceiro.core.network

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SummaryResponse(
    val month: Int,
    val year: Int,
    val salario: Double,
    val adiantamento: Double,
    val renda_extra_amt: Double,
    val restante_salario: Double,
    val restante_adiantamento: Double,
    val restante_renda_extra: Double,
    val total_gasto_salario: Double,
    val total_gasto_adiantamento: Double,
    val total_gasto_renda_extra: Double,
    val total_expense: Double,
    val total_geral_disponivel: Double,
    val total_income: Double
)

@Serializable
data class Category(
    val id: Int,
    val user_id: Int? = null,
    val name: String
)

@Serializable
data class CategoriesResponse(val categories: List<Category>, val total: Int)

@Serializable
data class Expense(
    val id: Int,
    val category_id: Int,
    val amount: Double,
    @Serializable(with = LegacyTextSerializer::class)
    val description: String = "",
    @Serializable(with = LegacyTextSerializer::class)
    val date: String = "",
    @Serializable(with = LegacyTextSerializer::class)
    val type: String = "",
    val installments: Int?,
    val current_installment: Int?,
    val payment_source: String?,
    @Serializable(with = LegacyPaymentSplitsSerializer::class)
    val payment_splits: List<PaymentSplit> = emptyList(),
    val notes: String? = null,
    val is_paid: Boolean = false,
    val paid_at: String? = null,
    @SerialName("is_advanced")
    val isAdvanced: Boolean = false,
    @SerialName("advanced_at")
    val advancedAt: String? = null
)

@Serializable
data class PaymentSplit(
    val expense_id: Int? = null,
    @Serializable(with = LegacyTextSerializer::class)
    val payment_source: String = "",
    val amount: Double
)

@Serializable
data class PaymentSplitRequest(
    val payment_source: String,
    val amount: Double
)

val Expense.displayDescription: String
    get() = description.orEmpty().ifBlank { "Despesa sem descrição" }

fun Expense.paymentSources(): List<String> = payment_splits.orEmpty()
    .filterNotNull()
    .mapNotNull { it.payment_source?.takeIf(String::isNotBlank) }
    .ifEmpty { listOfNotNull(payment_source?.takeIf(String::isNotBlank)) }

fun Expense.paymentSourceLabel(): String = paymentSources()
    .joinToString(" + ")
    .ifBlank { "Não informado" }
fun Expense.paymentCardSourceLabel(): String {
    val sources = paymentSources().distinct()
    return if (sources.size > 1) "Pagamento dividido" else paymentSourceLabel()
}

@Serializable
data class ExpensesResponse(val expenses: List<Expense>, val total: Int)

@Serializable
data class Income(
    val id: Int,
    val source: String,
    val amount: Double,
    val month: Int,
    val year: Int
)
@Serializable
data class IncomesResponse(val incomes: List<Income>, val total: Int)

@Serializable
data class IncomeRequest(
    val source: String,
    val amount: Double,
    val month: Int,
    val year: Int,
    val type: String,
    val repeat_future: Boolean? = null
)

@Serializable
data class IncomeUpdateRequest(
    val amount: Double? = null,
    val update_future: Boolean? = null
)

@Serializable
data class CategoryRequest(
    val name: String
)

@Serializable
data class CategoryResponse(
    val data: Category,
    val message: String
)

@Serializable
data class AssistantChatRequest(
    val message: String,
    val conversation_id: Int? = null
)

@Serializable
data class AssistantChatResponse(
    val conversation_id: Int,
    val reply: String,
    val tool_call: String? = null,
    val error_code: String? = null,
    val retry_after_seconds: Int? = null,
    val tool_result: AssistantToolResult? = null
)

@Serializable
data class AssistantToolResult(
    val status: String? = null,
    val missing_fields: List<String> = emptyList()
)

@Serializable
data class AssistantConversation(
    val conversation_id: Int,
    val title: String,
    val created_at: String,
    val updated_at: String,
    val display_date: String? = null,
    val display_time: String? = null,
    val display_label: String? = null
)

@Serializable
data class AssistantConversationsResponse(
    val total: Int,
    val conversations: List<AssistantConversation>
)

@Serializable
data class AssistantConversationSummary(
    val conversation_id: Int,
    val title: String,
    val display_label: String? = null
)

@Serializable
data class AssistantStoredMessage(
    val message_id: Int,
    val conversation_id: Int,
    val role: String,
    val content: String,
    val display_time: String? = null
)

@Serializable
data class AssistantMessagesResponse(
    val conversation: AssistantConversationSummary,
    val messages: List<AssistantStoredMessage>
)


@Serializable
data class ExpenseRequest(
    val amount: Double,
    val description: String,
    val category_id: Int,
    val payment_source: String? = null,
    val payment_splits: List<PaymentSplitRequest>? = null,
    val date: String,
    val type: String,
    val installments: Int,
    val notes: String = ""
)

@Serializable
data class DefaultResponse(
    val message: String
)

@Serializable
data class ExpenseUpdateRequest(
    val amount: Double? = null,
    val description: String? = null,
    val category_id: Int? = null,
    val payment_source: String? = null,
    val payment_splits: List<PaymentSplitRequest>? = null,
    val type: String? = null,
    val date: String? = null,
    val notes: String? = null,
    val update_future: Boolean? = null
)

@Serializable
data class ExpensePaymentStatusRequest(
    @SerialName("is_paid") val isPaid: Boolean
)

@Serializable
data class ExpensePaymentStatusResponse(
    val message: String,
    val expense: Expense
)

@Serializable
data class UpdateAdvanceStatusRequest(
    @SerialName("is_advanced")
    val isAdvanced: Boolean,
    @SerialName("advanced_at")
    val advancedAt: String? = null
)

@Serializable
data class AdvanceStatusResponse(
    val message: String,
    val expense: Expense
)

@Serializable
data class UpdateProfileRequest(
    val name: String,
    val email: String
)

@Serializable
data class ProfileUser(
    val id: Int,
    val name: String,
    val email: String,
    val role: String,
    val avatar_url: String? = null
)

@Serializable
data class ProfileResponse(
    val user: ProfileUser
)

@Serializable
data class ProfilePhotoResponse(
    val message: String,
    val avatar_url: String? = null
)

@Serializable
data class AppVersionResponse(
    val id: Int? = null,
    val platform: String,
    @SerialName("latest_version_name")
    val latestVersionName: String,
    @SerialName("latest_version_code")
    val latestVersionCode: Int,
    @SerialName("min_required_version_code")
    val minRequiredVersionCode: Int,
    @SerialName("force_update")
    val forceUpdate: Boolean,
    @SerialName("play_store_url")
    val playStoreUrl: String,
    val message: String? = null,
    @SerialName("created_at")
    val createdAt: String? = null,
    @SerialName("updated_at")
    val updatedAt: String? = null
)

@Serializable
data class CategoryReportResponse(
    val category_id: Int,
    val category_name: String,
    val total_amount: Double,
    val percentage: Double
)

@Serializable
data class ChartReportResponse(
    val month: Int,
    val income: Double,
    val expense: Double
)

@Serializable
data class YearlySummaryResponse(
    val economia_total: Double,
    val media_mensal: Double,
    val year: Int
)

@Serializable
data class MonthComparisonResponse(
    val mes_atual: Int,
    val ano_atual: Int,
    val mes_comparado: Int,
    val ano_comparado: Int,
    val resumo: MonthComparisonSummary,
    val categorias: List<MonthComparisonCategory> = emptyList(),
    val fontes_pagamento: List<MonthComparisonPaymentSource> = emptyList(),
    val tipos_despesa: List<MonthComparisonExpenseType> = emptyList(),
    val insights: List<String> = emptyList()
)

@Serializable
data class MonthComparisonSummary(
    val receitas_atual: Double,
    val receitas_comparado: Double,
    val diferenca_receitas: Double,
    val percentual_receitas: Double,
    val status_receitas: String,
    val despesas_atual: Double,
    val despesas_comparado: Double,
    val diferenca_despesas: Double,
    val percentual_despesas: Double,
    val status_despesas: String,
    val saldo_atual: Double,
    val saldo_comparado: Double,
    val diferenca_saldo: Double,
    val percentual_saldo: Double,
    val status_saldo: String
)

@Serializable
data class MonthComparisonCategory(
    val categoria_id: Int,
    val categoria_nome: String,
    val valor_atual: Double,
    val valor_comparado: Double,
    val diferenca: Double,
    val percentual: Double,
    val status: String
)

@Serializable
data class MonthComparisonPaymentSource(
    val fonte_pagamento: String,
    val valor_atual: Double,
    val valor_comparado: Double,
    val diferenca: Double,
    val percentual: Double,
    val status: String
)

@Serializable
data class MonthComparisonExpenseType(
    val tipo: String,
    val valor_atual: Double,
    val valor_comparado: Double,
    val diferenca: Double,
    val percentual: Double,
    val status: String
)

@Serializable
data class InstallmentCommitmentsResponse(
    val mes_base: Int,
    val ano_base: Int,
    val meses: Int,
    val resumo: InstallmentCommitmentsSummary,
    val compras: List<InstallmentPurchase>,
    val linha_do_tempo: List<InstallmentTimelineMonth>
)

@Serializable
data class InstallmentCommitmentsSummary(
    val total_original: Double,
    val total_pago: Double,
    val total_restante: Double,
    val parcelas_pagas: Int,
    val parcelas_restantes: Int,
    val total_compras: Int,
    val mes_mais_pesado: InstallmentHeavyMonth? = null
)

@Serializable
data class InstallmentHeavyMonth(
    val mes: Int,
    val ano: Int,
    val total: Double
)

@Serializable
data class InstallmentPurchase(
    val serie_id: String,
    val descricao: String,
    val categoria_id: Int,
    val categoria_nome: String,
    val fonte_pagamento: String,
    val valor_parcela: Double,
    val total_original: Double,
    val total_pago: Double,
    val total_restante: Double,
    val parcelas_pagas: Int,
    val parcelas_restantes: Int,
    val total_parcelas: Int,
    val primeiro_mes: Int,
    val primeiro_ano: Int,
    val ultimo_mes: Int,
    val ultimo_ano: Int,
    val proxima_parcela: InstallmentParcel? = null
)

@Serializable
data class InstallmentParcel(
    val id: Int,
    val serie_id: String,
    val descricao: String,
    val categoria_id: Int,
    val categoria_nome: String,
    val fonte_pagamento: String,
    val valor: Double,
    val mes: Int,
    val ano: Int,
    val parcela_atual: Int,
    val total_parcelas: Int
)

@Serializable
data class InstallmentTimelineMonth(
    val mes: Int,
    val ano: Int,
    val total: Double,
    val parcelas: List<InstallmentParcel>
)

