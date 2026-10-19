package com.example.appfinanceiro.feature.despesas

fun validateExpenseForm(
    amountText: String,
    description: String,
    categoryId: Int?,
    notes: String,
    requiredFieldsMessage: String
): String? {
    if (amountText.isEmpty() || description.isEmpty() || categoryId == null) {
        return requiredFieldsMessage
    }

    if (notes.length > 500) {
        return "Observações devem ter no máximo 500 caracteres."
    }

    return null
}
