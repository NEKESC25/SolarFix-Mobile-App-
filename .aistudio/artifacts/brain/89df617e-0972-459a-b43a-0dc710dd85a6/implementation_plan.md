# Solarfix - Solar Technician & Parts

Solarfix is an all-in-one Android application designed for African homes, businesses, and solar installers. It integrates an intelligent AI Solar Technician that diagnoses faults, decodes inverter error codes, and provides actionable repair guidance; a comprehensive Solar Hardware Marketplace for genuine panels, inverters, lithium/tubular batteries, and accessories with multi-currency support (NGN primary with USD/KES/GHS conversion); and a dynamic Energy Analytics Dashboard tracking generation, battery health, and load optimization.

---

### User Review & Critical Decisions

> [!IMPORTANT]
> The following key decisions were confirmed during the planning consultation and are incorporated into the system blueprint:

- **Brand & Identity**: The application is named **Solarfix - Solar Technician and Parts**, reflecting its dual role as a trusted AI diagnostic assistant and a verified parts marketplace.
- **Regional Currency & Multi-Currency Engine**: Primary pricing is in **Nigerian Naira (NGN, ₦)** with an instant live toggle for **US Dollars ($)**, **Kenyan Shillings (KES)**, and **Ghanaian Cedis (GHS)** to cater to solar owners across West, East, and Pan-African markets.
- **Unified 3-in-1 Diagnostic Suite**: The AI Solar Technician incorporates all three key modes:
  1. *Interactive Inverter Error Code Scanner & Guided Troubleshooter* (Growatt, Felicity, Must, Luminous, Deye, SRNE, Sunsynk, Victron, etc.).
  2. *Conversational AI Diagnostics* powered by Gemini AI with rapid troubleshooting for issues like battery drain, high inverter temperature, PV string drops, and overloading.
  3. *Step-by-step DIY Preventive Maintenance Checklists* with a *Certified Solar Technician Directory* allowing users to request emergency on-site professional intervention when hardware repairs or high-voltage work is required.

---

## 1. Overview & Core Concept

### Target Audience & Persona
- **Homeowners & Small Businesses**: Users who rely on off-grid or hybrid solar systems to overcome grid outages and rising diesel/petrol generator costs, but lack technical know-how when inverters trip or batteries deplete prematurely.
- **Solar Enthusiasts & Installers**: Technicians who need fast lookup of inverter fault codes, wire sizing tables, replacement parts sourcing, and real-time solar yield monitoring.

### Key Value Delivered
- **Zero-Downtime Troubleshooting**: Demystifies cryptic inverter fault beeps and codes (e.g., Error 04, Error 08, Error 51, High Temp, Overload) into plain English explanations and safety-first DIY fixes.
- **Direct Access to Quality Solar Hardware**: Solves the widespread counterfeit battery and panel problem by connecting owners with verified hardware listings, technical spec sheets, and clear warranty badges.
- **Energy Independence & Analytics**: Visualizes daily PV generation, battery state of charge (SoC), load distribution (essential vs heavy loads), and estimated generator fuel savings.

---

## 2. User Experience & Visual Design

### Aesthetic Direction & Visual Identity
- **Theme**: "Solar Clean Tech" — Modern, authoritative, and solar-themed with high-contrast readability under bright African daylight as well as dark mode for nighttime power checks.
- **Color Palette**:
  - *Primary (Solar Amber / Sunrise Gold)*: `Color(0xFFF59E0B)` / `Color(0xFFD97706)` — represents solar energy, generation, and active power.
  - *Secondary (Emerald Green / Clean Grid)*: `Color(0xFF10B981)` — battery health, successful status, eco savings.
  - *Surface / Background (Deep Obsidian & Slate)*: `Color(0xFF0F172A)` background with card elevation at `Color(0xFF1E293B)` for premium tech depth and high contrast.
  - *Warning / Fault (Crimson Alert)*: `Color(0xFFEF4444)` for inverter fault codes and overload alarms.
- **Typography & Rhythm**:
  - Crisp display headings with bold technical badges.
  - Clear visual metrics (Watts, Volts, Amps, Amp-hours, Percentages) formatted in readable monospaced or high-legibility numerals.
- **Touch & Accessibility**:
  - Full adherence to 48dp minimum interactive touch targets.
  - Full edge-to-edge support with `WindowInsets.safeDrawing` and navigation bar padding.

### Navigation Structure & Primary Flows

```
┌────────────────────────────────────────────────────────────────────────────────┐
│                           SOLARFIX MOBILE ARCHITECTURE                         │
├────────────────────┬────────────────────┬────────────────────┬─────────────────┤
│  ⚡ AI TECHNICIAN   │   🛒 MARKETPLACE   │   📈 ANALYTICS     │  🛠 SYSTEM HUB   │
├────────────────────┼────────────────────┼────────────────────┼─────────────────┤
│ • Inverter Faults  │ • Solar Panels     │ • Real-time Output │ • My Equipment  │
│ • Interactive Chat │ • Lithium/Gel Batt │ • Battery Health   │ • Maintenance   │
│ • Common Symptoms  │ • Smart Inverters  │ • Load Breakdown   │ • Tech Dispatch │
│ • Safety Protocols │ • Cables & Parts   │ • Generator Saved  │ • Currency Swp  │
└────────────────────┴────────────────────┴────────────────────┴─────────────────┘
```

