package com.example.data.payment

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path

/**
 * Authorized SolarFix Backend Cloud/Edge Functions Proxy Interface.
 * Base URL: https://api.solarfix.africa/v1/functions/
 *
 * SECURITY ARCHITECTURE:
 * The mobile application NEVER holds or transmits third-party secret keys
 * (such as PAYSTACK_SECRET_KEY, FLUTTERWAVE_SECRET_KEY, IDENTITY_VERIFICATION_SECRET_KEY,
 * or DELIVERY_PROVIDER_SECRET_KEY). Instead, the mobile client authenticates with
 * SolarFix Backend Functions using short-lived Firebase Auth ID tokens and Firebase App Check
 * attestation tokens, and the backend function attaches secret credentials server-side.
 */
interface PaystackApiService {

    @POST("payments/paystack/initialize")
    suspend fun initializeTransaction(
        @Header("Authorization") firebaseUserIdTokenBearer: String,
        @Header("X-Firebase-AppCheck") appCheckToken: String = "",
        @Body request: PaystackInitRequest
    ): Response<PaystackApiResponse<PaystackInitData>>

    @GET("payments/paystack/verify/{reference}")
    suspend fun verifyTransaction(
        @Header("Authorization") firebaseUserIdTokenBearer: String,
        @Header("X-Firebase-AppCheck") appCheckToken: String = "",
        @Path("reference") reference: String
    ): Response<PaystackApiResponse<PaystackVerifyData>>

    @POST("payments/paystack/subaccount")
    suspend fun createSubaccount(
        @Header("Authorization") firebaseUserIdTokenBearer: String,
        @Header("X-Firebase-AppCheck") appCheckToken: String = "",
        @Body request: PaystackCreateSubaccountRequest
    ): Response<PaystackApiResponse<PaystackSubaccountData>>

    @POST("payments/paystack/refund")
    suspend fun createRefund(
        @Header("Authorization") firebaseUserIdTokenBearer: String,
        @Header("X-Firebase-AppCheck") appCheckToken: String = "",
        @Body request: PaystackCreateRefundRequest
    ): Response<PaystackApiResponse<PaystackRefundData>>

    @GET("payments/flutterwave/verify/{transactionId}")
    suspend fun verifyFlutterwaveTransaction(
        @Header("Authorization") firebaseUserIdTokenBearer: String,
        @Header("X-Firebase-AppCheck") appCheckToken: String = "",
        @Path("transactionId") transactionId: String
    ): Response<PaystackApiResponse<PaystackVerifyData>>

    @POST("identity/kyb/verify")
    suspend fun verifySellerIdentityOnBackend(
        @Header("Authorization") firebaseUserIdTokenBearer: String,
        @Header("X-Firebase-AppCheck") appCheckToken: String = "",
        @Body request: BackendIdentityVerificationRequest
    ): Response<PaystackApiResponse<BackendIdentityVerificationResult>>

    @POST("logistics/delivery/dispatch")
    suspend fun dispatchDeliveryOrderOnBackend(
        @Header("Authorization") firebaseUserIdTokenBearer: String,
        @Header("X-Firebase-AppCheck") appCheckToken: String = "",
        @Body request: BackendDeliveryDispatchRequest
    ): Response<PaystackApiResponse<BackendDeliveryDispatchResult>>
}

data class BackendIdentityVerificationRequest(
    val storeId: String,
    val documentType: String, // "CAC", "TIN", "NIN"
    val documentNumber: String,
    val businessName: String
)

data class BackendIdentityVerificationResult(
    val verified: Boolean,
    val verificationReference: String,
    val providerStatus: String
)

data class BackendDeliveryDispatchRequest(
    val orderId: String,
    val pickupCity: String,
    val destinationCity: String,
    val destinationState: String,
    val recipientPhone: String
)

data class BackendDeliveryDispatchResult(
    val trackingNumber: String,
    val logisticsPartner: String,
    val estimatedDeliveryWindow: String
)

data class PaystackInitRequest(
    val email: String,
    val amount: Long, // in Kobo (NGN * 100)
    val reference: String,
    val callback_url: String? = null,
    val currency: String = "NGN",
    val channels: List<String> = listOf("card", "bank_transfer", "ussd", "bank"),
    val subaccount: String? = null,
    val bearer: String = "subaccount"
)

data class PaystackCreateSubaccountRequest(
    val business_name: String,
    val settlement_bank: String,
    val account_number: String,
    val percentage_charge: Double
)

data class PaystackCreateRefundRequest(
    val transaction: String,
    val amount: Long? = null,
    val merchant_note: String? = null,
    val customer_note: String? = null
)

data class PaystackApiResponse<T>(
    val status: Boolean,
    val message: String,
    val data: T?
)

data class PaystackInitData(
    val authorization_url: String,
    val access_code: String,
    val reference: String
)

data class PaystackVerifyData(
    val id: Long,
    val domain: String,
    val status: String, // "success", "failed", "abandoned"
    val reference: String,
    val amount: Long, // in Kobo
    val gateway_response: String,
    val paid_at: String?,
    val created_at: String?,
    val channel: String?,
    val currency: String,
    val ip_address: String?,
    val customer: PaystackCustomerInfo?
)

data class PaystackCustomerInfo(
    val id: Long?,
    val first_name: String?,
    val last_name: String?,
    val email: String?,
    val phone: String?
)

data class PaystackSubaccountData(
    val subaccount_code: String,
    val business_name: String,
    val settlement_bank: String,
    val account_number: String,
    val percentage_charge: Double
)

data class PaystackRefundData(
    val id: Long,
    val status: String,
    val transaction_reference: String,
    val amount: Long,
    val deducted_amount: Long?,
    val fully_deducted: Boolean?
)
