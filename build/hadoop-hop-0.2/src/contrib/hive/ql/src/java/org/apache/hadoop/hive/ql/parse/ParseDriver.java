/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.hadoop.hive.ql.parse;

import java.util.ArrayList;
import java.util.HashMap;

import org.antlr.runtime.*;
import org.antlr.runtime.tree.*;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class ParseDriver {
    
  static final private Log LOG = LogFactory.getLog("hive.ql.parse.ParseDriver");

  private static HashMap<String, String> xlateMap;
  static {
    xlateMap = new HashMap<String, String>();

    // Keywords
    xlateMap.put("KW_TRUE", "TRUE");
    xlateMap.put("KW_FALSE", "FALSE");
    xlateMap.put("KW_ALL", "ALL");
    xlateMap.put("KW_AND", "AND");
    xlateMap.put("KW_OR", "OR");
    xlateMap.put("KW_NOT", "NOT");
    xlateMap.put("KW_LIKE", "LIKE");

    xlateMap.put("KW_ASC", "ASC");
    xlateMap.put("KW_DESC", "DESC");
    xlateMap.put("KW_ORDER", "ORDER");
    xlateMap.put("KW_BY", "BY");
    xlateMap.put("KW_GROUP", "GROUP");
    xlateMap.put("KW_WHERE", "WHERE");
    xlateMap.put("KW_FROM", "FROM");
    xlateMap.put("KW_AS", "AS");
    xlateMap.put("KW_SELECT", "SELECT");
    xlateMap.put("KW_DISTINCT", "DISTINCT");
    xlateMap.put("KW_INSERT", "INSERT");
    xlateMap.put("KW_OVERWRITE", "OVERWRITE");
    xlateMap.put("KW_OUTER", "OUTER");
    xlateMap.put("KW_JOIN", "JOIN");
    xlateMap.put("KW_LEFT", "LEFT");
    xlateMap.put("KW_RIGHT", "RIGHT");
    xlateMap.put("KW_FULL", "FULL");
    xlateMap.put("KW_ON", "ON");
    xlateMap.put("KW_PARTITION", "PARTITION");
    xlateMap.put("KW_TABLE", "TABLE");
    xlateMap.put("KW_DIRECTORY", "DIRECTORY");
    xlateMap.put("KW_LOCAL", "LOCAL");
    xlateMap.put("KW_TRANSFORM", "TRANSFORM");
    xlateMap.put("KW_USING", "USING");
    xlateMap.put("KW_CLUSTER", "CLUSTER");
    xlateMap.put("KW_UNION", "UNION");
    
    // Operators
    xlateMap.put("DOT", ".");
    xlateMap.put("COLON", ":");
    xlateMap.put("COMMA", ",");
    xlateMap.put("SEMICOLON", ");");
    
    xlateMap.put("LPAREN", "(");
    xlateMap.put("RPAREN", ")");
    xlateMap.put("LSQUARE", "[");
    xlateMap.put("RSQUARE", "]");
    
    xlateMap.put("EQUAL", "=");
    xlateMap.put("NOTEQUAL", "<>");
    xlateMap.put("LESSTHANOREQUALTO", "<=");
    xlateMap.put("LESSTHAN", "<");
    xlateMap.put("GREATERTHANOREQUALTO", ">=");
    xlateMap.put("GREATERTHAN", ">");
    
    xlateMap.put("DIVIDE", "/");
    xlateMap.put("PLUS", "+");
    xlateMap.put("MINUS", "-");
    xlateMap.put("STAR", "*");
    xlateMap.put("MOD", "%");

    xlateMap.put("AMPERSAND", "&");
    xlateMap.put("TILDE", "~");
    xlateMap.put("BITWISEOR", "|");
    xlateMap.put("BITWISEXOR", "^");
  }

  private static String xlate(String name) {
    
    String ret = xlateMap.get(name);
    if (ret == null) {
      ret = name;
    }
    
    return ret;
  }

  // This class provides and implementation for a case insensitive token checker for
  // the lexical analysis part of antlr. By converting the token stream into upper case
  // at the time when lexical rules are checked, this class ensures that the lexical rules
  // need to just match the token with upper case letters as opposed to combination of upper
  // case and lower case characteres. This is purely used for matching lexical rules. The 
  // actual token text is stored in the same way as the user input without actually converting
  // it into an upper case. The token values are generated by the consume() function of the
  // super class ANTLRStringStream. The LA() function is the lookahead funtion and is purely
  // used for matching lexical rules. This also means that the grammar will only accept
  // capitalized tokens in case it is run from other tools like antlrworks which do not
  // have the ANTLRNoCaseStringStream implementation.
  public class ANTLRNoCaseStringStream  extends ANTLRStringStream {

    public ANTLRNoCaseStringStream(String input) {
      super(input);
    }
    
    public int LA(int i) {

      int returnChar = super.LA(i);
      if(returnChar == CharStream.EOF) {
        return returnChar; 
      }
      else if(returnChar == 0) {
        return returnChar;
      }
    
      return Character.toUpperCase((char)returnChar);
    }
  }

  public class HiveLexerX extends HiveLexer {

    private ArrayList<ParseError> errors;

    public HiveLexerX() {
      super();
      errors = new ArrayList<ParseError>();
    }

    public HiveLexerX(CharStream input) {
      super(input);
      errors = new ArrayList<ParseError>();
    }

    public void displayRecognitionError(String[] tokenNames,
                                        RecognitionException e) {

      errors.add(new ParseError(this, e, tokenNames));
    }

    public String getErrorMessage(RecognitionException e, String[] tokenNames) {
      String msg = null;

      if (e instanceof NoViableAltException) {
        @SuppressWarnings("unused")
    NoViableAltException nvae = (NoViableAltException)e;
        // for development, can add "decision=<<"+nvae.grammarDecisionDescription+">>"
        // and "(decision="+nvae.decisionNumber+") and
        // "state "+nvae.stateNumber
        msg = "character " + getCharErrorDisplay(e.c) + " not supported here";
      }
      else {
        msg = super.getErrorMessage(e, tokenNames);
      }

      return msg;
    }

    public ArrayList<ParseError> getErrors() {
      return errors;
    }

  }

  public class HiveParserX extends HiveParser {

    private ArrayList<ParseError> errors;

    public HiveParserX(TokenStream input) {
      super(input);
      errors = new ArrayList<ParseError>();
    }

    protected void mismatch(IntStream input, int ttype, BitSet follow) 
      throws RecognitionException {

      throw new MismatchedTokenException(ttype, input);
    }

    public void recoverFromMismatchedSet(IntStream input,
                                         RecognitionException re,
                                         BitSet follow)
      throws RecognitionException
    {
      throw re;
    }

    public void displayRecognitionError(String[] tokenNames,
                                        RecognitionException e) {

      errors.add(new ParseError(this, e, tokenNames));
    }

    public String getErrorMessage(RecognitionException e, String[] tokenNames) {
      String msg = null;

      // Transalate the token names to something that the user can understand
      String[] xlateNames = new String[tokenNames.length];
      for(int i=0; i<tokenNames.length; ++i) {
        xlateNames[i] = ParseDriver.xlate(tokenNames[i]);
      }

      if (e instanceof NoViableAltException) {
        @SuppressWarnings("unused")
    NoViableAltException nvae = (NoViableAltException)e;
        // for development, can add "decision=<<"+nvae.grammarDecisionDescription+">>"
        // and "(decision="+nvae.decisionNumber+") and
        // "state "+nvae.stateNumber
        msg = "cannot recognize input " + getTokenErrorDisplay(e.token);
      }
      else {
        msg = super.getErrorMessage(e, xlateNames);
      }

      return msg;
    }

    public ArrayList<ParseError> getErrors() {
      return errors;
    }

  }

  public CommonTree parse(String command) throws ParseException {
    LOG.info("Parsing command: " + command);
      
    HiveLexerX lexer = new HiveLexerX(new ANTLRNoCaseStringStream(command));
    TokenStream tokens = new TokenRewriteStream(lexer);
    HiveParserX parser = new HiveParserX(tokens);
    HiveParser.statement_return r = null;
    try {
      r = parser.statement();
    } catch (RecognitionException e) {
      throw new ParseException(parser.getErrors());
    }

    if (lexer.getErrors().size() == 0 &&
        parser.getErrors().size() == 0) {
      LOG.info("Parse Completed");
    }
    else if (lexer.getErrors().size() != 0) {
      throw new ParseException(lexer.getErrors());
    } else {
      throw new ParseException(parser.getErrors());
    }
      
    return (CommonTree)r.getTree();
  }
}

