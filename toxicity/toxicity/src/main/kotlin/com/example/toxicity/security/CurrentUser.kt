package com.example.toxicity.security

object CurrentUser {
    private val holder = ThreadLocal<Long?>()

    fun set(userId: Long?) = holder.set(userId)
    fun clear() = holder.remove()
    fun idOrNull(): Long? = holder.get()
    fun id(): Long = holder.get() ?: throw UnauthorizedException("Authentication required")
}

class UnauthorizedException(message: String) : RuntimeException(message)
