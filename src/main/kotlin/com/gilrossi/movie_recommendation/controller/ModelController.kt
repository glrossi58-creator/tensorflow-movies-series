package com.gilrossi.movie_recommendation.controller

import com.gilrossi.movie_recommendation.recommendation.ModelLifecycleService
import org.springframework.web.bind.annotation.*

data class ModelSettings(val autoTrainEnabled: Boolean)
@RestController
@RequestMapping("/users/{userId}/model")
class ModelController(private val models: ModelLifecycleService) {
    @GetMapping("/status") suspend fun status(@PathVariable userId: Long) = models.status(userId)
    @PutMapping("/settings") suspend fun settings(@PathVariable userId: Long, @RequestBody settings: ModelSettings) = models.settings(userId, settings.autoTrainEnabled)
}
