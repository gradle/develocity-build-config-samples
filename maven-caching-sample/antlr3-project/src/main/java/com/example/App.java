package com.example;

import com.example.antlr.TLexer;
import com.example.antlr.TParser;

import org.antlr.runtime.*;

public class App {
    public static boolean useAntlr3() {
      TLexer lexer = new TLexer(new ANTLRStringStream("This is Keiser Soze"));
      CommonTokenStream tokenStream = new CommonTokenStream(lexer);
      TParser parser = new TParser(tokenStream);
      return true;
    }
}
