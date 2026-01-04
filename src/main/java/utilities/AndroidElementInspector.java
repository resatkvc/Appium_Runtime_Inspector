package utilities;

import io.appium.java_client.AppiumDriver;
import io.appium.java_client.android.AndroidDriver;
import org.openqa.selenium.NoSuchElementException;
import org.w3c.dom.*;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * AndroidElementInspector - Runtime Element Inspector for Appium Android Tests
 *
 * When a NoSuchElementException occurs, this inspector automatically:
 * 1. Captures the current page source (XML hierarchy)
 * 2. Finds the closest matching element using a scoring algorithm
 * 3. Displays locator suggestions (accessibility id, id, uiautomator, xpath)
 * 4. Shows all element attributes in a formatted table
 * 5. Prints the parent XML block for context
 *
 * Usage:
 *   - Inspector is enabled by default
 *   - Disable: AndroidElementInspector.setEnabled(false)
 *   - Enable:  AndroidElementInspector.setEnabled(true)
 *   - Check:   AndroidElementInspector.isEnabled()
 */
public class AndroidElementInspector {

    // Enable/Disable inspector output (default: true)
    private static boolean enabled = true;

    // ANSI Color Codes for terminal output
    private static final String RESET = "\u001B[0m";
    private static final String RED = "\u001B[31m";
    private static final String GREEN = "\u001B[32m";
    private static final String YELLOW = "\u001B[33m";
    private static final String BLUE = "\u001B[34m";
    private static final String CYAN = "\u001B[36m";
    private static final String WHITE = "\u001B[37m";
    private static final String BOLD = "\u001B[1m";
    private static final String DIM = "\u001B[2m";
    private static final String MAGENTA = "\u001B[35m";

