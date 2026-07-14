package com.example.appfinanceiro.feature.despesas.components

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.appfinanceiro.core.data.SessionManager
import com.example.appfinanceiro.core.designsystem.components.AppDatePickerDialog
import com.example.appfinanceiro.core.designsystem.theme.PrimaryBlue
import com.example.appfinanceiro.core.network.Category
import com.example.appfinanceiro.core.network.ExpenseUpdateRequest
import com.example.appfinanceiro.core.network.parseApiErrorMessage
import com.example.appfinanceiro.core.network.auth.RetrofitClient
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditarDespesaScreen(expenseId: Int, onNavigateBack: () -> Unit) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val userToken by remember { SessionManager(context) }.token.collectAsState(initial = null)
    val colorScheme = MaterialTheme.colorScheme

    val backgroundColor = colorScheme.background
    val inputBgColor = colorScheme.surface
    val dialogBackgroundColor = colorScheme.background
    val dialogTextColor = colorScheme.onBackground

    var amountText by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var categories by remember { mutableStateOf<List<Category>>(emptyList()) }
    var selectedCategory by remember { mutableStateOf<Category?>(null) }
    var expandedCategory by remember { mutableStateOf(false) }

    val sources = listOf("Salário", "Adiantamento", "Renda Extra")
    var selectedSource by remember { mutableStateOf(sources[0]) }
    var expandedSource by remember { mutableStateOf(false) }

    val calendar = remember { Calendar.getInstance() }
    val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'00:00:00XXX", Locale.getDefault())
    val displayFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

    var dateText by remember { mutableStateOf("") }
    var showDatePicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState()

    var originalType by remember { mutableStateOf("Única") }
    var showFixedExpenseConfirmation by remember { mutableStateOf(false) }

    var updateFuture by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(userToken, expenseId) {
        if (userToken != null) {
            try {
                categories = RetrofitClient.financeApi.getCategories("Bearer $userToken").categories
                val expense = RetrofitClient.financeApi.getExpenseById("Bearer $userToken", expenseId)

                amountText = expense.amount.toString().replace(".", ",")
                description = expense.description
                notes = expense.notes.orEmpty()
                selectedCategory = categories.find { it.id == expense.category_id }

                if (sources.contains(expense.payment_source)) {
                    selectedSource = expense.payment_source ?: sources[0]
                }

                originalType = expense.type ?: "Única"

                try {
                    val apiParser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.getDefault())
                    val parsedDate = apiParser.parse(expense.date)
                    if (parsedDate != null) {
                        calendar.time = parsedDate
                        dateText = displayFormat.format(parsedDate)
                    }
                } catch (e: Exception) {
                    Log.e("API_DATE", "Erro ao converter a data", e)
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Erro ao carregar despesa", Toast.LENGTH_SHORT).show()
                onNavigateBack()
            } finally {
                isLoading = false
            }
        }
    }

    if (showDatePicker) {
        AppDatePickerDialog(
            state = datePickerState,
            onConfirm = { selectedDateMillis ->
                selectedDateMillis?.let { millis ->
                    val selectedCal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
                        timeInMillis = millis
                    }
                    calendar.set(
                        selectedCal.get(Calendar.YEAR),
                        selectedCal.get(Calendar.MONTH),
                        selectedCal.get(Calendar.DAY_OF_MONTH)
                    )
                    dateText = displayFormat.format(calendar.time)
                }
                showDatePicker = false
            },
            onDismiss = { showDatePicker = false }
        )
    }

    Scaffold(
        containerColor = backgroundColor,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Editar Despesa",
                        color = colorScheme.onBackground,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Fechar",
                            tint = colorScheme.onBackground
                        )
                    }
                },
                actions = {
                    Spacer(modifier = Modifier.height(48.dp))
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = backgroundColor)
            )
        }
    ) { paddingValues ->
        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = PrimaryBlue)
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 24.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(16.dp))

                ExpenseValueInput(
                    amountText = amountText,
                    onAmountChange = { amountText = it }
                )

                Spacer(modifier = Modifier.height(32.dp))

                FormLabel("Descrição")
                CustomInput(
                    value = description,
                    onValueChange = { description = it },
                    icon = Icons.Default.Description,
                    placeholder = "Ex: Supermercado",
                    bgColor = inputBgColor
                )

                CustomDropdown(
                    label = "Categoria",
                    selectedValue = selectedCategory?.name ?: "Selecione",
                    options = categories.map { it.name },
                    expanded = expandedCategory,
                    onExpandedChange = { expandedCategory = it }
                ) {
                    selectedCategory = categories[it]
                    expandedCategory = false
                }

                Spacer(modifier = Modifier.height(16.dp))

                CustomDropdown(
                    label = "Origem do Pagamento",
                    selectedValue = selectedSource,
                    options = sources,
                    expanded = expandedSource,
                    onExpandedChange = { expandedSource = it }
                ) {
                    selectedSource = sources[it]
                    expandedSource = false
                }

                Spacer(modifier = Modifier.height(16.dp))
                FormLabel("Data de Pagamento")
                CustomInput(
                    value = dateText,
                    onValueChange = {
                        if (it.length <= 10 && it.all { char -> char.isDigit() || char == '/' }) {
                            dateText = it
                        }
                    },
                    icon = null,
                    placeholder = "DD/MM/AAAA",
                    bgColor = inputBgColor,
                    trailingIcon = Icons.Default.CalendarToday,
                    onTrailingIconClick = { showDatePicker = true },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )

                FormLabel("Observações")
                CustomInput(
                    value = notes,
                    onValueChange = {
                        if (it.length <= 500) {
                            notes = it
                        }
                    },
                    icon = null,
                    placeholder = "Detalhes extras da despesa",
                    bgColor = inputBgColor,
                    singleLine = false,
                    minLines = 3,
                    maxLines = 5
                )
                Text(
                    text = "${notes.length}/500",
                    color = colorScheme.onSurfaceVariant,
                    fontSize = 12.sp,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.End
                )

                if (
                    originalType.equals("Parcelada", ignoreCase = true) ||
                    originalType.equals("Fixa", ignoreCase = true)
                ) {
                    Spacer(modifier = Modifier.height(24.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(inputBgColor, RoundedCornerShape(12.dp))
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                if (originalType.equals("Fixa", ignoreCase = true)) {
                                    "Atualizar despesas futuras?"
                                } else {
                                    "Atualizar parcelas futuras?"
                                },
                                color = colorScheme.onSurface,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                if (originalType.equals("Fixa", ignoreCase = true)) {
                                    "Aplica essa edição aos próximos meses desta despesa fixa"
                                } else {
                                    "Aplica essa edição aos próximos meses"
                                },
                                color = colorScheme.onSurfaceVariant,
                                fontSize = 12.sp
                            )
                        }
                        Switch(
                            checked = updateFuture,
                            onCheckedChange = { updateFuture = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = colorScheme.onPrimary,
                                checkedTrackColor = PrimaryBlue
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                Button(
                    onClick = {
                        if (amountText.isEmpty() || description.isEmpty() || selectedCategory == null) {
                            Toast.makeText(context, "Preencha os campos!", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        if (notes.length > 500) {
                            Toast.makeText(context, "Observações devem ter no máximo 500 caracteres.", Toast.LENGTH_SHORT).show()
                            return@Button
                        }

                        if (originalType.equals("Fixa", ignoreCase = true) && updateFuture) {
                            showFixedExpenseConfirmation = true
                            return@Button
                        }

                        isLoading = true
                        coroutineScope.launch {
                            try {
                                val finalAmount = amountText.replace(",", ".").toDoubleOrNull() ?: 0.0
                                val parsedDate = try {
                                    displayFormat.parse(dateText) ?: calendar.time
                                } catch (e: Exception) {
                                    calendar.time
                                }

                                val request = ExpenseUpdateRequest(
                                    amount = finalAmount,
                                    description = description,
                                    category_id = selectedCategory!!.id,
                                    payment_source = selectedSource,
                                    date = dateFormat.format(parsedDate),
                                    notes = notes.trim(),
                                    update_future = if (
                                        originalType.equals("Parcelada", ignoreCase = true) ||
                                        originalType.equals("Fixa", ignoreCase = true)
                                    ) {
                                        updateFuture
                                    } else {
                                        null
                                    }
                                )

                                RetrofitClient.financeApi.updateExpense("Bearer $userToken", expenseId, request)
                                Toast.makeText(context, "Despesa atualizada!", Toast.LENGTH_SHORT).show()
                                onNavigateBack()
                            } catch (e: HttpException) {
                                val apiMessage = parseApiErrorMessage(e.response()?.errorBody()?.string())
                                Toast.makeText(context, apiMessage ?: "Erro ao atualizar", Toast.LENGTH_SHORT).show()
                            } catch (e: Exception) {
                                Toast.makeText(context, "Erro ao atualizar", Toast.LENGTH_SHORT).show()
                            } finally {
                                isLoading = false
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                ) {
                    Text(
                        "Salvar Alterações",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = colorScheme.onPrimary
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))
            }

            if (showFixedExpenseConfirmation) {
                AlertDialog(
                    onDismissRequest = { if (!isLoading) showFixedExpenseConfirmation = false },
                    containerColor = dialogBackgroundColor,
                    titleContentColor = dialogTextColor,
                    textContentColor = dialogTextColor,
                    title = {
                        Text(
                            "Atualizar despesa fixa",
                            fontWeight = FontWeight.Bold
                        )
                    },
                    text = {
                        Text(
                            "Esta alteração será aplicada ao mês atual e aos próximos meses desta despesa fixa."
                        )
                    },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                showFixedExpenseConfirmation = false
                                isLoading = true
                                coroutineScope.launch {
                                    try {
                                        val finalAmount = amountText.replace(",", ".").toDoubleOrNull() ?: 0.0
                                        val parsedDate = try {
                                            displayFormat.parse(dateText) ?: calendar.time
                                        } catch (e: Exception) {
                                            calendar.time
                                        }

                                        val request = ExpenseUpdateRequest(
                                            amount = finalAmount,
                                            description = description,
                                            category_id = selectedCategory!!.id,
                                            payment_source = selectedSource,
                                            date = dateFormat.format(parsedDate),
                                            notes = notes.trim(),
                                            update_future = true
                                        )

                                        RetrofitClient.financeApi.updateExpense("Bearer $userToken", expenseId, request)
                                        Toast.makeText(context, "Despesa atualizada!", Toast.LENGTH_SHORT).show()
                                        onNavigateBack()
                                    } catch (e: HttpException) {
                                        val apiMessage = parseApiErrorMessage(e.response()?.errorBody()?.string())
                                        Toast.makeText(context, apiMessage ?: "Erro ao atualizar", Toast.LENGTH_SHORT).show()
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "Erro ao atualizar", Toast.LENGTH_SHORT).show()
                                    } finally {
                                        isLoading = false
                                    }
                                }
                            },
                            enabled = !isLoading
                        ) {
                            Text(
                                "Confirmar",
                                color = PrimaryBlue,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    },
                    dismissButton = {
                        TextButton(
                            onClick = { showFixedExpenseConfirmation = false },
                            enabled = !isLoading
                        ) {
                            Text(
                                "Cancelar",
                                color = colorScheme.onSurfaceVariant
                            )
                        }
                    }
                )
            }
        }
    }
}
