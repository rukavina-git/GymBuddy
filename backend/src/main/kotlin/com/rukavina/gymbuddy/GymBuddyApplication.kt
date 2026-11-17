package com.rukavina.gymbuddy

import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableScheduling
import kotlin.system.exitProcess

@SpringBootApplication
@EnableScheduling
class GymBuddyApplication

fun main(args: Array<String>) {
    val context = runApplication<GymBuddyApplication>(*args)
    // One-off job (sync/AccountDeleteRunner): the scheduler's threads
    // would otherwise keep the JVM alive after the runner is done.
    if (context.environment.matchesProfiles("account-delete")) {
        exitProcess(SpringApplication.exit(context))
    }
}
