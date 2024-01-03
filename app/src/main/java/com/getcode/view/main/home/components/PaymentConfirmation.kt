package com.getcode.view.main.home.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.getcode.model.CodePayload
import com.getcode.model.CurrencyCode
import com.getcode.model.Fiat
import com.getcode.model.Kin
import com.getcode.model.Kin.Companion.fromFiat
import com.getcode.model.Kin.Companion.fromKin
import com.getcode.model.KinAmount
import com.getcode.model.Kind
import com.getcode.model.Rate
import com.getcode.models.PaymentConfirmation
import com.getcode.models.PaymentState
import com.getcode.network.repository.Request
import com.getcode.theme.CodeTheme
import com.getcode.theme.White50
import com.getcode.view.components.SlideToConfirm
import kotlinx.coroutines.delay

@Composable
internal fun PaymentConfirmation(
    modifier: Modifier = Modifier,
    confirmation: PaymentConfirmation?,
    onSend: () -> Unit,
    onCancel: () -> Unit,
) {
    val state by remember(confirmation?.state) {
        derivedStateOf { confirmation?.state }
    }

    val isSending by remember(state) {
        derivedStateOf { state is PaymentState.Sending }
    }

    val requestedAmount by remember(confirmation?.requestedAmount) {
        derivedStateOf { confirmation?.requestedAmount }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .background(Color.Black)
            .padding(horizontal = 20.dp, vertical = 30.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(15.dp)
    ) {
        if (state != null) {
            requestedAmount?.let { amount ->
                PriceWithFlag(
                    currencyCode = amount.rate.currency,
                    amount = amount,
                    iconSize = 24.dp
                ) {
                    Text(
                        text = it,
                        color = Color.White,
                        style = MaterialTheme.typography.h1
                    )
                }
            }
            SlideToConfirm(
                isLoading = isSending,
                isSuccess = state is PaymentState.Sent,
                onConfirm = { onSend() },
            )
            AnimatedContent(
                targetState = !isSending && state !is PaymentState.Sent,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "show/hide cancel button"
            ) { show ->
                if (show) {
                    TextButton(
                        shape = RoundedCornerShape(percent = 50),
                        onClick = { onCancel() }) {
                        Text(
                            text = "Cancel",
                            style = MaterialTheme.typography.caption,
                            color = White50
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.minimumInteractiveComponentSize())
                }
            }
        }
    }
}

private val payload = CodePayload(
    Kind.RequestPayment,
    value = Fiat(CurrencyCode.USD, 0.25),
    nonce = listOf(
        -85, -37, -27, -38, 37, -1, -4, -128, 102, 123, -35
    ).map { it.toByte() }
)

private fun confirmationWithState(state: PaymentState) = PaymentConfirmation(
    state = state,
    payload = payload,
    requestedAmount = KinAmount.fromFiatAmount(
        fiat = 0.25,
        fx = 0.00001585,
        CurrencyCode.USD
    )
)
@Preview(showBackground = true)
@Composable
fun Preview_PaymentConfirmModal_Awaiting() {
    CodeTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
        ) {
            PaymentConfirmation(
                modifier = Modifier.align(Alignment.BottomCenter),
                confirmation = confirmationWithState(PaymentState.AwaitingConfirmation),
                onSend = { }
            ) {

            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun Preview_PaymentConfirmModal_Sending() {
    CodeTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
        ) {
            PaymentConfirmation(
                modifier = Modifier.align(Alignment.BottomCenter),
                confirmation = confirmationWithState(PaymentState.Sending),
                onSend = { }
            ) {

            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun Preview_PaymentConfirmModal_Sent() {
    CodeTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
        ) {
            PaymentConfirmation(
                modifier = Modifier.align(Alignment.BottomCenter),
                confirmation = confirmationWithState(PaymentState.Sent),
                onSend = { }
            ) {

            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun Preview_PaymentConfirmModal_Interactive() {
    CodeTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
        ) {
            var confirmation by remember {
                mutableStateOf<PaymentConfirmation?>(
                    PaymentConfirmation(
                        state = PaymentState.AwaitingConfirmation,
                        payload = payload,
                        requestedAmount = KinAmount.fromFiatAmount(
                            fiat = 0.25,
                            fx = 0.00001585,
                            CurrencyCode.USD
                        )
                    )
                )
            }

            AnimatedContent(
                modifier = Modifier.align(Alignment.BottomCenter),
                targetState = confirmation?.payload,
                transitionSpec = {
                    slideIntoContainer(
                        towards = AnimatedContentTransitionScope.SlideDirection.Up,
                        animationSpec = tween(durationMillis = 600, delayMillis = 450)
                    ) togetherWith slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Down)
                },
                label = "payment confirmation",
            ) {
                // uses static payload for animation criteria; renders off state
                if (it != null) {
                    Box(
                        contentAlignment = Alignment.BottomCenter
                    ) {
                        PaymentConfirmation(
                            confirmation = confirmation,
                            onSend = {
                                confirmation = confirmation?.copy(state = PaymentState.Sending)
                            },
                            onCancel = { confirmation = null }
                        )
                    }
                }
            }

            LaunchedEffect(confirmation?.state) {
                val state = confirmation?.state
                if (state is PaymentState.Sending) {
                    delay(1_500)
                    confirmation = confirmation?.copy(state = PaymentState.Sent)
                } else if (state is PaymentState.Sent) {
                    delay(500)
                    confirmation = null
                }
            }
        }
    }
}