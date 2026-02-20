import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class Engine {

    // 🧠 CONFIGURATION: Toggle your tests here
    static final List<TestConfig> TEST_CONFIGS = Arrays.asList(
            // 🌐 WEB TESTS
            // new TestConfig("HotKeySelect.json", "HotKeySelect.txt", "WEB"),
            // new TestConfig("login.json", "login.txt", "WEB"),
            // new TestConfig("page_objects.json", "test_scenario.txt", "WEB"),

            // 📱 iOS REAL DEVICE TEST
            new TestConfig("ios_login.json", "ios_login.txt", "IOS_REAL_DEVICE")
    );

    public static void main(String[] args) {
        System.out.println("🚀 Engine Started at " + LocalDateTime.now());
        for (TestConfig config : TEST_CONFIGS) {
            runFullTest(config);
        }
        System.out.println("🏁 Execution Finished.");
    }

    public static void runFullTest(TestConfig config) {
        ActionLibrary actionLib = new ActionLibrary();
        String currentStep = "Initialization";

        try {
            System.out.println("--------------------------------------------------");
            System.out.println("🧵 Starting Scenario: " + config.scenarioFile + " [" + config.platform + "]");

            // 1. Load Data
            ObjectMapper mapper = new ObjectMapper();
            JsonNode pageObjects;
            try (InputStream is = Engine.class.getClassLoader().getResourceAsStream(config.jsonFile)) {
                if (is == null) throw new RuntimeException("❌ JSON File Not Found: " + config.jsonFile);
                pageObjects = mapper.readTree(is);
            }

            List<String> steps;
            try (InputStream is = Engine.class.getClassLoader().getResourceAsStream(config.scenarioFile)) {
                if (is == null) throw new RuntimeException("❌ Scenario File Not Found: " + config.scenarioFile);
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {
                    steps = reader.lines().collect(Collectors.toList());
                }
            }

            // 2. Launch Driver
            if (config.platform.equalsIgnoreCase("WEB")) {
                actionLib.openBrowser();
            } else if (config.platform.equalsIgnoreCase("IOS_REAL_DEVICE")) {
                actionLib.openIOSRealDevice();
            } else if (config.platform.equalsIgnoreCase("IOS_SIMULATOR")) {
                actionLib.openIOSSimulator();
            }

            // 🎥 3. START RECORDING
            actionLib.startRecording();

            // 4. Execute Steps
            for (String step : steps) {
                if (step.trim().isEmpty() || step.startsWith("#") || step.startsWith("//")) continue;

                currentStep = step.trim();
                executeStep(actionLib, pageObjects, currentStep);
            }

            // ✅ SUCCESS
            String msg = "✅ PASSED: " + config.scenarioFile;
            System.out.println(msg);
            actionLib.sendSlackNotification(msg);

        } catch (Exception e) {
            // 🚨 FAILURE
            String msg = "🚨 FAILED: " + config.scenarioFile + " | Step: [" + currentStep + "]\nError: " + e.getMessage();
            System.err.println(msg);
            actionLib.sendSlackNotification(msg);
        } finally {
            // 🎥 5. STOP RECORDING & SAVE
            actionLib.stopRecording(config.scenarioFile);

            actionLib.quit();
        }
    }

    public static void executeStep(ActionLibrary lib, JsonNode pageObjects, String step) throws Exception {
        // Skip explicit browser opening
        if (step.equalsIgnoreCase("Open Browser")) {
            System.out.println("   ℹ️ Skipping 'Open Browser' step (Already launched)");
            return;
        }

        if (step.startsWith("Navigate to")) {
            lib.navigate(step.substring(12).trim());
            return;
        }

        if (step.startsWith("Wait for")) {
            lib.waitFor(Integer.parseInt(step.replaceAll("\\D", "")));
            return;
        }

        if (step.startsWith("Tap on") || step.startsWith("Click on")) {
            String obj = step.replace("Tap on", "").replace("Click on", "").trim();
            lib.tap(getObj(pageObjects, obj));
            return;
        }

        if (step.startsWith("Type")) {
            String text = step.split(" in ")[0].substring(5);
            String obj = step.split(" in ")[1].trim();
            lib.type(text, getObj(pageObjects, obj));
            return;
        }

        if (step.startsWith("Verify")) {
            String obj = step.substring(7).replace(" is visible", "").trim();
            lib.verifyVisible(getObj(pageObjects, obj));
            return;
        }

        throw new Exception("Unknown Command: " + step);
    }

    private static String getObj(JsonNode pageObjects, String key) throws Exception {
        if (!pageObjects.has(key)) throw new Exception("Object '" + key + "' not found in JSON.");
        return pageObjects.get(key).asText();
    }

    static class TestConfig {
        String jsonFile, scenarioFile, platform;
        TestConfig(String j, String s, String p) { jsonFile=j; scenarioFile=s; platform=p; }
    }
}