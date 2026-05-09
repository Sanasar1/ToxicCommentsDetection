package com.example.toxicity.domain

import jakarta.persistence.Column
import jakarta.persistence.Embeddable
import jakarta.persistence.EmbeddedId
import jakarta.persistence.Entity
import jakarta.persistence.Index
import jakarta.persistence.Table
import java.io.Serializable
import java.time.Instant

@Embeddable
data class PostLikeId(
    @Column(name = "post_id")
    var postId: Long = 0,
    @Column(name = "user_id")
    var userId: Long = 0,
) : Serializable

@Entity
@Table(
    name = "post_likes",
    indexes = [
        Index(name = "idx_post_likes_user", columnList = "user_id"),
    ]
)
class PostLikeEntity(
    @EmbeddedId
    var id: PostLikeId,

    @Column(nullable = false)
    var createdAt: Instant = Instant.now(),
)
