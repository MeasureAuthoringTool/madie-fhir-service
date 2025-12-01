package gov.cms.madie.madiefhirservice.utils;

import gov.cms.madie.models.library.CqlLibrary;
import gov.cms.madie.models.common.Version;
import gov.cms.mat.cql.CqlTextParser;
import gov.cms.mat.cql.elements.LibraryProperties;
import org.hl7.fhir.r4.model.Library;

import java.util.UUID;

public interface LibraryHelper {
  default Library createLibrary(String cql) {
    CqlTextParser cqlTextParser = new CqlTextParser(cql);

    LibraryProperties libraryProperties = cqlTextParser.getLibrary();

    Library library = new Library();
    library.setId(UUID.randomUUID().toString());
    library.setUrl("http://hapi.test.com/" + library.getId());

    library.setName(libraryProperties.getName());
    library.setVersion(libraryProperties.getVersion());
    library.addContent().setContentType("text/cql").setData(cql.getBytes());
    return library;
  }

  default CqlLibrary createCqlLibrary(String cql) {
    CqlTextParser cqlTextParser = new CqlTextParser(cql);
    LibraryProperties libraryProperties = cqlTextParser.getLibrary();

    CqlLibrary cqlLibrary =
        CqlLibrary.builder()
            .id(UUID.randomUUID().toString())
            .cqlLibraryName(libraryProperties.getName())
            .version(Version.parse(libraryProperties.getVersion()))
            .publisher("SemanticBits")
            .description("Test Description for this library")
            .experimental(true)
            .cql(cql)
            .build();
    return cqlLibrary;
  }
}
