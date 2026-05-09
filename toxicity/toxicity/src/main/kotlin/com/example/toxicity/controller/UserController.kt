package com.example.toxicity.controller

import com.example.toxicity.dto.PostDto
import com.example.toxicity.dto.ProfileDto
import com.example.toxicity.dto.UpdateProfileRequest
import com.example.toxicity.dto.UserDto
import com.example.toxicity.service.FollowService
import com.example.toxicity.service.PostService
import com.example.toxicity.service.UserService
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/users")
class UserController(
    private val userService: UserService,
    private val postService: PostService,
    private val followService: FollowService,
) {

    @GetMapping("/{username}")
    fun profile(@PathVariable username: String): ProfileDto = userService.getProfile(username)

    @GetMapping("/{username}/posts")
    fun userPosts(
        @PathVariable username: String,
        @RequestParam(defaultValue = "20") limit: Int,
        @RequestParam(defaultValue = "0") offset: Int,
    ): List<PostDto> = postService.listByUsername(username, limit, offset)

    @PutMapping("/me")
    fun updateMe(@RequestBody req: UpdateProfileRequest): UserDto = userService.updateProfile(req)

    @PostMapping("/{username}/follow")
    fun follow(@PathVariable username: String) = followService.follow(username)

    @DeleteMapping("/{username}/follow")
    fun unfollow(@PathVariable username: String) = followService.unfollow(username)
}
