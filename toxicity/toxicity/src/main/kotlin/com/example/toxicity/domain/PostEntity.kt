package com.example.toxicity.domain

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Table
import java.time.Instant

@Entity
@Table(
    name = "posts",
    indexes = [
        Index(name = "idx_posts_author", columnList = "author_id"),
        Index(name = "idx_posts_created_at", columnList = "createdAt"),
    ]
)
class PostEntity(
    @Column(name = "author_id", nullable = false)
    var authorId: Long,

    @Column(nullable = false, columnDefinition = "TEXT")
    var content: String,

    @Column(nullable = false)
    var createdAt: Instant = Instant.now(),

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,
)
