package com.diamonds.states

import com.diamonds.contracts.DiamondContract
import net.corda.core.contracts.*
import net.corda.core.identity.AbstractParty
import net.corda.core.serialization.CordaSerializable
import java.util.*

@CordaSerializable
enum class DiamondType {
    ROUGH, CUT, POLISHED
}

// *********
// * State *
// *********
@BelongsToContract(DiamondContract::class)
data class DiamondState(
        val state: DiamondType = DiamondType.ROUGH,
        val value: Amount<Currency>,
        override val owner: AbstractParty,
        override val linearId: UniqueIdentifier = UniqueIdentifier()) : OwnableState, LinearState {

    override val participants: List<AbstractParty> get() = listOf(owner)

    override fun withNewOwner(newOwner: net.corda.core.identity.AbstractParty): net.corda.core.contracts.CommandAndState {
        return CommandAndState(DiamondContract.Commands.Sell(), this.copy(owner = newOwner))
    }
}