1. **AI Solar Technician Tab**:
   - Quick symptom selector chips (e.g., "Battery draining fast at night", "Inverter tripping on F08", "Panels not charging", "AC output voltage fluctuating").
   - Instant Inverter Error Code quick-lookup tool: pick brand (e.g., Growatt, Felicity Solar, Must Power, Luminous) and error code number to get instant cause + safety checklist.
   - Live AI Diagnostic Chat: interactive conversational assistance for deep-dive questions, safety warnings, and component recommendations.
   - Emergency "Call Certified Technician" bottom sheet with nearby verified local solar engineers.

2. **Solar Marketplace Tab**:
   - Filterable categories: **Solar Panels (Mono/Bifacial)**, **Lithium LiFePO4 & Tubular Batteries**, **Hybrid Inverters (1kVA–15kVA)**, **Charge Controllers (MPPT/PWM)**, **DC Cables, Breakers & Surge Protectors**.
   - Currency switcher badge in top bar: switch between NGN (₦), USD ($), KES (KSh), and GHS (GH₵) with calculated currency conversion.
   - Product detail screen: technical specifications (Max Voc, C-rating, Cycle life, Warranty), compatibility notes ("Matches 24V or 48V setup"), in-cart management, and simulated order checkout.

3. **Energy Analytics Dashboard Tab**:
   - Real-time PV solar yield curve (Day/Week view with morning peak to evening tapering).
   - Battery State of Charge (SoC %) dial with estimated hours remaining based on current load.
   - Household / Office Load Split: Refrigeration, Lighting, Pumping, AC/Fans, Electronics.
   - Fuel & Cost Savings Tracker: calculated fuel liters and costs saved compared to running a petrol/diesel generator.

4. **Maintenance & Equipment Hub**:
   - User's Solar Profile: register system capacity (e.g., 5kVA Inverter, 4x 200Ah Gel or 5.12kWh Lithium, 6x 450W Mono Panels).
   - Preventive Checklist: cleaning dust/soot off panels, checking battery terminal corrosion, checking inverter dust filters, tightening breaker screws.

---

## 3. Key Product Decisions & Trade-Offs

- **Decision 1: Dual Diagnostic System (Instant Offline Lookup + Cloud AI Chat)**
  - *Chosen Approach*: Provide a rich local database of common inverter error codes and standard troubleshooting steps that works instantly and offline, paired with Firebase AI Logic (Gemini 3.8 Flash) for bespoke, conversational diagnostics.
  - *Why*: Solar system failures in Africa often coincide with grid blackouts or intermittent internet. Giving users instant offline error diagnostics ensures the app is dependable in emergency brownout situations, while the AI chat handles complex, bespoke multi-component setups.
- **Decision 2: Local Persistence for Cart, Equipment Profile, and Maintenance Logs**
  - *Chosen Approach*: Use Jetpack Room Database / in-memory reactive state flows for the shopping cart, saved system specifications, error history, and completed maintenance tasks.
  - *Why*: Instant reactivity, offline availability, zero barrier to entry (no forced login screen on first launch), with zero latency.
- **Decision 3: Multi-Currency Dynamic Formatting**
  - *Chosen Approach*: Store item base prices and apply a live converter supporting Nigerian Naira, US Dollars, Kenyan Shillings, and Ghanaian Cedis.
  - *Why*: African solar users and cross-border businesses purchase equipment priced both in local currencies and USD benchmarks.

---

## 4. Technical Architecture & Data Strategy

```
┌────────────────────────────────────────────────────────────────────────┐
│                        PRESENTATION (COMPOSE M3)                       │
│  MainActivity ─► AppNavigation ─► Scaffold + BottomBar                 │
│    ├── TechnicianScreen (Error Code Lookup, Symptom Guide, AI Chat)    │
│    ├── MarketplaceScreen (Catalog, Search, Product Detail, Cart)       │
│    ├── AnalyticsScreen (Solar Yield Chart, Battery Dial, Fuel Savings) │
│    └── SystemProfileScreen (My Solar Setup, Maintenance Checklists)    │
└───────────────────────────────────┬────────────────────────────────────┘
                                    │ State / User Events
                                    ▼
┌────────────────────────────────────────────────────────────────────────┐
│                       VIEWMODEL & BUSINESS LOGIC                       │
│  • TechnicianViewModel: Manages AI conversation, fault diagnostics     │
│  • MarketplaceViewModel: Filter categories, search, currency, cart     │
│  • AnalyticsViewModel: Solar generation simulations, battery load math │
│  • SystemProfileViewModel: User hardware profile, checklist progress   │
└───────────────────────────────────┬────────────────────────────────────┘
                                    │ Repositories
                                    ▼
┌───────────────────────────────────┴────────────────────────────────────┐
│                             DATA LAYER                                 │
│  ┌───────────────────────┐  ┌───────────────────────┐  ┌─────────────┐ │
│  │ Firebase AI Logic     │  │ In-Memory & Room DB   │  │ Offline     │ │
│  │ (Gemini 3.8 Flash)    │  │ Cart, Equipment, Logs │  │ Diagnostics │ │
│  │ App Check Attestation │  │ Multi-Currency Engine │  │ Catalog Data│ │
│  └───────────────────────┘  └───────────────────────┘  └─────────────┘ │
└────────────────────────────────────────────────────────────────────────┘
```

