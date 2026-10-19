package com.example.appfinanceiro.core.network

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.*

val FinanceJson = Json {
    ignoreUnknownKeys = true
    explicitNulls = false
    encodeDefaults = true
}

// Somente os campos de apresentação toleram os nulos do JSON legado.
object LegacyTextSerializer : KSerializer<String> {
    override val descriptor: SerialDescriptor = String.serializer().descriptor
    override fun deserialize(decoder: Decoder): String {
        val element = (decoder as JsonDecoder).decodeJsonElement()
        return if (element === JsonNull) "" else element.jsonPrimitive.content
    }
    override fun serialize(encoder: Encoder, value: String) = encoder.encodeString(value)
}

object LegacyPaymentSplitsSerializer : KSerializer<List<PaymentSplit>> {
    private val delegate = ListSerializer(PaymentSplit.serializer())
    override val descriptor: SerialDescriptor = delegate.descriptor
    override fun deserialize(decoder: Decoder): List<PaymentSplit> {
        val input = decoder as JsonDecoder
        val element = input.decodeJsonElement()
        if (element === JsonNull) return emptyList()
        if (element !is JsonArray) throw SerializationException("Rateio inválido")
        return element.filter { it !== JsonNull }.map {
            input.json.decodeFromJsonElement(PaymentSplit.serializer(), it)
        }
    }
    override fun serialize(encoder: Encoder, value: List<PaymentSplit>) = delegate.serialize(encoder, value)
}
