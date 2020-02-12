package com.diamonds

import net.corda.core.contracts.Amount
import net.corda.finance.`issued by`
import net.corda.finance.contracts.asset.Cash
import net.corda.testing.core.TestIdentity
import java.util.*

//TODO: workout how to move this into a seperate package so that it can be utilized by both contract tests
// and workflow tests.
fun createCashState(owner: TestIdentity, issuer: TestIdentity, amount: Amount<Currency>): Cash.State {
    val defaultRef = ByteArray(1) { 1 }
    return Cash.State(amount = amount `issued by` issuer.ref(defaultRef.first()), owner = owner.party)
}