package com.diamonds.contracts

import com.diamonds.states.DiamondState
import net.corda.core.contracts.TypeOnlyCommandData
import net.corda.testing.node.MockServices
import net.corda.testing.node.ledger
import org.junit.Test

class ContractTests {
    private val ledgerServices = MockServices(listOf("com.diamonds.contracts"))

    val diamond = DiamondState(owner = ALICE.party)
    @Test
    fun shouldOnlyWorkWithTheCorrectCommandOptions() {
        class DummyCommand : TypeOnlyCommandData()
        ledgerServices.ledger {
            transaction {
                output(DiamondContract.ID, diamond)
                command(listOf(ALICE.publicKey), DiamondContract.Commands.Mine())
                this.verifies()
            }
            transaction {
                output(DiamondContract.ID, diamond)
                command(listOf(ALICE.publicKey), DummyCommand())
                this.fails()
            }
        }
    }

    @Test
    fun miningShouldOnlyHave0InputsAndOnly1Output() {
        ledgerServices.ledger {
            transaction {
                output(DiamondContract.ID, diamond)
                command(listOf(ALICE.publicKey), DiamondContract.Commands.Mine())
                this.verifies()
            }
            transaction {
                output(DiamondContract.ID, diamond)
                input(DiamondContract.ID, diamond)
                command(listOf(ALICE.publicKey), DiamondContract.Commands.Mine())
                this.fails()
            }
        }
    }
}