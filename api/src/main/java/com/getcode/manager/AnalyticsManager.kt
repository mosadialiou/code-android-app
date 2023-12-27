package com.getcode.manager

import com.getcode.api.BuildConfig
import com.getcode.model.CurrencyCode
import com.getcode.model.Kin
import com.getcode.model.KinAmount
import com.getcode.solana.keys.PublicKey
import com.getcode.solana.keys.base58
import com.google.firebase.ktx.Firebase
import com.google.firebase.perf.ktx.performance
import com.google.firebase.perf.metrics.Trace
import com.mixpanel.android.mpmetrics.MixpanelAPI
import org.json.JSONObject
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

interface AnalyticsService {
    fun open(screen: AnalyticsManager.Screen)
    fun login(ownerPublicKey: String, autoCompleteCount: Int, inputChangeCount: Int)
    fun logout()
    fun track(event: AnalyticsManager.Name, vararg properties: Pair<AnalyticsManager.Property, String>)
}

class AnalyticsServiceNull : AnalyticsService {
    override fun open(screen: AnalyticsManager.Screen) = Unit

    override fun login(ownerPublicKey: String, autoCompleteCount: Int, inputChangeCount: Int) = Unit

    override fun logout() = Unit

    override fun track(
        event: AnalyticsManager.Name,
        vararg properties: Pair<AnalyticsManager.Property, String>
    ) = Unit
}

@Singleton
class AnalyticsManager @Inject constructor(private val mixpanelAPI: MixpanelAPI): AnalyticsService {
    private var grabStartMillis: Long = 0L
    private var cashLinkGrabStartMillis: Long = 0L

    override fun open(screen: Screen) {
        track(Name.Open, Pair(Property.Screen, screen.value))
    }

    override fun logout() {
        track(Name.Logout)
    }

    override fun login(ownerPublicKey: String, autoCompleteCount: Int, inputChangeCount: Int) {
        track(
            Name.Login,
            Pair(Property.OwnerPublicKey, ownerPublicKey),
            Pair(Property.AutoCompleteCount, autoCompleteCount.toString()),
            Pair(Property.InputChangeCount, inputChangeCount.toString()),
        )
    }

    fun createAccount(isSuccessful: Boolean, ownerPublicKey: String?) {
        track(
            Name.CreateAccount,
            Pair(Property.Result, isSuccessful.toString()),
            Pair(Property.OwnerPublicKey, ownerPublicKey.orEmpty())
        )
    }

    fun billTimeoutReached(kin: Kin, currencyCode: CurrencyCode, animation: BillPresentationStyle) {
        track(
            Name.Bill,
            Pair(Property.State, StringValue.TimedOut.value),
            Pair(Property.Amount, kin.toKin().toInt().toString()),
            Pair(Property.Currency, currencyCode.name),
            Pair(Property.Animation, animation.value)
        )
    }

    fun billShown(kin: Kin, currencyCode: CurrencyCode, animation: BillPresentationStyle) {
        track(
            Name.Bill,
            Pair(Property.State, StringValue.Shown.value),
            Pair(Property.Amount, kin.toKin().toInt().toString()),
            Pair(Property.Currency, currencyCode.name),
            Pair(Property.Animation, animation.value)
        )
    }

    fun billHidden(kin: Kin, currencyCode: CurrencyCode, animation: BillPresentationStyle) {
        track(
            Name.Bill,
            Pair(Property.State, StringValue.Hidden.value),
            Pair(Property.Amount, kin.toKin().toInt().toString()),
            Pair(Property.Currency, currencyCode.name),
            Pair(Property.Animation, animation.value)
        )
    }

    fun transfer(amount: KinAmount, successful: Boolean) {
        track(
            Name.Transfer,
            Property.State to if (successful) StringValue.Success.value else StringValue.Failure.value,
            Property.Amount to amount.kin.toKin().toInt().toString(),
            Property.Fiat to amount.fiat.toString(),
            Property.Fx to amount.rate.fx.toString(),
            Property.Currency to amount.rate.currency.name,
        )
    }

    fun recomputed(fxIn: Double, fxOut: Double) {
        val delta = ((fxOut / fxIn) - 1) * 100
        track(
            Name.Recompute,
            Property.PercentDelta to delta.toString()
        )
    }

    fun grabStart() {
        grabStartMillis = System.currentTimeMillis()
    }

    fun grab(kin: Kin, currencyCode: CurrencyCode) {
        if (grabStartMillis == 0L) return
        val millisecondsToGrab = System.currentTimeMillis() - grabStartMillis
        track(
            Name.Grab,
            Pair(Property.Amount, kin.toKin().toInt().toString()),
            Pair(Property.Currency, currencyCode.name),
            Pair(Property.GrabTime, String.format("%,.2f", millisecondsToGrab / 1000.0))
        )
        grabStartMillis = 0
    }

