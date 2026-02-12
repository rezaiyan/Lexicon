package com.alirezaiyan.vokab

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform