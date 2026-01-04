# Appium Runtime Inspector

A Java-based Appium test automation project with a built-in **Runtime Element Inspector** that automatically displays element information when `NoSuchElementException` occurs - similar to Appium Inspector but directly in your terminal!

## Features

- **Automatic Element Inspector**: When an element is not found, the inspector automatically:
  - Captures the current page source (XML hierarchy)
  - Finds the closest matching element using a smart scoring algorithm
  - Displays multiple locator suggestions (accessibility id, id, uiautomator, xpath)
  - Shows all element attributes in a formatted table
  - Prints the parent XML block for context

- **Colorful Terminal Output**: Uses ANSI colors for easy reading
- **Enable/Disable Toggle**: Turn inspector on/off as needed
- **TestNG Integration**: Ready-to-use test framework

## Requirements

| Tool | Version | Description |
|------|---------|-------------|
| **JDK** | 17+ | Java Development Kit |
| **Maven** | 3.8+ | Build and dependency management |
| **Appium** | 2.x | Mobile automation server |
| **Android SDK** | Latest | Android platform tools |
| **Node.js** | 18+ | Required for Appium |
| **Android Emulator/Device** | Any | Test target device |

## Project Structure

```
Appium_Runtime_Inspector/
├── pom.xml                          # Maven configuration
├── testng.xml                       # TestNG suite configuration
├── README.md                        # This file
└── src/
    ├── main/java/
    │   ├── base/
    │   │   └── BaseTest.java        # Base test class with Inspector integration
    │   └── utilities/
    │       └── AndroidElementInspector.java  # The main Inspector class
    └── test/java/
        └── tests/
            └── ApiDemosTest.java    # Example test cases
```

## Dependencies

```xml
<!-- Appium Java Client 8.6.0 (includes Selenium) -->
<dependency>
    <groupId>io.appium</groupId>
    <artifactId>java-client</artifactId>
    <version>8.6.0</version>
</dependency>

<!-- TestNG 7.10.2 -->
<dependency>
    <groupId>org.testng</groupId>
    <artifactId>testng</artifactId>
    <version>7.10.2</version>
</dependency>
```

## Setup Instructions

### 1. Install Prerequisites

```bash
# Install Node.js (macOS)
brew install node

# Install Appium
npm install -g appium

# Install UiAutomator2 driver
appium driver install uiautomator2

# Verify installation
appium --version
```

### 2. Android Setup

```bash
# Set ANDROID_HOME environment variable
export ANDROID_HOME=$HOME/Library/Android/sdk
export PATH=$PATH:$ANDROID_HOME/platform-tools
export PATH=$PATH:$ANDROID_HOME/tools

# Verify ADB
adb devices
```

### 3. Install ApiDemos App

Download and install the ApiDemos-debug.apk on your emulator/device:

```bash
adb install ApiDemos-debug.apk
```

### 4. Start Appium Server

```bash
appium --base-path /wd/hub
```

### 5. Run Tests

```bash
# Run all tests
mvn test

# Run specific test class
mvn test -Dtest=ApiDemosTest
```

---

## AndroidElementInspector

### How It Works

The `AndroidElementInspector` is the core feature of this project. When a `NoSuchElementException` occurs during test execution, it automatically:

1. **Captures Page Source**: Gets the current XML hierarchy from the Android driver
2. **Parses XML**: Converts the page source to a DOM Document
3. **Extracts Search Term**: Parses the failed locator to extract the search term
4. **Finds Best Match**: Uses a scoring algorithm to find the closest matching element
5. **Prints Results**: Displays formatted output with locator suggestions and attributes

### Scoring Algorithm

The inspector uses a weighted scoring system to find the best match:

| Match Type | Points |
|------------|--------|
| Exact match (text/content-desc/resource-id) | 1000 |
| Resource-id ends with `:id/search` | 900 |
| Resource-id ends with `/search` | 800 |
| Text/content-desc contains search | 500 |
| Resource-id contains search | 400 |
| Class name contains search | 300 |
| Prefix similarity | 5 per char |

### Sample Output

When an element is not found, you'll see output like this:

