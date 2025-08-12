package gov.cms.madie.madiefhirservice.utils;

import com.vladsch.flexmark.html2md.converter.FlexmarkHtmlConverter;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.data.MutableDataSet;
import org.apache.commons.lang3.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;
import com.vladsch.flexmark.ext.gfm.strikethrough.StrikethroughExtension;
import com.vladsch.flexmark.ext.tables.TablesExtension;
import com.vladsch.flexmark.ext.ins.InsExtension;
import java.util.Arrays;

public class RichTextUtil {
  private static final Safelist RICH_TEXT_SAFE_LIST =
      Safelist.basic()
          .addTags("s", "br", "table", "tbody", "td", "th", "thead", "tr", "col", "colgroup", "del")
          .addAttributes("table", "style", "class", "id")
          .addAttributes("th", "rowspan", "colspan", "style", "colwidth")
          .addAttributes("td", "rowspan", "colspan", "style", "colwidth")
          .addAttributes("col", "style");

  public static String sanitizeText(String val) {
    if (StringUtils.isBlank(val)) {
      return val;
    }
    String safeHtml = Jsoup.clean(val, RICH_TEXT_SAFE_LIST);
    // br and col tags are not self-closing in html,
    // so we need to close them to make them wel-formed
    safeHtml = safeHtml.replaceAll("<br([^/>]*)>", "<br$1 />");
    safeHtml = safeHtml.replaceAll("<col ([^/>]*)>", "<col $1 />");
    return safeHtml;
  }

  public static String toMarkDown(String text) {
    if (StringUtils.isBlank(text)) {
      return text;
    }
    MutableDataSet options = new MutableDataSet();
    options.set(
        Parser.EXTENSIONS,
        Arrays.asList(
            TablesExtension.create(), StrikethroughExtension.create(), InsExtension.create()));
    FlexmarkHtmlConverter converter = FlexmarkHtmlConverter.builder(options).build();
    return converter.convert(text);
  }
}
