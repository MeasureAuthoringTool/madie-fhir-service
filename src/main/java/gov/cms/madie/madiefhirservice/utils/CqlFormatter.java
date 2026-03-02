package gov.cms.madie.madiefhirservice.utils;

import gov.cms.mat.cql.exceptions.CqlFormatterException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.cqframework.cql.tools.formatter.CqlFormatterVisitor;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.Principal;

@Slf4j
@RequiredArgsConstructor
public final class CqlFormatter {

  public static String formatCql(String cqlData, Principal principal) {
    try (var cqlDataStream = new ByteArrayInputStream(cqlData.getBytes())) {
      CqlFormatterVisitor.FormatResult formatResult =
          CqlFormatterVisitor.Companion.getFormattedOutput(cqlDataStream);
      if (formatResult.getErrors() != null && !formatResult.getErrors().isEmpty()) {
        log.info("User [{}] requested to format the CQL, but errors found", principal.getName());
        throw new CqlFormatterException(
            "Unable to format CQL, because one or more Syntax errors are identified");
      }
      log.info("User [{}] successfully formatted the CQL", principal.getName());
      return formatResult.getOutput();
    } catch (IOException e) {
      log.info("User [{}] is unable to format the CQL", principal.getName());
      throw new CqlFormatterException(e.getMessage());
    }
  }
}
