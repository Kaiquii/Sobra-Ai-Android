@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class, kotlinx.cinterop.BetaInteropApi::class)

package com.example.appfinanceiro.core.data

import kotlinx.cinterop.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import platform.CoreFoundation.*
import platform.Foundation.NSData
import platform.Foundation.NSUserDefaults
import platform.Foundation.create
import platform.Foundation.CFBridgingRetain
import platform.Foundation.CFBridgingRelease
import platform.Security.*

class IosSessionStore(service: String) : SessionStore by PersistentSessionStore(
    SecureSessionPersistence(KeychainSessionStorage(service), AppleBiometricPreferences(service))
)

private class AppleBiometricPreferences(service: String) : BiometricPreferenceStorage {
    private val defaults = NSUserDefaults.standardUserDefaults
    private val key = "$service.biometric_enabled_emails"
    override fun read(): Set<String> = defaults.stringArrayForKey(key)?.filterIsInstance<String>()?.toSet().orEmpty()
    override fun write(emails: Set<String>) { defaults.setObject(emails.toList(), forKey = key) }
}

private class KeychainSessionStorage(private val service: String) : SecureSessionStorage {
    init { require(service.isNotBlank()) }
    private fun <T> dictionary(values: Map<CFStringRef?, CFTypeRef?>, block: (CFDictionaryRef) -> T): T = memScoped {
        val keys = allocArrayOf(*values.keys.toTypedArray())
        val entries = allocArrayOf(*values.values.toTypedArray())
        val reference = CFDictionaryCreate(null, keys.reinterpret(), entries.reinterpret(), values.size.convert(), null, null)
            ?: error("Não foi possível preparar o Keychain.")
        try { block(reference) } finally { CFRelease(reference) }
    }
    private fun <T> query(extra: Map<CFStringRef?, CFTypeRef?> = emptyMap(), block: (CFDictionaryRef) -> T): T {
        val serviceRef = CFBridgingRetain(service)
        val accountRef = CFBridgingRetain("user_session")
        try {
            return dictionary(mapOf(kSecClass to kSecClassGenericPassword,
                kSecAttrService to serviceRef, kSecAttrAccount to accountRef,
                kSecAttrSynchronizable to kCFBooleanFalse) + extra, block)
        } finally {
            if (serviceRef != null) CFRelease(serviceRef)
            if (accountRef != null) CFRelease(accountRef)
        }
    }
    override suspend fun read(): String? = withContext(Dispatchers.Default) {
        query(mapOf(kSecReturnData to kCFBooleanTrue, kSecMatchLimit to kSecMatchLimitOne)) { query ->
            memScoped {
                val result = alloc<CFTypeRefVar>()
                result.value = null
                when (val status = SecItemCopyMatching(query, result.ptr)) {
                    errSecItemNotFound -> null
                    errSecSuccess -> {
                        val data = CFBridgingRelease(result.value) as? NSData ?: error("Sessão inválida no Keychain.")
                        data.bytes?.reinterpret<ByteVar>()?.readBytes(data.length.toInt())?.decodeToString()
                            ?: error("Sessão vazia no Keychain.")
                    }
                    else -> error("Não foi possível ler a sessão no Keychain ($status).")
                }
            }
        }
    }
    override suspend fun write(value: String) = withContext(Dispatchers.Default) {
        val bytes = value.encodeToByteArray()
        val data = bytes.usePinned { NSData.create(bytes = it.addressOf(0), length = bytes.size.toULong()) }
        val dataRef = CFBridgingRetain(data)
        try {
            query { query ->
                val status = dictionary(mapOf(kSecValueData to dataRef)) { changes -> SecItemUpdate(query, changes) }
                if (status == errSecItemNotFound) {
                    val added = this@KeychainSessionStorage.query(mapOf(
                        kSecValueData to dataRef, kSecAttrAccessible to kSecAttrAccessibleWhenUnlockedThisDeviceOnly
                    )) { SecItemAdd(it, null) }
                    check(added == errSecSuccess) { "Não foi possível salvar a sessão no Keychain ($added)." }
                } else check(status == errSecSuccess) { "Não foi possível atualizar a sessão no Keychain ($status)." }
            }
        } finally {
            if (dataRef != null) CFRelease(dataRef)
        }
    }
    override suspend fun remove() = withContext(Dispatchers.Default) {
        val status = query { SecItemDelete(it) }
        check(status == errSecSuccess || status == errSecItemNotFound) { "Não foi possível remover a sessão do Keychain ($status)." }
    }
}
