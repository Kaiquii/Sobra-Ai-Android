package com.example.appfinanceiro.core.network

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.TypeAdapter
import com.google.gson.TypeAdapterFactory
import com.google.gson.reflect.TypeToken
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter

fun createFinanceGson(): Gson = GsonBuilder()
    .registerTypeAdapterFactory(ExpenseAdapterFactory())
    .create()

// Gson can bypass Kotlin constructors, leaving non-null fields null for legacy JSON.
// Normalize only presentation fields; amounts, identifiers and payment state stay intact.
private class ExpenseAdapterFactory : TypeAdapterFactory {
    override fun <T> create(gson: Gson, type: TypeToken<T>): TypeAdapter<T>? {
        if (type.rawType != Expense::class.java) return null
        val delegate = gson.getDelegateAdapter(this, type)
        return object : TypeAdapter<T>() {
            override fun read(reader: JsonReader): T? {
                val value = delegate.read(reader) ?: return null
                val expense = value as Expense
                @Suppress("UNCHECKED_CAST")
                return expense.copy(
                    description = expense.description.orEmpty(),
                    date = expense.date.orEmpty(),
                    type = expense.type.orEmpty(),
                    payment_splits = expense.payment_splits.orEmpty().filterNotNull().map {
                        it.copy(payment_source = it.payment_source.orEmpty())
                    }
                ) as T
            }

            override fun write(writer: JsonWriter, value: T?) = delegate.write(writer, value)
        }
    }
}
