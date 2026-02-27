import time
import re
import json
import os
from google import genai

LOG_FILE = "appium.log"
JSON_FILE = "locators.json"
TXT_FILE = "steps.txt"

# Paste your API key here!
client = genai.Client(api_key="AIzaSyB3Fec45_yMNH72xbQ1S1jSvBT956bK06M")

element_memory = {}
locator_repo = {}

def get_smart_name(xpath):
    fallback_match = re.search(r'@name=[\'"]([^\'"]+)[\'"]', xpath)
    if fallback_match:
        fallback_name = fallback_match.group(1).replace(" ", "")
    else:
        fallback_name = f"DynamicElement_{len(locator_repo) + 1}"

    prompt = f"Return a PascalCase variable name for this iOS Appium XPath. Append 'Button', 'Field', etc. Return ONLY the string. XPath: {xpath}"

    try:
        response = client.models.generate_content(model='gemini-2.5-flash', contents=prompt)
        ai_name = response.text.strip()
        if len(ai_name) < 40 and " " not in ai_name:
            return ai_name
        return fallback_name
    except:
        return fallback_name

def write_files(action_text=None):
    with open(JSON_FILE, 'w') as jf:
        json.dump(locator_repo, jf, indent=2)

    if action_text:
        with open(TXT_FILE, 'a') as tf:
            tf.write(action_text + "\n")
        print(f"✅ SUCCESSFULLY SAVED: {action_text}")

def start_agent():
    print("🤖 AI Agent Starting...")
    if not os.path.exists(LOG_FILE):
        open(LOG_FILE, 'w').close()

    open(JSON_FILE, 'w').write("{}")
    open(TXT_FILE, 'w').write("")

    print("🎧 Scanning log history and listening for new commands...")

    with open(LOG_FILE, "r") as file:
        # Notice we removed the "seek to end" line so it reads the past logs!
        last_xpath = None

        while True:
            line = file.readline()
            if not line:
                time.sleep(0.1)
                continue

            # 1. Catching XPath (Now ignores spaces!)
            match_xpath = re.search(r'"value"\s*:\s*"(//[^"]+)"', line)
            if match_xpath:
                last_xpath = match_xpath.group(1)
                print(f"👀 [DEBUG] Found XPath in logs: {last_xpath}")

            # 2. Catching Element ID (W3C format)
            match_id = re.search(r'"(?:element-6066-11e4-a52e-4f735466cecf|ELEMENT)"\s*:\s*"([^"]+)"', line)
            if match_id and last_xpath:
                element_id = match_id.group(1)
                element_name = get_smart_name(last_xpath)

                element_memory[element_id] = element_name
                locator_repo[element_name] = f"xpath={last_xpath}"
                write_files()
                print(f"📌 [DEBUG] Saved Locator: {element_name}")
                last_xpath = None

            # 3. Catching Clicks
            match_click = re.search(r'/element/([^/]+)/click', line)
            if match_click:
                element_id = match_click.group(1)
                print(f"👆 [DEBUG] Found Click for ID: {element_id[:8]}...")
                if element_id in element_memory:
                    name = element_memory[element_id]
                    write_files(f"Tap on {name}\nWait for 2")

            # 4. Catching Typing
            match_type_id = re.search(r'/element/([^/]+)/value', line)
            if match_type_id:
                element_id = match_type_id.group(1)

            match_text = re.search(r'"text"\s*:\s*"([^"]+)"', line)
            if match_text and 'element_id' in locals():
                typed_text = match_text.group(1)
                if element_id in element_memory:
                    name = element_memory[element_id]
                    write_files(f"Type {typed_text} in {name}\nWait for 1")
                    del locals()['element_id'] # Clear it

if __name__ == "__main__":
    start_agent()