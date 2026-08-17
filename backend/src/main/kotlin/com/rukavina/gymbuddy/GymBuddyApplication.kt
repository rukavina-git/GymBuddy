package com.rukavina.gymbuddy

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableScheduling

@SpringBootApplication
@EnableScheduling
class GymBuddyApplication

fun main(args: Array<String>) {
    runApplication<GymBuddyApplication>(*args)
}