    /**
     * Inspects the current page when NoSuchElementException occurs
     *
     * @param driver    The AndroidDriver instance
     * @param locator   The locator string that failed
     * @param exception The NoSuchElementException that was thrown
     */
    public static void inspect(AppiumDriver driver, String locator, NoSuchElementException exception) {
        if (!enabled) return;
        if (driver == null || !(driver instanceof AndroidDriver)) return;

        try {
            String pageSource = driver.getPageSource();
            if (pageSource == null || pageSource.isEmpty()) return;

            Document doc = parseXml(pageSource);
            if (doc == null) return;

            String searchTerm = extractSearchTerm(locator);
            ElementMatch bestMatch = findBestMatch(doc.getDocumentElement(), searchTerm);

            printInspectorOutput(locator, bestMatch);

        } catch (Exception e) {
            System.err.println("[Inspector Error] " + e.getMessage());
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════════
    // ELEMENT MATCH - Stores matched element data
    // ═══════════════════════════════════════════════════════════════════════════════

    private static class ElementMatch {
        Element element;
        int score;
        String className;
        String text;
        String resourceId;
        String contentDesc;
        String enabled;
        String index;
        String packageName;
        String bounds;
        String displayed;

        ElementMatch(Element e, int score) {
            this.element = e;
            this.score = score;
            this.className = e.getTagName();
            this.text = getAttr(e, "text");
            this.resourceId = getAttr(e, "resource-id");
            this.contentDesc = getAttr(e, "content-desc");
            this.enabled = getAttr(e, "enabled");
            this.index = getAttr(e, "index");
            this.packageName = getAttr(e, "package");
            this.bounds = getAttr(e, "bounds");
            this.displayed = getAttr(e, "displayed");
        }

        private String getAttr(Element e, String name) {
            String val = e.getAttribute(name);
            return val != null ? val : "";
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════════
    // FIND BEST MATCHING ELEMENT
    // ═══════════════════════════════════════════════════════════════════════════════

    private static ElementMatch findBestMatch(Node node, String searchTerm) {
        List<ElementMatch> matches = new ArrayList<>();
        collectMatches(node, searchTerm, matches);

        if (matches.isEmpty()) return null;

        // Sort by score descending and return the best match
        matches.sort((a, b) -> b.score - a.score);
        return matches.get(0);
    }

    private static void collectMatches(Node node, String searchTerm, List<ElementMatch> matches) {
        if (node.getNodeType() == Node.ELEMENT_NODE) {
            Element e = (Element) node;
            int score = calculateScore(e, searchTerm);
            if (score > 0) {
                matches.add(new ElementMatch(e, score));
            }
        }
        NodeList children = node.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            collectMatches(children.item(i), searchTerm, matches);
        }
    }

    private static int calculateScore(Element e, String searchTerm) {
        if (searchTerm == null || searchTerm.isEmpty()) return 0;

        String search = searchTerm.toLowerCase().trim();
        String text = e.getAttribute("text");
        String resourceId = e.getAttribute("resource-id");
        String contentDesc = e.getAttribute("content-desc");
        String className = e.getTagName();

        if (text == null) text = "";
        if (resourceId == null) resourceId = "";
        if (contentDesc == null) contentDesc = "";
        if (className == null) className = "";

        int score = 0;

        // Exact match: highest priority (1000 points)
        if (text.equalsIgnoreCase(searchTerm)) score += 1000;
        if (contentDesc.equalsIgnoreCase(searchTerm)) score += 1000;
        if (resourceId.equalsIgnoreCase(searchTerm)) score += 1000;
        if (resourceId.toLowerCase().endsWith(":id/" + search)) score += 900;
        if (resourceId.toLowerCase().endsWith("/" + search)) score += 800;

        // Contains match: medium priority (500 points)
        if (text.toLowerCase().contains(search)) score += 500;
        if (contentDesc.toLowerCase().contains(search)) score += 500;
        if (resourceId.toLowerCase().contains(search)) score += 400;
        if (className.toLowerCase().contains(search)) score += 300;

        // Prefix similarity: lower priority
        score += prefixMatch(text.toLowerCase(), search) * 5;
        score += prefixMatch(contentDesc.toLowerCase(), search) * 5;
        score += prefixMatch(resourceId.toLowerCase(), search) * 3;

        return score;
    }

    private static int prefixMatch(String s1, String s2) {
        if (s1.isEmpty() || s2.isEmpty()) return 0;
        int count = 0;
        int len = Math.min(s1.length(), s2.length());
        for (int i = 0; i < len; i++) {
            if (s1.charAt(i) == s2.charAt(i)) count++;
            else break;
        }
        return count;
    }

    // ═══════════════════════════════════════════════════════════════════════════════
    // EXTRACT SEARCH TERM FROM LOCATOR
    // ═══════════════════════════════════════════════════════════════════════════════

    private static String extractSearchTerm(String locator) {
        if (locator == null || locator.isEmpty()) return "";

        // By.id: io.appium.android.apis:id/button_id -> button_id
        if (locator.contains("By.id:")) {
            String id = locator.replace("By.id:", "").trim();
            if (id.contains(":id/")) {
                return id.substring(id.lastIndexOf("/") + 1);
            }
            return id;
        }

        // By.xpath: ... [@text='xxx'] -> xxx
        Pattern textPattern = Pattern.compile("@text\\s*=\\s*['\"]([^'\"]+)['\"]");
        Matcher textMatcher = textPattern.matcher(locator);
        if (textMatcher.find()) {
            return textMatcher.group(1);
        }

        // By.xpath: ... [@resource-id='xxx'] -> xxx
        Pattern idPattern = Pattern.compile("@resource-id\\s*=\\s*['\"]([^'\"]+)['\"]");
        Matcher idMatcher = idPattern.matcher(locator);
        if (idMatcher.find()) {
            String id = idMatcher.group(1);
            if (id.contains(":id/")) {
                return id.substring(id.lastIndexOf("/") + 1);
            }
            return id;
        }

        // By.xpath: ... [@content-desc='xxx'] -> xxx
        Pattern descPattern = Pattern.compile("@content-desc\\s*=\\s*['\"]([^'\"]+)['\"]");
        Matcher descMatcher = descPattern.matcher(locator);
        if (descMatcher.find()) {
            return descMatcher.group(1);
        }

        return locator;
    }

    // ═══════════════════════════════════════════════════════════════════════════════
    // PRINT INSPECTOR OUTPUT
    // ═══════════════════════════════════════════════════════════════════════════════

    private static void printInspectorOutput(String locator, ElementMatch match) {
        StringBuilder sb = new StringBuilder();

        // Header
        sb.append("\n\n");
        sb.append(RED).append("╔══════════════════════════════════════════════════════════════════════════════════════════════════╗").append(RESET).append("\n");
        sb.append(RED).append("║ ").append(BOLD).append("🔍 ANDROID ELEMENT INSPECTOR").append(RESET).append(RED).append("                                                                    ║").append(RESET).append("\n");
        sb.append(RED).append("╚══════════════════════════════════════════════════════════════════════════════════════════════════╝").append(RESET).append("\n\n");

        // Exception and Target
        sb.append(YELLOW).append("⚠️  EXCEPTION: ").append(RESET).append(RED).append("NoSuchElementException").append(RESET).append("\n");
        sb.append(YELLOW).append("📍 TARGET LOCATOR: ").append(RESET).append(WHITE).append(locator).append(RESET).append("\n");

        if (match == null) {
            sb.append(RED).append("\n❌ NO SIMILAR ELEMENT FOUND ON PAGE!").append(RESET).append("\n\n");
            System.out.println(sb);
            return;
        }

        sb.append(GREEN).append("✅ CLOSEST MATCHING ELEMENT FOUND").append(RESET).append("\n");

        // Find By / Selector Table
        sb.append("\n");
        sb.append(CYAN).append("┌──────────────────────────────────────────────────────────────────────────────────────────────────┐").append(RESET).append("\n");
        sb.append(CYAN).append("│ ").append(BOLD).append("Find By                              Selector").append(RESET).append("                             ").append(CYAN).append("│").append(RESET).append("\n");
        sb.append(CYAN).append("├──────────────────────────────────────────────────────────────────────────────────────────────────┤").append(RESET).append("\n");

        if (!match.contentDesc.isEmpty()) {
            appendTableRow(sb, CYAN, "accessibility id", match.contentDesc);
        }
        if (!match.resourceId.isEmpty()) {
            appendTableRow(sb, CYAN, "id", match.resourceId);
            appendTableRow(sb, CYAN, "-android uiautomator", "new UiSelector().resourceId(\"" + match.resourceId + "\")");
        }
        if (!match.text.isEmpty()) {
            appendTableRow(sb, CYAN, "-android uiautomator", "new UiSelector().text(\"" + match.text + "\")");
        }

        // XPath
        String xpath = buildXpath(match);
        appendTableRow(sb, CYAN, "xpath", xpath);

        sb.append(CYAN).append("└──────────────────────────────────────────────────────────────────────────────────────────────────┘").append(RESET).append("\n");

        // Attribute / Value Table
        sb.append("\n");
        sb.append(MAGENTA).append("┌──────────────────────────────────────────────────────────────────────────────────────────────────┐").append(RESET).append("\n");
        sb.append(MAGENTA).append("│ ").append(BOLD).append("Attribute                            Value").append(RESET).append("                                ").append(MAGENTA).append("│").append(RESET).append("\n");
        sb.append(MAGENTA).append("├──────────────────────────────────────────────────────────────────────────────────────────────────┤").append(RESET).append("\n");

        appendAttrRow(sb, "index", match.index);
        appendAttrRow(sb, "package", match.packageName);
        appendAttrRow(sb, "class", match.className);
        appendAttrRow(sb, "text", match.text);
        appendAttrRow(sb, "content-desc", match.contentDesc);
        appendAttrRow(sb, "resource-id", match.resourceId);
        appendAttrRow(sb, "enabled", match.enabled);
        appendAttrRow(sb, "bounds", match.bounds);
        appendAttrRow(sb, "displayed", match.displayed);

        sb.append(MAGENTA).append("└──────────────────────────────────────────────────────────────────────────────────────────────────┘").append(RESET).append("\n");

        // XML Block
        Element xmlElement = findParentContainer(match.element);
        if (xmlElement == null) {
            xmlElement = match.element;
        }

        sb.append("\n");
        sb.append(BLUE).append("┌──────────────────────────────────────────────────────────────────────────────────────────────────┐").append(RESET).append("\n");
        sb.append(BLUE).append("│ ").append(BOLD).append("📦 XML Block (Parent: ").append(getShortClassName(xmlElement.getTagName())).append(")").append(RESET).append("\n");
        sb.append(BLUE).append("├──────────────────────────────────────────────────────────────────────────────────────────────────┤").append(RESET).append("\n");

        String xmlOutput = buildXmlBlock(xmlElement, 0, 4);
        for (String line : xmlOutput.split("\n")) {
            if (!line.trim().isEmpty()) {
                sb.append(BLUE).append("│ ").append(RESET).append(DIM).append(line).append(RESET).append("\n");
            }
        }

        sb.append(BLUE).append("└──────────────────────────────────────────────────────────────────────────────────────────────────┘").append(RESET).append("\n\n");

        System.out.println(sb);
    }

    private static void appendTableRow(StringBuilder sb, String color, String findBy, String selector) {
        String selectorShort = selector.length() > 55 ? selector.substring(0, 52) + "..." : selector;
        sb.append(color).append("│ ").append(RESET);
        sb.append(String.format("%-36s", findBy));
        sb.append(GREEN).append(selectorShort).append(RESET).append("\n");
    }

    private static void appendAttrRow(StringBuilder sb, String attr, String value) {
        sb.append(MAGENTA).append("│ ").append(RESET);
        sb.append(String.format("%-36s", attr));
        if (value == null || value.isEmpty()) {
            sb.append(DIM).append("-").append(RESET);
        } else {
            String valueShort = value.length() > 50 ? value.substring(0, 47) + "..." : value;
            sb.append(WHITE).append(valueShort).append(RESET);
        }
        sb.append("\n");
    }

    private static String buildXpath(ElementMatch match) {
        String shortClass = getShortClassName(match.className);
        if (!match.contentDesc.isEmpty()) {
            return "//" + shortClass + "[@content-desc=\"" + match.contentDesc + "\"]";
        } else if (!match.resourceId.isEmpty()) {
            return "//*[@resource-id=\"" + match.resourceId + "\"]";
        } else if (!match.text.isEmpty()) {
            return "//" + shortClass + "[@text=\"" + match.text + "\"]";
        }
        return "//" + shortClass;
    }

    // ═══════════════════════════════════════════════════════════════════════════════
    // XML BLOCK BUILDER
    // ═══════════════════════════════════════════════════════════════════════════════

    private static Element findParentContainer(Element e) {
        Node parent = e.getParentNode();
        while (parent != null && parent.getNodeType() == Node.ELEMENT_NODE) {
            String tag = ((Element) parent).getTagName().toLowerCase();
            if (tag.contains("layout") || tag.contains("viewgroup") || tag.contains("view") ||
                tag.contains("scroll") || tag.contains("list") || tag.contains("recycler") ||
                tag.contains("frame") || tag.contains("linear") || tag.contains("relative") ||
                tag.contains("constraint")) {
                return (Element) parent;
            }
            parent = parent.getParentNode();
        }
        return null;
    }

    private static String buildXmlBlock(Element element, int indent, int maxDepth) {
        if (maxDepth < 0) return spaces(indent) + "...\n";

        StringBuilder sb = new StringBuilder();
        String tag = getShortClassName(element.getTagName());
        sb.append(spaces(indent)).append("<").append(tag);

        appendXmlAttr(sb, element, "text");
        appendXmlAttr(sb, element, "resource-id");
        appendXmlAttr(sb, element, "content-desc");

        List<Element> children = getChildElements(element);

        if (children.isEmpty()) {
            sb.append("/>\n");
        } else {
            sb.append(">\n");
            for (Element child : children) {
                sb.append(buildXmlBlock(child, indent + 2, maxDepth - 1));
            }
            sb.append(spaces(indent)).append("</").append(tag).append(">\n");
        }

        return sb.toString();
    }

    private static List<Element> getChildElements(Element parent) {
        List<Element> children = new ArrayList<>();
        NodeList nodes = parent.getChildNodes();
        for (int i = 0; i < nodes.getLength(); i++) {
            if (nodes.item(i).getNodeType() == Node.ELEMENT_NODE) {
                children.add((Element) nodes.item(i));
            }
        }
        return children;
    }

    private static void appendXmlAttr(StringBuilder sb, Element e, String attrName) {
        String value = e.getAttribute(attrName);
        if (value != null && !value.isEmpty()) {
            String shortValue = value.length() > 35 ? value.substring(0, 32) + "..." : value;
            if (attrName.equals("resource-id") && shortValue.contains(":id/")) {
                shortValue = shortValue.substring(shortValue.lastIndexOf("/") + 1);
            }
            sb.append(" ").append(attrName).append("=\"").append(shortValue).append("\"");
        }
    }

    private static String getShortClassName(String className) {
        if (className == null) return "Unknown";
        if (className.contains(".")) {
            return className.substring(className.lastIndexOf(".") + 1);
        }
        return className;
    }

    private static String spaces(int count) {
        return " ".repeat(Math.max(0, count));
    }

    // ═══════════════════════════════════════════════════════════════════════════════
    // XML PARSER
    // ═══════════════════════════════════════════════════════════════════════════════

    private static Document parseXml(String xml) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            DocumentBuilder builder = factory.newDocumentBuilder();
            return builder.parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            return null;
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════════
    // ENABLE / DISABLE INSPECTOR
    // ═══════════════════════════════════════════════════════════════════════════════

    /**
     * Enable or disable the inspector output
     * @param isEnabled true to enable, false to disable
     */
    public static void setEnabled(boolean isEnabled) {
        enabled = isEnabled;
    }

    /**
     * Check if the inspector is currently enabled
     * @return true if enabled, false otherwise
     */
    public static boolean isEnabled() {
        return enabled;
    }
}
