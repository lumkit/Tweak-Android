package io.github.lumkit.tweak.common.status

import io.ktor.http.HttpStatusCode

class BackendException(
    override val message: String,
    val status: HttpStatusCode = HttpStatusCode.InternalServerError,
): RuntimeException(message)