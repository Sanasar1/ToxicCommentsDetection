package com.example.toxicity.service

import com.example.toxicity.repository.FollowRepository
import com.example.toxicity.repository.PostRepository
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.domain.PageRequest
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Service

@Service
class FeedService(
    private val redis: StringRedisTemplate,
    private val followRepository: FollowRepository,
    private val postRepository: PostRepository,
    @Value("\${feed.fanout-cap}") private val fanoutCap: Long,
) {
    private val log = LoggerFactory.getLogger(FeedService::class.java)

    private fun key(userId: Long) = "feed:$userId"

    fun onPostCreated(authorId: Long, postId: Long, createdAtEpochSecond: Long) {
        val score = createdAtEpochSecond.toDouble()
        val recipients = followRepository.findFollowerIds(authorId).toMutableSet()
        recipients.add(authorId)
        runCatching {
            recipients.forEach { uid ->
                val k = key(uid)
                redis.opsForZSet().add(k, postId.toString(), score)
                trimFeed(k)
            }
        }.onFailure { log.warn("Redis fan-out failed: {}", it.message) }
    }

    fun onPostDeleted(authorId: Long, postId: Long) {
        val recipients = followRepository.findFollowerIds(authorId).toMutableSet()
        recipients.add(authorId)
        runCatching {
            recipients.forEach { uid -> redis.opsForZSet().remove(key(uid), postId.toString()) }
        }.onFailure { log.warn("Redis cleanup failed: {}", it.message) }
    }

    fun onFollow(followerId: Long, followeeId: Long) {
        val recent = postRepository.findAllByAuthorIdOrderByCreatedAtDesc(followeeId, PageRequest.of(0, 50))
        if (recent.isEmpty()) return
        val k = key(followerId)
        runCatching {
            recent.forEach { p ->
                redis.opsForZSet().add(k, p.id!!.toString(), p.createdAt.epochSecond.toDouble())
            }
            trimFeed(k)
        }.onFailure { log.warn("Redis backfill failed: {}", it.message) }
    }

    fun onUnfollow(followerId: Long, followeeId: Long) {
        val recent = postRepository.findAllByAuthorIdOrderByCreatedAtDesc(followeeId, PageRequest.of(0, fanoutCap.toInt()))
        if (recent.isEmpty()) return
        val k = key(followerId)
        runCatching {
            recent.forEach { p -> redis.opsForZSet().remove(k, p.id!!.toString()) }
        }.onFailure { log.warn("Redis unfollow cleanup failed: {}", it.message) }
    }

    fun feedPostIds(userId: Long, limit: Int, beforeEpochSecond: Long?): List<Long> {
        val k = key(userId)
        val cached = runCatching {
            val max = beforeEpochSecond?.let { (it - 1).toDouble() } ?: Double.POSITIVE_INFINITY
            redis.opsForZSet().reverseRangeByScore(k, Double.NEGATIVE_INFINITY, max, 0, limit.toLong())
        }.getOrNull()
        if (!cached.isNullOrEmpty()) {
            return cached.mapNotNull { it.toLongOrNull() }
        }
        return loadFromDbAndRehydrate(userId, limit, beforeEpochSecond)
    }

    private fun loadFromDbAndRehydrate(userId: Long, limit: Int, beforeEpochSecond: Long?): List<Long> {
        val followeeIds = followRepository.findFolloweeIds(userId).toMutableList()
        followeeIds.add(userId)
        val posts = postRepository.findAllByAuthorIdInOrderByCreatedAtDesc(
            followeeIds,
            PageRequest.of(0, fanoutCap.toInt())
        )
        runCatching {
            val k = key(userId)
            redis.delete(k)
            posts.forEach { p ->
                redis.opsForZSet().add(k, p.id!!.toString(), p.createdAt.epochSecond.toDouble())
            }
            trimFeed(k)
        }
        return posts.asSequence()
            .filter { beforeEpochSecond == null || it.createdAt.epochSecond < beforeEpochSecond }
            .take(limit)
            .map { it.id!! }
            .toList()
    }

    private fun trimFeed(k: String) {
        val size = redis.opsForZSet().zCard(k) ?: 0
        if (size > fanoutCap) {
            redis.opsForZSet().removeRange(k, 0, size - fanoutCap - 1)
        }
    }
}
