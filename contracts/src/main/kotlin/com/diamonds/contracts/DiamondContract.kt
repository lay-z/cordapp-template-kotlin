package com.diamonds.contracts

import com.diamonds.states.DiamondState
import com.diamonds.states.DiamondType
import net.corda.core.contracts.*
import net.corda.core.transactions.LedgerTransaction
import java.security.PublicKey
import net.corda.finance.contracts.asset.Cash

// ************
// * Contract *
// ************
class DiamondContract : Contract {
    companion object {
        // Used to identify our contract when building a transaction.
        const val ID = "com.diamonds.contracts.DiamondContract"
    }

    // A transaction is valid if the verify() function of the contract of all the transaction's input and output states
    // does not throw an exception.
    override fun verify(tx: LedgerTransaction) {
        // Make sure that there's only one command. and that the type is of DiamondContract.Commands
        // Throws an error otherwise
        println("THIS IS THE COMMANDS ${tx.commands}")
        val command = tx.commands.requireSingleCommand<Commands>()
        // Verification logic goes here.
        when (command.value) {
            is Commands.Mine -> {
                val outputDiamond = tx.outputsOfType<DiamondState>()
                requireThat {
                    "No inputs should be present" using (tx.inputs.size == 0)
                    "Expected at least one or more diamond outputs" using (outputDiamond.size > 0)
                    // TODO: Should we make sure no dodgy diamond states are in? "Expected only diamond objects in outputs"
                    "Diamond Owners must have signed transaction" using (command.signers.toSet().containsAll(
                            outputDiamond.fold(listOf<PublicKey>()) { participants, diamond -> participants + diamond.participants.map { it.owningKey } }))
                    "Diamond output state must be of type ROUGH" using (outputDiamond.all { it.state == DiamondType.ROUGH })
                }
            }
            is Commands.Sell -> {
                requireThat {
                    val outputDiamonds = tx.outputsOfType<DiamondState>()
                    val outputCash = tx.outputsOfType<Cash.State>()
                    val inputDiamonds = tx.inputsOfType<DiamondState>()
                    val inputCash = tx.inputsOfType<Cash.State>()
                    "Different number of diamonds inputs and outputs. Inputs: ${inputDiamonds.size}, outputs ${outputDiamonds.size}" using (inputDiamonds.size == outputDiamonds.size)
                    "Incorrect number of cash states detected. Should have ${inputDiamonds.size}, instead have ${outputCash.size}" using (inputDiamonds.size == outputCash.size)
                    for (i in outputDiamonds.indices) {
                        val outputDiamond = outputDiamonds[i]
                        val inputDiamond = inputDiamonds[i]
                        val inputCashState = inputCash[i]
                        val outputCashState = outputCash[i]
                        "Required cash for transfer not sent. Expected ${inputDiamond.value} but received ${inputCashState.amount.withoutIssuer()}" using (inputDiamond.value == inputCashState.amount.withoutIssuer())
                        "Only owner field is allowed to change" using (outputDiamond == inputDiamond.copy(owner = outputDiamond.owner))
                        "Cash state is not being transferred to correct party" using (inputCashState.owner == outputDiamond.owner && outputCashState.owner == inputDiamond.owner)
                    }
                }
            }
            is Commands.Cut -> {

            }
            is Commands.Polish -> {

            }
        }
    }

    // Used to indicate the transaction's intent.
    interface Commands : CommandData {
        class Mine : Commands
        class Sell : Commands
        class Cut : Commands
        class Polish : Commands
    }
}