---

## 5. Execution Steps

1. **Platform Alignment & Config**:
   - Update `metadata.json` and `res/values/strings.xml` to match "Solarfix".
   - Setup App Check configuration helper per `firebase-ai-logic-android` standard.
2. **Data Models & Offline Repositories**:
   - Inverter error codes database (Growatt, Felicity, Must, Luminous, etc.).
   - Solar marketplace catalog (authentic solar panels, lithium/gel batteries, inverters, cables, protection accessories).
   - Maintenance checklists and technician directory with phone/WhatsApp dispatch affordances.
3. **ViewModels & State Management**:
   - `TechnicianViewModel` handling error lookups and streaming Gemini AI diagnostic assistance.
   - `MarketplaceViewModel` with dynamic currency conversion and cart management.
   - `AnalyticsViewModel` computing real-time energy production curves and battery runtimes.
4. **Jetpack Compose UI Screens**:
   - Responsive Material 3 design, edge-to-edge support, custom battery & solar dial components, crisp tabs, and rich visual cards.
5. **Execution Steps**:
   - Run `compile_applet` to ensure error-free compilation and clean build output.

---

## 6. Secure Payment & Webhook Verification Architecture (Paystack Nigeria)

- **Backend Verification Only**: SolarFix never trusts frontend payment callbacks; payments are validated server-side against Paystack APIs and cryptographic webhook headers.
- **HMAC-SHA512 Webhook Verification with `PAYSTACK_SECRET_KEY`**: Paystack does not issue a separate webhook signing secret. All incoming webhooks are validated on the backend by calculating `HMAC-SHA512(rawPayload, PAYSTACK_SECRET_KEY)` and comparing against the `x-paystack-signature` HTTP header.
- **Secret Isolation from Mobile Frontend**: `PAYSTACK_SECRET_KEY` is strictly managed on the backend and added to `secrets { ignoreList.add("PAYSTACK_SECRET_KEY") }` in `app/build.gradle.kts` so that it is never compiled into the Android APK or exposed to client code.
- **Zero Card/PIN Storage**: PCI-DSS Level 1 compliant checkout; card details are handled by Paystack checkout tokens with zero card data stored on device.
- **Marketplace Split Payments**: Automated split routing dynamically aligned with configured seller commission rates (e.g. 2.0% SolarFix commission, 98.0% net seller proceeds).

---

## 7. SolarFix Commission & Seller Wallet Architecture

- **Administrator-Controlled Commission System**:
  - Initial default global platform commission set to **2.0%** (98.0% net seller proceeds), fully configurable via the Administrator Dashboard.
  - Individual seller commission rate overrides (`customCommissionPercent`) allowing the administrator to set tailored rates per store (e.g., 1.5% for strategic partners).
- **Automated Per-Transaction Financial Breakdown**:
  - Automatically calculates and exposes: **Product Selling Price**, **SolarFix Commission**, **Seller's Net Earnings**, **Payment Gateway Charges** (Paystack 1.5% capped at ₦2,000), **Logistics Delivery Charges**, and **Dispute/Refund Adjustments**.
- **Separate Financial Ledgers**:
  - *Customers*: Transparent item subtotals, delivery fees, and order receipts.
  - *Sellers*: Comprehensive seller wallet tracking **Total Net Earnings**, **Pending Escrow** (locked during 48-hour inspection period), **Available Balance** (cleared for payout), **Withdrawn Earnings**, and **SolarFix Commission Deductions**.
  - *SolarFix Platform*: Admin financial report summary auditing Gross Merchandise Value (GMV), total platform commissions earned, pending escrow, and total disbursed payouts.
- **Strict Withdrawal Guards**:
  - *Escrow Protection*: Pending payments are held in escrow until order delivery is confirmed (`OrderStatus.DELIVERED`) and can never be treated as available seller balance.
  - *Balance Protection*: Sellers are strictly prevented from withdrawing more than their current available balance.
  - *Admin Payout Workflow*: Administrators can approve or reject withdrawal requests; approved requests disburse funds to verified bank accounts, while rejected requests return funds to the seller's available balance.

