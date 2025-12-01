package gov.cms.madie.madiefhirservice.cql;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

@Slf4j
public class CqlUtils {
  public static final String BLOCK_COMMENT_END = "*/";
  public static final int BLK_SEP_LENGTH = BLOCK_COMMENT_END.length();

  /**
   * @param s The string to chomp.
   * @return Removes 1 character from the front end and end of the string.
   */
  public static String chomp1(String s) {
    return s.length() >= BLK_SEP_LENGTH ? s.substring(1, s.length() - 1) : s;
  }

  public static boolean isQuoted(String s) {
    return StringUtils.isNotBlank(s) && s.startsWith("\"") && s.endsWith("\"");
  }

  public static boolean isSingleQuoted(String s) {
    return StringUtils.isNotBlank(s) && s.startsWith("'") && s.endsWith("'");
  }

  public static String unquote(String fullText) {
    return (isQuoted(fullText) || isSingleQuoted(fullText)) ? chomp1(fullText) : fullText;
  }
}
