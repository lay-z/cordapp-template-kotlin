package com.diamonds.contracts

import net.corda.core.contracts.CommandData
import net.corda.core.contracts.Contract
import net.corda.core.contracts.requireSingleCommand
import net.corda.core.contracts.requireThat
import net.corda.core.transactions.LedgerTransaction

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
        val command = tx.commands.requireSingleCommand<Commands>()
        // Verification logic goes here.
        when (command.value) {
            is Commands.Mine -> {
                requireThat {
                    "No inputs should be present" using (tx.inputs.size == 0)
                }
            }
            is Commands.Sell -> {

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