```
╔══════════════════════════════════════════════════════════════════════════════════════════════════╗
║ 🔍 ANDROID ELEMENT INSPECTOR                                                                    ║
╚══════════════════════════════════════════════════════════════════════════════════════════════════╝

⚠️  EXCEPTION: NoSuchElementException
📍 TARGET LOCATOR: By.xpath: //*[@text='Aksesibiliti']
✅ CLOSEST MATCHING ELEMENT FOUND

┌──────────────────────────────────────────────────────────────────────────────────────────────────┐
│ Find By                              Selector                                                    │
├──────────────────────────────────────────────────────────────────────────────────────────────────┤
│ -android uiautomator                 new UiSelector().text("Accessibility")                      │
│ xpath                                //TextView[@text="Accessibility"]                           │
└──────────────────────────────────────────────────────────────────────────────────────────────────┘

┌──────────────────────────────────────────────────────────────────────────────────────────────────┐
│ Attribute                            Value                                                       │
├──────────────────────────────────────────────────────────────────────────────────────────────────┤
│ index                                0                                                           │
│ package                              io.appium.android.apis                                      │
│ class                                android.widget.TextView                                     │
│ text                                 Accessibility                                               │
│ content-desc                         -                                                           │
│ resource-id                          -                                                           │
│ enabled                              true                                                        │
│ bounds                               [0,210][1080,273]                                           │
│ displayed                            true                                                        │
└──────────────────────────────────────────────────────────────────────────────────────────────────┘

┌──────────────────────────────────────────────────────────────────────────────────────────────────┐
│ 📦 XML Block (Parent: ListView)                                                                  │
├──────────────────────────────────────────────────────────────────────────────────────────────────┤
│ <ListView>                                                                                       │
│   <TextView text="Accessibility"/>                                                               │
│   <TextView text="Animation"/>                                                                   │
│   <TextView text="App"/>                                                                         │
│   ...                                                                                            │
│ </ListView>                                                                                      │
└──────────────────────────────────────────────────────────────────────────────────────────────────┘
```

### Enable/Disable Inspector

You can control the inspector programmatically:

```java
// Disable inspector output
AndroidElementInspector.setEnabled(false);

// Enable inspector output (default)
AndroidElementInspector.setEnabled(true);

// Check current status
boolean isEnabled = AndroidElementInspector.isEnabled();
```

### Integration with BaseTest

The `BaseTest` class automatically integrates the inspector with all element finding methods:

```java
// These methods automatically trigger Inspector on NoSuchElementException
findElement(By locator)       // Single element
findElements(By locator)      // Multiple elements (triggers on empty list)
waitAndFind(By locator)       // Wait + find (triggers on timeout)
findByText(String text)       // Find by text attribute
findById(String resourceId)   // Find by resource-id
```

---

## Test Cases

The project includes 5 test cases:

### Successful Tests
| Test | Description |
|------|-------------|
| `testAccessibilityNavigation` | Navigate to Accessibility category |
| `testViewsButtonsNavigation` | Navigate to Views > Buttons |

### Inspector Tests (NoSuchElementException)
| Test | Description |
|------|-------------|
| `testInspector_NoSuchElement_1` | Search with wrong resource-id |
| `testInspector_NoSuchElement_2` | Search with typo in text |
| `testInspector_NoSuchElement_3` | Search with wrong XPath |

---

## Configuration

### Appium Server URL

Default: `http://127.0.0.1:4723/wd/hub`

To change, edit `BaseTest.java`:
```java
private static final String APPIUM_SERVER_URL = "http://YOUR_IP:PORT/wd/hub";
```

### App Configuration

Edit `BaseTest.java` to change the target app:
```java
.setAppPackage("your.app.package")
.setAppActivity("your.app.MainActivity")
```

---

## Troubleshooting

### Common Issues

| Error | Solution |
|-------|----------|
| `Connection refused` | Start Appium server: `appium --base-path /wd/hub` |
| `Response code 404` | Add `/wd/hub` to server URL |
| `No device found` | Check `adb devices` and start emulator |
| `App not installed` | Install ApiDemos-debug.apk on device |

### Verify Setup

```bash
# Check Appium
appium --version

# Check connected devices
adb devices

# Check Java
java -version

# Check Maven
mvn -version
```

---

## License

MIT License

## Author

Created for Android test automation with runtime element inspection capability.
