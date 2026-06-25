# 🚀 AutoPulse

[![AutoPulse CI Pipeline](https://github.com/sandeep-kaki/AutoPulse/actions/workflows/autopulse-ci.yml/badge.svg)](https://github.com/sandeep-kaki/AutoPulse/actions/workflows/autopulse-ci.yml)
![Java](https://img.shields.io/badge/Java-17-orange)
![Selenium](https://img.shields.io/badge/Selenium-4.21-green)
![TestNG](https://img.shields.io/badge/TestNG-7.9-blue)
![Maven](https://img.shields.io/badge/Maven-Build-red)

> A production-grade, multi-layer test automation framework — with two AI-powered intelligence layers built on top.

---

## 🎯 What Is AutoPulse?

AutoPulse is a test automation framework that validates
[automationexercise.com](https://automationexercise.com) across
its **UI and API layers**, using Java, Selenium, and REST Assured —
architected with the same design principles used in real
production test suites: Page Object Model, Singleton configuration,
ThreadLocal-safe parallel execution, and a two-tier CI/CD pipeline.

On top of that foundation sit two AI layers — automated failure
diagnosis, and an autonomous Self-Healing Agent that investigates
locator failures using function calling. The AI is the
differentiator. The framework underneath it is the actual
engineering.

---

## 🏗️ Architecture

```
┌──────────────────────────────────────────────────┐
│                  AutoPulse Framework             │
│                                                  │
│   ┌─────────────┐         ┌──────────────────┐   │
│   │   UI Layer  │         │    API Layer     │   │
│   │  Selenium + │         │  REST Assured    │   │
│   │  Page Object│         │  UserEndpoints   │   │
│   │    Model    │         │                  │   │
│   └──────┬──────┘         └────────┬─────────┘   │
│          │                         │             │
│          └────────────┬────────────┘             │
│                       ↓                          │
│            ┌────────────────────┐                │
│            │   TestNG Suite     │                │
│            │   + AutoPulseListener│              │
│            └─────────┬──────────┘                │
│                       ↓                          │
│          ┌────────────────────────┐              │
│          │      Test Result       │              │
│          ├────────────────────────┤              │
│     ✅   │     ExtentReport       │   ❌         │
│   PASS   │   (dark theme, HTML)   │   FAIL       │
│          └────────────────────────┘              │
│                                  ↓               │
│                        ┌──────────────────┐      │
│                        │ FailureAnalyser  │      │
│                        │ (Groq — quick    │      │
│                        │  CAUSE/FIX/      │      │
│                        │  PREVENT)        │      │
│                        └────────┬─────────┘      │
│                                 ↓                │
│                   Locator-type failure only:     │
│                        ┌──────────────────┐      │
│                        │ SelfHealingAgent │      │
│                        │ (Groq function   │      │
│                        │  calling loop)   │      │
│                        └──────────────────┘      │
│                                                  │
└──────────────────────────────────────────────────┘
                       ↓
            ┌──────────────────────┐
            │  GitHub Actions CI   │
            │  Smoke (every push)  │
            │  Regression (nightly)│
            └──────────────────────┘
```

---

## 🛠️ Tech Stack

| Layer | Technology |
|---|---|
| UI Automation | Selenium WebDriver 4.21 |
| Design Pattern | Page Object Model (POM) |
| API Testing | REST Assured 5.4 |
| Test Framework | TestNG 7.9 |
| Build Tool | Apache Maven |
| Reporting | ExtentReports 5.1 (dark theme) |
| CI/CD | GitHub Actions |
| AI — Quick Analysis | Groq API — Llama 3.3 70B |
| AI — Agent | Groq Function Calling (Self-Healing Agent) |
| Test Data | Apache POI (Excel) |
| WebDriver Mgmt | WebDriverManager |
| JSON | Jackson |
| Language | Java 17 |

---

## 📁 Project Structure

```
AutoPulse/
├── src/main/java/com/autopulse/
│   ├── ai/
│   │   ├── FailureAnalyser.java     ← Quick AI analysis (Groq)
│   │   └── SelfHealingAgent.java    ← Agentic locator investigation
│   ├── config/
│   │   └── ConfigReader.java        ← Singleton config reader
│   ├── pages/
│   │   ├── BasePage.java            ← Shared interactions + locator tracking
│   │   ├── LoginPage.java
│   │   ├── HomePage.java
│   │   ├── ProductsPage.java
│   │   ├── ProductDetailPage.java
│   │   └── CartPage.java
│   └── utils/
│       ├── DriverManager.java       ← ThreadLocal WebDriver
│       ├── ExtentReportManager.java ← HTML reporting
│       ├── ScreenshotUtil.java      ← Auto screenshot on failure
│       └── ExcelReader.java         ← Data-driven test data
│
├── src/test/java/com/autopulse/tests/
│   ├── BaseTest.java                ← Lifecycle: setup/teardown
│   ├── AutoPulseListener.java       ← TestNG hook → routes to AI
│   ├── ui/
│   │   ├── SmokeTest.java
│   │   ├── LoginTest.java
│   │   └── ProductTest.java
│   └── api/
│       └── UserApiTest.java
│
├── src/test/resources/
│   ├── config.properties            ← gitignored (API key)
│   ├── testng.xml
│   └── testdata/loginData.xlsx
│
├── .github/workflows/
│   └── autopulse-ci.yml
└── pom.xml
```

---

## 🧱 Key Architectural Decisions

| Decision | Why |
|---|---|
| Page Object Model | Locators live in one place per page. UI changes → fix once, not across every test. |
| `ConfigReader` as Singleton | `config.properties` read once, served everywhere — no repeated file I/O. |
| `DriverManager` with ThreadLocal | Each parallel test thread gets its own isolated browser instance — no cross-test interference. |
| `PageLoadStrategy.EAGER` | automationexercise.com's heavy ad iframes prevented `NORMAL` strategy from ever completing. |
| `jsClick()` via Actions class | Chrome 148 shipped without full Selenium 4.21 CDP support — direct `executeScript`/click calls randomly timed out. Actions-class clicks use a different protocol path unaffected by the mismatch. |
| `Keys.RETURN` instead of clicking search button | Every click mechanism failed intermittently on Chrome 148; sending Enter on the input bypassed the issue entirely. |
| Direct `href` navigation instead of clicking product links | Same CDP issue — extracting the link and navigating directly is more reliable than clicking. |
| `AutoPulseListener` lives in `src/test/java` | Maven's `main` source set cannot import classes from `test` — listener depends on `BaseTest`, so it must live in test. |
| `alwaysRun=true` on all TestNG lifecycle annotations | Required for `-Dgroups="smoke"` filtering to still trigger setup/teardown/report init correctly. |
| AI key read from env variable first, config file second | Local dev uses `config.properties` (gitignored); CI uses GitHub Secrets — same code, zero manual switching. |

---

## 🤖 AI Layer 1 — Quick Failure Analysis

Every failed test automatically gets a structured, color-coded analysis:

```
🔍 Root Cause: [specific reason this test failed]
🔧 Fix: [actionable suggested fix]
🛡️ Prevention: [how to avoid this in future]
```

One Groq API call, prompt capped under 100 words, response capped at 100 tokens, stack trace truncated to 5 lines — minimal cost, maximum signal.

---

## 🩺 AI Layer 2 — Self-Healing Agent

For locator-specific failures (`NoSuchElementException`,
`StaleElementReferenceException`, element-wait `TimeoutException`),
AutoPulse wakes up an **autonomous agent** built on Groq's function
calling — not a single AI call, a genuine reasoning loop:

```
Agent investigates:
  → calls getBrokenLocator()         (what were we looking for?)
  → calls getPageSource()            (what actually exists now?)
  → forms a hypothesis
  → calls validateLocator(candidate) (REAL Selenium check, live DOM)
  → reaches a verdict
```

The model never executes code directly — it only ever requests a
tool by name; Java code runs the real method and feeds the result
back. Bounded at 6 reasoning steps maximum.

**By design, the agent never modifies source code.** It reports a
verified verdict for human review — the same responsible pattern
used by enterprise tools like Healenium and Testim:

```
FOUND_FIX: //input[@data-qa='login-email'] |
Validated: 1 element found, tag=input
```
or
```
REAL_BUG: No element matching this intent exists 
anywhere on the current page.
```

---

## ⚙️ CI/CD Pipeline

```
Every push     → Smoke tests (3 critical paths, ~3 min)
Every night    → Full regression (all tests, midnight IST)
Manual trigger → Full regression on demand
```

Two-tier GitHub Actions pipeline. Headless Chrome auto-detected
via `CI=true`. Dependency caching for faster subsequent runs.
Reports uploaded as downloadable artifacts (7–30 day retention).

---

## 🌍 Two Environments — Local vs CI

| | Local | CI (GitHub Actions) |
|---|---|---|
| `ai.enabled` | `true` | `true` (dynamically created) |
| API Key Source | `config.properties` (gitignored) | `GROQ_API_KEY` GitHub Secret |
| Browser Mode | Visible | Headless (auto-detected) |

`ConfigReader.getAiApiKey()` checks the environment variable
first, falling back to the local config file — identical code
path, zero manual switching between environments.

---

## 🚀 How To Run

```bash
# Full test suite
mvn test

# Smoke tests only
mvn test -Dgroups="smoke"

# Single test class
mvn test -Dtest=LoginTest
```

---

## ✅ Test Coverage

| Area | Scenarios |
|---|---|
| Login | Valid, invalid password, empty credentials, data-driven (Excel) |
| Products | Page load, search, product detail |
| Cart | Add to cart, item verification, end-to-end search-to-cart |
| User API | Full lifecycle — create, verify, delete |
| AI Failure Analysis | Fires on every failed test |
| Self-Healing Agent | Fires on every locator-related failure |

---

## 👨‍💻 Built By

**Sandeep** — QA Engineering Associate → SDET
[GitHub](https://github.com/sandeep-kaki/AutoPulse)