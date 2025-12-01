package gov.cms.madie.madiefhirservice.cql;

import gov.cms.madie.madiefhirservice.utils.ResourceFileUtil;
import gov.cms.madie.madiefhirservice.utils.ResourceUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

@ExtendWith(MockitoExtension.class)
public class LibraryCqlVisitorTest implements ResourceFileUtil {
  private String cql;

  @BeforeEach
  void setUp() {
    cql = ResourceUtils.getData("/test-cql/cv_populations.cql");
  }

  @Test
  void testGetNameVersionFromInclude() {
    LibraryCqlVisitor cqlVisitor = new LibraryCqlVisitorFactory().visit(cql);
    List<Pair<String, String>> includedLibs = cqlVisitor.getIncludedLibraries();
    assertThat(includedLibs.size(), is(equalTo(3)));
    assertThat(includedLibs.get(0).getLeft(), is(equalTo("FHIRHelpers")));
    assertThat(includedLibs.get(1).getLeft(), is(equalTo("SupplementalDataElementsFHIR4")));
  }
}
