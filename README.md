
# Telemetry Agent

A desktop Java application that collects system telemetry data and integrates with Gemini AI for analysis.

---

## 🚀 Getting Started

### 1. Clone the repository
```bash
git clone https://github.com/yourorg/telemetry-agent.git
cd telemetry-agent
````

### 2. API Key Setup

The project requires a **Gemini API key** to run.

You have **two ways** to provide the key:

#### Option A – Environment Variable (recommended)

Set an environment variable on your system:

**Linux / macOS:**

```bash
export GEMINI_API_KEY="your-api-key-here"
```

**Windows (PowerShell):**

```powershell
setx GEMINI_API_KEY "your-api-key-here"
```

#### Option B – Local ApiKey.java (for development only)

Create a file at:

```
src/main/java/com/yourorg/telemetryagent/core/ai/ApiKey.java
```

with the following content:

```java
package com.yourorg.telemetryagent.core.ai;

public class ApiKey {
    private static final String DEFAULT_KEY = "your-api-key-here";

    public static String get() {
        String envKey = System.getenv("GEMINI_API_KEY");
        if (envKey != null && !envKey.isBlank()) {
            return envKey;
        }
        return DEFAULT_KEY;
    }
}
```

⚠️ **Important:**

* This file is in `.gitignore` so your personal key will never be committed.
* If you want to share the project, provide an `ApiKeyTemplate.java` instead of the real one.

---

### 3. Run the Application

Use Gradle to run the app with stacktrace logging enabled:

```bash
./gradlew run --stacktrace
```

Or build a JAR:

```bash
./gradlew build
java -jar build/libs/telemetry-agent.jar
```

---

## 📂 Project Structure

```
src/
 └── main/java/com/yourorg/telemetryagent/
     ├── core/         # GeminiClient & API integration
     ├── domain/       # Models (ProcessInfo, SystemMetricsSnapshot)
     ├── ui/           # JavaFX components
     └── TelemetryAgent.java
```

---


## 🛠️ Tech Stack

* Java 17+
* JavaFX
* Gradle
* Gemini AI API


