import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import io.appium.java_client.AppiumBy;
import io.appium.java_client.ios.IOSDriver;
import io.appium.java_client.ios.options.XCUITestOptions;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.Base64;
import java.util.Properties;

public class ActionLibrary {
    public WebDriver driver;
    private WebDriverWait wait;
    private Properties configProps;

    public ActionLibrary() {
        loadConfig();
    }

    // ==========================================
    // 📂 CONFIG LOADER
    // ==========================================
    private void loadConfig() {
        configProps = new Properties();
        try (InputStream input = ActionLibrary.class.getClassLoader().getResourceAsStream("config.properties")) {
            if (input != null) {
                configProps.load(input);
            }
        } catch (Exception e) {
            System.out.println("⚠️ Warning: config.properties issue - " + e.getMessage());
        }
    }

    // ==========================================
    // 🌐 1. WEB SETUP (Chrome)
    // ==========================================
    public void openBrowser() {
        System.out.println("   🌐 Launching Chrome...");
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--remote-allow-origins=*");
        options.addArguments("--start-maximized");
        driver = new ChromeDriver(options);
        initWait();
    }

    // ==========================================
    // 📲 2. iOS REAL DEVICE SETUP
    // ==========================================
    public void openIOSRealDevice() throws Exception {
        System.out.println("   📲 Connecting to Real iPhone...");

        XCUITestOptions options = new XCUITestOptions();
        options.setPlatformName("iOS");
        options.setAutomationName("XCUITest");

        options.setUdid("00008140-001259423C0B001C");
        options.setDeviceName("iPhone");
        options.setBundleId("com.justdial.justdialjd");

        // 🔐 Security & WebDriverAgent Settings
        options.setCapability("appium:xcodeOrgId", "P6ND3CJR5D");
        options.setCapability("appium:xcodeSigningId", "iPhone Developer");
        options.setCapability("appium:updatedWDABundleId", "com.swagat.WebDriverAgentRunner");
        options.setCapability("appium:usePrebuiltWDA", false); // Set to TRUE after your first successful run
        options.setCapability("appium:wdaLocalPort", 8102);

        options.setNoReset(true);
        options.setNewCommandTimeout(Duration.ofSeconds(60));

        driver = new IOSDriver(new URL("http://127.0.0.1:4723/"), options);
        initWait();
        System.out.println("   🚀 Real Device Connected!");
    }

    // ==========================================
    // 📱 3. iOS SIMULATOR SETUP (Safari Fallback)
    // ==========================================
    public void openIOSSimulator() throws Exception {
        System.out.println("   📱 Connecting to Simulator (Safari Mode)...");
        XCUITestOptions options = new XCUITestOptions();
        options.setDeviceName("iPhone 16e");
        options.setAutomationName("XCUITest");
        options.withBrowserName("Safari");

        driver = new IOSDriver(new URL("http://127.0.0.1:4723/"), options);
        initWait();
    }

    // ==========================================
    // 🎥 4. VIDEO RECORDING
    // ==========================================
    public void startRecording() {
        if (driver instanceof IOSDriver) {
            System.out.println("   🎥 Started iOS Screen Recording...");
            ((IOSDriver) driver).startRecordingScreen();
        } else {
            System.out.println("   🎥 (Web Video Recording skipped - Requires Monte Media Library)");
        }
    }

    public void stopRecording(String scenarioName) {
        if (driver instanceof IOSDriver) {
            System.out.println("   💾 Stopping recording and saving video...");
            try {
                String base64Video = ((IOSDriver) driver).stopRecordingScreen();
                byte[] decodedVideo = Base64.getDecoder().decode(base64Video);

                // Save to target folder
                String fileName = scenarioName.replace(".txt", "") + "_TestVideo.mp4";
                Path path = Paths.get("target/" + fileName);
                Files.createDirectories(path.getParent()); // Ensure directory exists
                Files.write(path, decodedVideo);

                System.out.println("   ✅ Video saved successfully: " + path.toAbsolutePath());
            } catch (Exception e) {
                System.err.println("   ❌ Failed to save video: " + e.getMessage());
            }
        }
    }

    // ==========================================
    // ⚙️ CORE ACTIONS
    // ==========================================
    private void initWait() {
        driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(5));
        wait = new WebDriverWait(driver, Duration.ofSeconds(15));
    }

    public void quit() {
        if (driver != null) {
            driver.quit();
            driver = null;
        }
    }

    public void navigate(String url) {
        System.out.println("   ➡️ Navigating to: " + url);
        driver.get(url);
    }

    public void tap(String locator) {
        System.out.println("   👆 Tapping: " + locator);
        wait.until(ExpectedConditions.elementToBeClickable(getLocator(locator))).click();
    }

    public void type(String text, String locator) {
        System.out.println("   ⌨️ Typing: " + text);
        WebElement el = wait.until(ExpectedConditions.visibilityOfElementLocated(getLocator(locator)));
        el.clear();
        el.sendKeys(text);
    }

    public void verifyVisible(String locator) {
        System.out.println("   👀 Verifying: " + locator);
        wait.until(ExpectedConditions.visibilityOfElementLocated(getLocator(locator)));
    }

    public void waitFor(int seconds) {
        try { Thread.sleep(seconds * 1000L); } catch (InterruptedException e) {}
    }

    private By getLocator(String raw) {
        if (raw.startsWith("id=")) return By.id(raw.substring(3));
        if (raw.startsWith("xpath=")) return By.xpath(raw.substring(6));
        if (raw.startsWith("accessId=")) return AppiumBy.accessibilityId(raw.substring(9));
        return By.xpath(raw); // Default to xpath
    }

    // ==========================================
    // 🔔 SLACK NOTIFICATION
    // ==========================================
    public void sendSlackNotification(String message) {
        String webhookUrl = (configProps != null) ? configProps.getProperty("slack.webhook.url") : null;
        if (webhookUrl == null || webhookUrl.isEmpty()) {
            webhookUrl = System.getenv("SLACK_WEBHOOK_URL");
        }

        if (webhookUrl == null || webhookUrl.isEmpty()) {
            System.out.println("   ⚠️ Slack Skipped: URL not found.");
            return;
        }

        try {
            String jsonPayload = "{\"text\": \"" + message.replace("\"", "'").replace("\n", "\\n") + "\"}";
            URL url = new URL(webhookUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setDoOutput(true);
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");

            try (OutputStream os = conn.getOutputStream()) {
                os.write(jsonPayload.getBytes("UTF-8"));
            }

            if (conn.getResponseCode() == 200) {
                System.out.println("   🔔 Slack Notification Sent!");
            } else {
                System.err.println("   ❌ Slack Failed: Code " + conn.getResponseCode());
            }
        } catch (Exception e) {
            System.err.println("   ❌ Slack Error: " + e.getMessage());
        }
    }
}