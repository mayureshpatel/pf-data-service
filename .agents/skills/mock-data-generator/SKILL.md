---
name: mock-data-generator
description: Generates deterministic, high-volume mock data for local testing. Trigger this skill when asked to seed the database or test performance with large datasets.
---
# Mock Data Generator

This skill ensures the local developer environment can handle production-scale volumes without relying on manual data entry.

## 🚨 Constraints & Guardrails
- **No Embedded Databases:** The mock data must be generated for the target PostgreSQL schema.
- **Relational Integrity:** You MUST strictly adhere to Foreign Key constraints. Transactions must map to valid Users, Accounts, and Categories.
- **Volume:** If asked for high-volume data (e.g., 5000+ rows), rely on procedural generation rather than writing 5000 individual `INSERT` lines.

## 🛠 Procedural Workflow
1. **Analyze Schema:** Query the MCP server or review Flyway migrations to understand the current relational schema.
2. **Choose Generator Method:**
   - For low volume (< 50 rows): Generate a standard Flyway repeatable migration script (e.g., `R__seed_mock_data.sql`).
   - For high volume (> 50 rows): Write a generic parameterized Java Test or Python script that utilizes a Faker library (e.g., Java Faker / DataFaker) to pump data into the DB dynamically.
3. **Execute:** Execute the generated script against the local developer DB.
