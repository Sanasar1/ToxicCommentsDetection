package com.example.toxicity.service

import com.example.toxicity.domain.FollowEntity
import com.example.toxicity.domain.FollowId
import com.example.toxicity.repository.FollowRepository
import com.example.toxicity.repository.UserRepository
import com.example.toxicity.security.CurrentUser
import com.example.toxicity.web.BadRequestException
import com.example.toxicity.web.NotFoundException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class FollowService(
    private val followRepository: FollowRepository,
    private val userRepository: UserRepository,
    private val feedService: FeedService,
) {

    @Transactional
    fun follow(targetUsername: String) {
        val me = CurrentUser.id()
        val target = userRepository.findByUsername(targetUsername)
            ?: throw NotFoundException("User not found")
        if (target.id == me) throw BadRequestException("Cannot follow yourself")
        val id = FollowId(followerId = me, followeeId = target.id!!)
        if (followRepository.existsById(id)) return
        followRepository.save(FollowEntity(id = id))
        feedService.onFollow(followerId = me, followeeId = target.id!!)
    }

    @Transactional
    fun unfollow(targetUsername: String) {
        val me = CurrentUser.id()
        val target = userRepository.findByUsername(targetUsername)
            ?: throw NotFoundException("User not found")
        val id = FollowId(followerId = me, followeeId = target.id!!)
        if (followRepository.existsById(id)) {
            followRepository.deleteById(id)
            feedService.onUnfollow(followerId = me, followeeId = target.id!!)
        }
    }
}
