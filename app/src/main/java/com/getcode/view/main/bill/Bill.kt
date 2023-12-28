package com.getcode.view.main.bill

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.getcode.model.CodePayload
import com.getcode.model.CurrencyCode
import com.getcode.model.Fiat
import com.getcode.model.KinAmount
import com.getcode.model.Kind
import com.getcode.models.Bill
import com.getcode.network.repository.Request
import com.getcode.theme.CodeTheme
import com.getcode.util.formatted
import com.getcode.utils.FormatUtils
import timber.log.Timber

@Composable
fun Bill(
    modifier: Modifier = Modifier,
    bill: Bill,
) {
    when (bill) {
        is Bill.Cash -> CashBill(
            modifier = Modifier
                .padding(bottom = 20.dp)
                .then(modifier),
            payloadData = bill.data,
            amount = bill.amount.formatted()
        )
        is Bill.Payment -> PaymentBill(
            modifier = modifier,
            data = bill.data,
            currencyCode = bill.request.payload.fiat?.currency,
            amount = bill.amount
        )
    }
}

@Preview
@Composable
fun Preview_CashBill() {
    CodeTheme {
        CashBill(payloadData = emptyList(), amount = "15,760")
    }
}

@Preview
@Composable
fun Preview_PaymentBill() {
    CodeTheme {
        val payload = CodePayload(
            Kind.RequestPayment,
            value = Fiat(CurrencyCode.USD, 0.25),
            nonce = listOf(
                -85, -37, -27, -38, 37, -1, -4, -128, 102, 123, -35
            ).map { it.toByte() }
        )

        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            PaymentBill(
                amount = KinAmount.fromFiatAmount(
                    fiat = 0.25,
                    fx = 0.00001585,
                    CurrencyCode.USD
                ),
                data = payload.codeData.toList(),
                currencyCode = payload.fiat?.currency
            )
        }
    }
}