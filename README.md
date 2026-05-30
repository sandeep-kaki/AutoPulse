# 🚀 AutoPulse

[![AutoPulse CI Pipeline](https://github.com/sandeep-kaki/AutoPulse/actions/workflows/autopulse-ci.yml/badge.svg)](https://github.com/sandeep-kaki/AutoPulse/actions/workflows/autopulse-ci.yml)
> AI-Powered Multi-Layer Test Automation Framework

## What Is AutoPulse?

AutoPulse is a production-grade test automation framework
built on Java, Selenium, REST Assured, TestNG, and Maven.
It tests [automationexercise.com](https://automationexercise.com)
across UI and API layers, with GitHub Actions CI/CD integration
and AI-powered failure analysis.

## Tech Stack

| Layer | Technology |
|---|---|
| UI Automation | Selenium 4.21 + Page Object Model |
| API Testing | REST Assured 5.4 |
| Test Framework | TestNG 7.9 |
| Build Tool | Apache Maven |
| Reporting | ExtentReports 5.1 |
| CI/CD | GitHub Actions |
| AI Analysis | Claude API (coming soon) |
| Language | Java 17 |

## Project Structure

src/
├── main/java/com/autopulse/
│   ├── config/       → ConfigReader (Singleton)
│   ├── pages/        → Page Objects (POM)
│   ├── api/          → REST Assured API clients
│   ├── utils/        → DriverManager, Reports, Screenshots
│   └── ai/           → AI Failure Analyser (coming soon)
└── test/java/com/autopulse/tests/
├── ui/           → Login, Product, Cart tests
└── api/          → User API tests

## How To Run

```bash
# Run full test suite
mvn test

# Tests run automatically on every push via GitHub Actions
```

## Test Coverage

- ✅ Login — valid, invalid, empty credentials, data-driven
- ✅ Products — page load, search, detail page
- ✅ Cart — add to cart, verify items, E2E flow
- ✅ User API — create, verify, delete lifecycle