    fun cashLinkGrabStart() {
        cashLinkGrabStartMillis = System.currentTimeMillis()
    }

    fun cashLinkGrab(kin: Kin, currencyCode: CurrencyCode) {
        if (cashLinkGrabStartMillis == 0L) return
        val millisecondsToGrab = System.currentTimeMillis() - cashLinkGrabStartMillis
        track(
            Name.CashLinkGrab,
            Pair(Property.Amount, kin.toKin().toInt().toString()),
            Pair(Property.Currency, currencyCode.name),
            Pair(Property.GrabTime, String.format("%,.2f", millisecondsToGrab / 1000.0))
        )
        cashLinkGrabStartMillis = 0
    }

    fun migration(amount: Kin) {
        track(
            Name.PrivacyMigration,
            Pair(Property.Amount, amount.toKin().toInt().toString())
        )
    }

    fun upgradePrivacy(successful: Boolean, intentId: PublicKey, actionCount: Int) {
        track(
            Name.UpgradePrivacy,
            Pair(Property.State, if (successful) StringValue.Success.value else StringValue.Failure.value),
            Pair(Property.IntentId, intentId.base58()),
            Pair(Property.ActionCount, actionCount.toString())
        )
    }

    private var traceAppInit: Trace? = null
    private var timeAppInit: Long? = null

    fun onAppStart() {
        timeAppInit = System.currentTimeMillis()
        traceAppInit = Firebase.performance.newTrace("Init")
        traceAppInit?.start()
    }

    fun onAppStarted() {
        traceAppInit ?: return
        traceAppInit?.stop()
        traceAppInit = null
        Timber.i("App init time: " + (System.currentTimeMillis() - (timeAppInit ?: 0)))
    }

    fun onBillReceived() {
        Timber.i("Bill scanned. From start: " + (System.currentTimeMillis() - (timeAppInit ?: 0)))
    }

    override fun track(event: Name, vararg properties: Pair<Property, String>) {
        if (BuildConfig.DEBUG) {
            Timber.d("debug track $event, ${properties.map { "${it.first.name}, ${it.second}" }}")
            return
        } //no logging in debug

        val jsonObject = JSONObject()
        properties.forEach { property ->
            jsonObject.put(property.first.value, property.second)
        }
        mixpanelAPI.track(event.value, jsonObject)
    }

    enum class Name(val value: String) {
        //Open
        Open("Open"),

        //Account
        Logout("Logout"),
        Login("Login"),
        CreateAccount("Create Account"),

        //Bill
        Bill("Bill"),

        //Transfer
        Transfer("Transfer"),
        RequestPayment("Request Payment"),
        Grab("Grab"),
        CashLinkGrab("CashLinkGrab"),
        Validation("Validation"),

        PrivacyMigration("Privacy Migration"),
        UpgradePrivacy("Upgrade Privacy"),
        ErrorRequest("Error Request"),


        Recompute("Recompute"),
    }

    enum class Property(val value: String) {
        //Open
        Screen("Screen"),

        // Account
        OwnerPublicKey("Owner Public Key"),
        AutoCompleteCount("Auto-complete count"),
        InputChangeCount("Input change count"),
        Result("Result"),
        MillisecondsToConfirm("Milliseconds to confirm"),
        GrabTime("Grab Time"),

        // Bill
        State("State"),
        Amount("Amount"),
        Fiat("Fiat"),
        Fx("Exchange Rate"),
        Currency("Currency"),
        Animation("Animation"),
        Rendezvous("Rendezvous"),

        // Validation
        Type("Type"),
        Error("Error"),

        // Privacy Upgrade
        IntentId("Intent ID"),
        ActionCount("Action Count"),

        PercentDelta("Percent Delta"),

    }

    enum class ValidationType(val value: String) {
        Transfer("Transfer"),
        CreateAccount("Create Account")
    }

    enum class Screen(val value: String) {
        GetKin("Get Kin Screen"),
        Invite("Invite Screen"),
        GiveKin("Give Kin Screen"),
        Balance("Balance Screen"),
        Faq("FAQ Screen"),
        Settings("Settings Screen"),
        BuyAndSellKin("Buy and Sell Kin Screen"),
        Deposit("Deposit Screen"),
        Backup("Backup Screen"),
        Withdraw("Withdraw Screen"),
        Debug("Debug Screen"),
        ForceUpgrade("Force Upgrade"),
        None("NONE"),
    }

    enum class BillPresentationStyle(val value: String) {
        Pop("Pop"),
        Slide("Slide"),
    }

    enum class StringValue(val value: String) {
        Success("Success"),
        Failure("Failure"),
        Shown("Shown"),
        Hidden("Hidden"),
        TimedOut("Timed Out"),
    }

}