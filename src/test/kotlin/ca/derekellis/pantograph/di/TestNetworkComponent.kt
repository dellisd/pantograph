package ca.derekellis.pantograph.di

import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import me.tatarka.inject.annotations.Component

@Component
abstract class TestNetworkComponent(private val mockBlock: suspend MockRequestHandleScope.(HttpRequestData) -> HttpResponseData) :
    NetworkComponent() {
    override fun httpClientEngine(): HttpClientEngine = MockEngine(mockBlock)
}
