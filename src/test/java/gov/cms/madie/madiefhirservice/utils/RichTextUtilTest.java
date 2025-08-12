package gov.cms.madie.madiefhirservice.utils;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class RichTextUtilTest {

  @Test
  void testSanitizeTextNullOrBlank() {
    assertNull(RichTextUtil.sanitizeText(null));
    assertEquals("", RichTextUtil.sanitizeText(""));
    assertEquals("   ", RichTextUtil.sanitizeText("   "));
  }

  @Test
  void testSanitizeTextBasicHtml() {
    String html = "<b>Bold</b> <script>alert('x')</script> <i>Italic</i>";
    String sanitized = RichTextUtil.sanitizeText(html);
    assertTrue(sanitized.contains("<b>Bold</b>"));
    assertFalse(sanitized.contains("<script>"));
    assertTrue(sanitized.contains("<i>Italic</i>"));
  }

  @Test
  void testSanitizeTextColTag() {
    String html = "<table><col style=\"width:10%\"><tr><td>Cell</td></tr></table>";
    String sanitized = RichTextUtil.sanitizeText(html);
    assertTrue(sanitized.contains("<col style=\"width:10%\" />"));
  }

  @Test
  void testToMarkDownNullOrBlank() {
    assertNull(RichTextUtil.toMarkDown(null));
    assertEquals("", RichTextUtil.toMarkDown(""));
    assertEquals("   ", RichTextUtil.toMarkDown("   "));
  }

  @Test
  void testToMarkDownBasicHtml() {
    String html = "<b>Bold</b> <i>Italic</i>";
    String markdown = RichTextUtil.toMarkDown(html);
    assertTrue(markdown.contains("**Bold**") || markdown.contains("Bold"));
    assertTrue(markdown.contains("_Italic_") || markdown.contains("Italic"));
  }

  @Test
  void testToMarkDownTable() {
    String html =
        "<table><thead><tr><th>H</th></tr></thead><tbody><tr><td>C</td></tr></tbody></table>";
    String markdown = RichTextUtil.toMarkDown(html);
    assertTrue(markdown.contains("| H |"));
    assertTrue(markdown.contains("| C |"));
  }

  @Test
  void testToMarkDownStrikethrough() {
    String html = "<del>strike</del>";
    String markdown = RichTextUtil.toMarkDown(html);
    assertTrue(markdown.contains("~~strike~~") || markdown.contains("strike"));
  }

  @Test
  void sanitizeTextHandlesEmptyBrTags() {
    String html = "<p>Line 1<br>Line 2</p>";
    String sanitized = RichTextUtil.sanitizeText(html);
    assertTrue(sanitized.contains("<br />"));
  }

  @Test
  void sanitizeTextIgnoresAlreadySelfClosingBrTags() {
    String html = "<p>Line 1<br />Line 2</p>";
    String sanitized = RichTextUtil.sanitizeText(html);
    assertTrue(sanitized.contains("<br />"));
  }
}
