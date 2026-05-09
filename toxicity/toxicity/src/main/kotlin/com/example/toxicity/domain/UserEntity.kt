package com.example.toxicity.domain

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant

@Entity
@Table(name = "users")
class UserEntity(
    @Column(nullable = false, unique = true, length = 50)
    var username: String,

    @Column(nullable = false, length = 255)
    var passwordHash: String,

    @Column(nullable = false, length = 100)
    var displayName: String,

    @Column(columnDefinition = "TEXT")
    var bio: String? = null,

    @Column(nullable = false)
    var createdAt: Instant = Instant.now(),

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,
)
