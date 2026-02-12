import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Engine {

    static final List<TestConfig> TEST_CONFIGS = Arrays.asList(
            new TestConfig("HotKeySelect.json", "HotKeySelect.txt"),
            new TestConfig("login.json", "login.txt"),
            new TestConfig("page_objects.json", "test_scenario.txt")
    );

    public static void main(String[] args) {
        System.out.println("🚀 GitHub Actions Triggered Execution at " + LocalDateTime.now());
        triggerParallelTests();
    }

    public static void triggerParallelTests() {
        ExecutorService parallelExecutor = Executors.newFixedThreadPool(TEST_CONFIGS.size());

        for (TestConfig config : TEST_CONFIGS) {
            parallelExecutor.execute(() -> runFullTest(config));
        }

        parallelExecutor.shutdown();
        try {
            if (!parallelExecutor.awaitTermination(15, TimeUnit.MINUTES)) {
                parallelExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            parallelExecutor.shutdownNow();
        }
    }

    public static void runFullTest(TestConfig config) {
        ActionLibrary actionLib = new ActionLibrary();
        JsonNode pageObjects;
        String currentStep = "Initialization";

        try {
            System.out.println("🧵 Starting: " + config.scenarioFile);
            ObjectMapper mapper = new ObjectMapper();
            pageObjects = mapper.readTree(new File(config.jsonFile));
            List<String> steps = Files.readAllLines(Paths.get(config.scenarioFile));

            actionLib.openBrowser();

            for (String step : steps) {
                if (step.trim().isEmpty()) continue;
                currentStep = step.trim();
                executeStep(actionLib, pageObjects, currentStep);
            }
            System.out.println("✅ PASSED: " + config.scenarioFile);
            actionLib.sendSlackNotification("✅ PASSED: " + config.scenarioFile);

        } catch (Exception e) {
            System.out.println("🚨 FAILED: " + config.scenarioFile);
            e.printStackTrace();
            actionLib.sendSlackNotification("🚨 FAILED: " + config.scenarioFile);
        } finally {
            actionLib.closeBrowser();
        }
    }

    public static void executeStep(ActionLibrary actionLib, JsonNode pageObjects, String step) throws Exception {
        if (step.equals("Open Browser")) return;
        Matcher navMatch = Pattern.compile("Navigate to (.*)").matcher(step);
        if (navMatch.find()) { actionLib.navigate(navMatch.group(1)); return; }

        Matcher verifyMatch = Pattern.compile("Verify (.*) is visible").matcher(step);
        if (verifyMatch.find()) {
            String name = verifyMatch.group(1).trim();
            if (pageObjects.get(name) == null) throw new Exception("Object '" + name + "' not found.");
            actionLib.verifyVisible(pageObjects.get(name).asText());
            return;
        }

        Matcher tapMatch = Pattern.compile("Tap on (.*)").matcher(step);
        if (tapMatch.find()) {
            String name = tapMatch.group(1).trim();
            if (pageObjects.get(name) == null) throw new Exception("Object '" + name + "' not found.");
            actionLib.tap(pageObjects.get(name).asText());
            return;
        }

        Matcher typeMatch = Pattern.compile("Type (.*) in (.*)").matcher(step);
        if (typeMatch.find()) {
            String text = typeMatch.group(1).trim();
            String name = typeMatch.group(2).trim();
            if (pageObjects.get(name) == null) throw new Exception("Object '" + name + "' not found.");
            actionLib.type(text, pageObjects.get(name).asText());
            return;
        }

        Matcher waitMatch = Pattern.compile("Wait for (\\d+) seconds").matcher(step);
        if (waitMatch.find()) { actionLib.waitFor(Integer.parseInt(waitMatch.group(1))); return; }
    }

    static class TestConfig {
        String jsonFile, scenarioFile;
        TestConfig(String json, String scenario) { this.jsonFile = json; this.scenarioFile = scenario; }
    }
}