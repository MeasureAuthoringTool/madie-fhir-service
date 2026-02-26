package gov.cms.madie.madiefhirservice.cql;

import org.antlr.v4.kotlinruntime.CommonTokenStream;
import org.antlr.v4.kotlinruntime.StringCharStream;
import org.cqframework.cql.gen.cqlLexer;
import org.cqframework.cql.gen.cqlParser;
import org.springframework.stereotype.Service;

@Service
public class LibraryCqlVisitorFactory {

  public LibraryCqlVisitor visit(String cql) {
    LibraryCqlVisitor result = new LibraryCqlVisitor();
    cqlParser.LibraryContext ctx = getLibraryContext(cql);
    result.visit(ctx);
    return result;
  }

  public static cqlParser.LibraryContext getLibraryContext(String cql) {
    cqlLexer lexer = new cqlLexer(new StringCharStream(cql, "cql"));
    CommonTokenStream tokens = new CommonTokenStream(lexer);
    cqlParser parser = new cqlParser(tokens);
    parser.setBuildParseTree(true);
    return parser.library();
  }
}
