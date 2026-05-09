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
data class FollowId(
    @Column(name = "follower_id")
    var followerId: Long = 0,
    @Column(name = "followee_id")
    var followeeId: Long = 0,
) : Serializable

@Entity
@Table(
    name = "follows",
    indexes = [
        Index(name = "idx_follows_followee", columnList = "followee_id"),
    ]
)
class FollowEntity(
    @EmbeddedId
    var id: FollowId,

    @Column(nullable = false)
    var createdAt: Instant = Instant.now(),
)
