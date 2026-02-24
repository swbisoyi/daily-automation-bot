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

    static final List<TestConfig> TEST_CONFIGS = Arrays.asList(
            // 🌐 WEB TESTS
//             new TestConfig("HotKeySelect.json", "HotKeySelect.txt", "WEB"),
//             new TestConfig("login.json", "login.txt", "WEB"),
//             new TestConfig("page_objects.json", "test_scenario.txt", "WEB")

            // 📱 iOS REAL DEVICE TEST
//            new TestConfig("ios_login.json", "ios_login.txt", "IOS_REAL_DEVICE")

            // 🤖 Android Emulator
//            new TestConfig("android_login.json", "android_login.txt", "ANDROID")
            // 📱 Android Real Device
            new TestConfig("android_login.json", "android_login.txt", "ANDROID_REAL_DEVICE")

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

            // 🚀 THE MULTI-PLATFORM LAUNCHER
            if (config.platform.equalsIgnoreCase("WEB")) {
                actionLib.openBrowser();
            } else if (config.platform.equalsIgnoreCase("IOS_REAL_DEVICE")) {
                actionLib.openIOSRealDevice();
            } else if (config.platform.equalsIgnoreCase("ANDROID_EMULATOR")) {
                actionLib.openAndroidEmulator();
            } else if (config.platform.equalsIgnoreCase("ANDROID_REAL_DEVICE")) {
                actionLib.openAndroidRealDevice();
            }

            actionLib.startRecording();

            for (String step : steps) {
                if (step.trim().isEmpty() || step.startsWith("#") || step.startsWith("//")) continue;
                currentStep = step.trim();
                executeStep(actionLib, pageObjects, currentStep);
            }

            String msg = "✅ PASSED: " + config.scenarioFile;
            System.out.println(msg);
            actionLib.sendSlackNotification(msg);

        } catch (Exception e) {
            String msg = "🚨 FAILED: " + config.scenarioFile + " | Step: [" + currentStep + "]\nError: " + e.getMessage();
            System.err.println(msg);
            actionLib.sendSlackNotification(msg);
        } finally {
            actionLib.stopRecording(config.scenarioFile);
            actionLib.quit();
        }
    }

    public static void executeStep(ActionLibrary lib, JsonNode pageObjects, String step) throws Exception {
        if (step.equalsIgnoreCase("Open Browser")) return;
        if (step.startsWith("Navigate to")) { lib.navigate(step.substring(12).trim()); return; }
        if (step.startsWith("Wait for")) { lib.waitFor(Integer.parseInt(step.replaceAll("\\D", ""))); return; }
        if (step.startsWith("Tap on") || step.startsWith("Click on")) { lib.tap(getObj(pageObjects, step.replace("Tap on", "").replace("Click on", "").trim())); return; }
        if (step.startsWith("Type")) { lib.type(step.split(" in ")[0].substring(5), getObj(pageObjects, step.split(" in ")[1].trim())); return; }
        if (step.startsWith("Verify")) { lib.verifyVisible(getObj(pageObjects, step.substring(7).replace(" is visible", "").trim())); return; }
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