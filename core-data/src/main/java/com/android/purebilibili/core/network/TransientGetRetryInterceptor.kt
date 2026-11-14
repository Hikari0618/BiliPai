package com.android.purebilibili.core.network

import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException
import java.net.ConnectException
import java.net.SocketException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.net.ssl.SSLException

/**
 * 全局 GET/HEAD 瞬时失败重试：仅幂等请求、仅连接类错误与超时，2 次 × 500ms 线性递增。
 *
 * POST 等写请求不重试——connectionError 时请求可能已到达服务器，自动重试会重复执行
 * （点赞/投币类操作不可接受）。HTTP 错误状态码与业务错误码也不重试，只处理未建立
 * 或中断的连接；SSL 握手失败通常是持续性故障，同样不重试。
 */
object TransientGetRetryInterceptor : Interceptor {
    private const val MAX_RETRIES = 2
    private const val BASE_DELAY_MS = 500L

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        if (request.method != "GET" && request.method != "HEAD") {
            return chain.proceed(request)
        }
        var attempt = 0
        while (true) {
            try {
                return chain.proceed(request)
            } catch (e: IOException) {
                if (chain.call().isCanceled() ||
                    attempt >= MAX_RETRIES ||
                    !isTransientConnectivityFailure(e)
                ) {
                    throw e
                }
                attempt++
                try {
                    Thread.sleep(BASE_DELAY_MS * attempt)
                } catch (interrupted: InterruptedException) {
                    Thread.currentThread().interrupt()
                    throw e
                }
            }
        }
    }

    private fun isTransientConnectivityFailure(e: IOException): Boolean = when (e) {
        is SSLException -> false
        is SocketTimeoutException -> true
        is ConnectException -> true
        is UnknownHostException -> true
        is SocketException -> true
        else -> false
    }
}
