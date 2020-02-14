package com.diamonds.webserver

import com.diamonds.flows.MineDiamondFlow
import com.diamonds.states.DiamondState
import net.corda.core.utilities.getOrThrow
import net.corda.finance.DOLLARS
import org.slf4j.LoggerFactory
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * Define your API endpoints here.
 */
@RestController
@RequestMapping("/diamonds") // The paths for HTTP requests are relative to this base path.
class Controller(rpc: NodeRPCConnection) {

    companion object {
        private val logger = LoggerFactory.getLogger(RestController::class.java)
    }

    private val proxy = rpc.proxy

    @GetMapping(value = ["/mine"], produces = ["Application/json"])
    private fun mine(): HashMap<String, String> {
        val signedTransaction = proxy.startFlowDynamic(MineDiamondFlow::class.java, 100.DOLLARS).returnValue.getOrThrow()

        val diamond = signedTransaction.tx.outputStates.single() as DiamondState

        return hashMapOf(
                "owner" to diamond.owner.toString(),
                "value" to diamond.value.toString(),
                "linearId" to diamond.linearId.toString(),
                "state" to diamond.state.toString()
        )
    }
}
