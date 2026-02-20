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

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.Duration;

public class ActionLibrary {
    public WebDriver driver;
    private WebDriverWait wait;

    // ==========================================
    // 🌐 1. WEB SETUP
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

        // Critical Real Device Caps
        options.setCapability("appium:xcodeOrgId", "P6ND3CJR5D");
        options.setCapability("appium:updatedWDABundleId", "com.swagat.WebDriverAgentRunner");
        options.setCapability("appium:usePrebuiltWDA", true);
        options.setCapability("appium:wdaLocalPort", 8102);

        options.setNoReset(true);
        options.setNewCommandTimeout(Duration.ofSeconds(60));

        driver = new IOSDriver(new URL("http://127.0.0.1:4723/"), options);
        initWait();
        System.out.println("   🚀 Real Device Connected!");
    }

    // ==========================================
    // 📱 3. iOS SIMULATOR SETUP
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
    // ⚙️ CORE ACTIONS
    // ==========================================
    private void initWait() {
        driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(5));
        wait = new WebDriverWait(driver, Duration.ofSeconds(10));
    }

    public void quit() {
        if (driver != null) {
            driver.quit();
            driver = null;
        }
    }

    public void navigate(String url) { driver.get(url); }

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
        return By.xpath(raw);
    }

    // ==========================================
    // 🔔 SLACK NOTIFICATION (ENV VAR)
    // ==========================================
    public void sendSlackNotification(String message) {
        // 1. READ FROM ENVIRONMENT VARIABLE
        String webhookUrl = System.getenv("SLACK_WEBHOOK_URL");

        if (webhookUrl == null || webhookUrl.isEmpty()) {
            System.err.println("   ⚠️ Slack Skipped: Env Var 'SLACK_WEBHOOK_URL' is missing.");
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

            int responseCode = conn.getResponseCode();
            if (responseCode == 200) {
                System.out.println("   🔔 Slack Notification Sent!");
            } else {
                System.err.println("   ❌ Slack Failed: Code " + responseCode);
            }
        } catch (Exception e) {
            System.err.println("   ❌ Slack Error: " + e.getMessage());
        }
    }
}