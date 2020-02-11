package io.anemos.metastore.metastep;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class GitLabMagic {

  boolean gitLabEmptyCall = false;
  String[] gitLabArgs = null;
  String workDir = null;
  HashMap<String, String> export = new HashMap<>();

  GitLabMagic(InputStream in) {
    System.out.println("GitLab detected, starting magic mode (tested on GitLab 11.5).");
    Scanner scanner = new Scanner(in);
    while (scanner.hasNext()) {
      String line = scanner.nextLine();

      if (line.startsWith(": | eval")) {
        Pattern cdPattern = Pattern.compile(".*\\\\'cd\\\\' \\\"(.*)\\\"");
        Pattern exportPattern = Pattern.compile("export (.*)=(.*)");

        String[] tokens = line.split("\\\\n");
        for (String token : tokens) {
          Matcher cdMatcher = cdPattern.matcher(token);
          Matcher exportMatcher = exportPattern.matcher(token);
          if (cdMatcher.matches()) {
            workDir = cdMatcher.group(1);
          } else if (exportMatcher.matches()) {
            String value = exportMatcher.group(2);
            if (value.startsWith("$")) {
              value = value.substring(3, value.length() - 2);
            }
            export.put(exportMatcher.group(1), value);
          }
        }
        if (tokens.length < 2) {
          gitLabEmptyCall = true;
        } else {
          List<String> args = new ArrayList<>();
          StringBuilder builder = new StringBuilder();
          for (String token : tokens[tokens.length - 2].split(" ")) {
            if(builder.length() > 0) {
              builder.append(" ");
              if(token.endsWith("\"")) {
                builder.append(token, 0, token.length()-1);
                args.add(builder.toString());
                builder = new StringBuilder();
              }
              else {
                builder.append(token);
              }
            }
            else {
            if (token.startsWith("$")) {
              args.add(export.get(token.substring(1)));
            } else if(token.startsWith("\"")) {
              if(token.endsWith("\"")) {
                args.add(token.substring(1,token.length()-1));
              }
              else {
                builder.append(token.substring(1));
              }
            } else {
              args.add(token);
            }}
          }
          gitLabArgs = args.toArray(new String[0]);
        }
      }
    }

    scanner.close();
  }
}
