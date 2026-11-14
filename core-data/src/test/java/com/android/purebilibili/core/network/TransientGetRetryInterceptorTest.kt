package com.android.purebilibili.core.network

import okhttp3.Call
import okhttp3.Connection
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import javax.net.ssl.SSLException

class TransientGetRetryInterceptorTest {
    private class FakeChain(
        private val request: Request,
        private val behavior: (attempt: Int) -> Response,
    ) : Interceptor.Chain {
        val calls = AtomicInteger()
        private val call: Call = OkHttpClient().newCall(request)

        override fun request(): Request = request

        override fun proceed(request: Request): Response {
            val attempt = calls.incrementAndGet()
            return behavior(attempt)
        }

        override fun connection(): Connection? = null

        override fun call(): Call = call

        override fun connectTimeoutMillis(): Int = 10_000

        override fun withConnectTimeout(timeout: Int, unit: TimeUnit): Chain = this

        override fun readTimeoutMillis(): Int = 10_000

        override fun withReadTimeout(timeout: Int, unit: TimeUnit): Chain = this

        override fun writeTimeoutMillis(): Int = 10_000

        override fun withWriteTimeout(timeout: Int, unit: TimeUnit): Chain = this
    }

    private fun okResponse(request: Request): Response = Response.Builder()
        .request(request)
        .protocol(Protocol.HTTP_1_1)
        .code(200)
        .message("OK")
        .body("{}".toResponseBody(null))
        .build()

    private fun getRequest(): Request = Request.Builder().url("https://api.bilibili.com/x/web-interface/view").build()

    @Test
    fun getRetriesTransientTimeoutsUpToTwoRetriesThenSucceeds() {
        val chain = FakeChain(getRequest()) { attempt ->
            if (attempt <= 2) throw SocketTimeoutException("read timeout")
            okResponse(getRequest())
        }

        val response = TransientGetRetryInterceptor.intercept(chain)

        assertEquals(200, response.code)
        // 首次 + 2 次重试后成功，不再多打。
        assertEquals(3, chain.calls.get())
    }

    @Test
    fun getStopsAtRetryCapAndSurfacesLastFailure() {
        val chain = FakeChain(getRequest()) { _ ->
            throw ConnectException("connection refused")
        }

        assertThrows(ConnectException::class.java) {
            TransientGetRetryInterceptor.intercept(chain)
        }

        assertEquals(3, chain.calls.get())
    }

    @Test
    fun postIsNeverRetried() {
        val post = Request.Builder()
            .url("https://app.bilibili.com/x/v2/view/coin/add")
            .post(okhttp3.RequestBody.create(null, "aid=1"))
            .build()
        val chain = FakeChain(post) { _ ->
            throw SocketTimeoutException("read timeout")
        }

        assertThrows(SocketTimeoutException::class.java) {
            TransientGetRetryInterceptor.intercept(chain)
        }

        assertEquals(1, chain.calls.get())
    }

    @Test
    fun sslHandshakeFailuresAreNotRetried() {
        val chain = FakeChain(getRequest()) { _ ->
            throw SSLException("handshake failed")
        }

        assertThrows(SSLException::class.java) {
            TransientGetRetryInterceptor.intercept(chain)
        }

        assertEquals(1, chain.calls.get())
    }

    @Test
    fun nonIoFailuresAreNotRetried() {
        val chain = FakeChain(getRequest()) { _ ->
            throw IllegalStateException("not an io failure")
        }

        assertThrows(IllegalStateException::class.java) {
            TransientGetRetryInterceptor.intercept(chain)
        }

        assertEquals(1, chain.calls.get())
    }
}
