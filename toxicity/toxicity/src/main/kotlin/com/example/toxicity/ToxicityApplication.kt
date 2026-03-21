package com.example.toxicity

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class ToxicityApplication

fun main(args: Array<String>) {
	runApplication<ToxicityApplication>(*args)
}
