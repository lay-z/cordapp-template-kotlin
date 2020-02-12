package com.diamonds

import net.corda.core.contracts.Amount
import net.corda.core.contracts.PartyAndReference
import net.corda.core.identity.Party
import net.corda.finance.`issued by`
import net.corda.finance.contracts.asset.Cash
import net.corda.testing.core.TestIdentity
import java.util.*

fun createCashState(owner: Party, issuer: Party, amount: Amount<Currency>): Cash.State {
    val defaultRef = ByteArray(1) { 1 }
    return Cash.State(amount = amount `issued by` issuer.ref(defaultRef.first()), owner = owner)
}
