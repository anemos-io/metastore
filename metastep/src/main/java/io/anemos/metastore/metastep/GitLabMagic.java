package io.anemos.metastore.metastep;

import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GitLabMagic {

    boolean gitLabEmptyCall = false;
    String[] gitLabArgs = null;
    String workDir = null;

    public GitLabMagic() {
        System.out.println("GitLab detected, starting magic mode (tested on GitLab 11.5).");
        Scanner scanner = new Scanner(System.in);
        while (scanner.hasNext()) {
            String line = scanner.nextLine();


            if (line.startsWith(": | eval")) {
                Pattern pattern = Pattern.compile(".*\\\\'cd\\\\' \\\"(.*)\\\"");

                String[] tokens = line.split("\\\\n");
                for (String token : tokens) {
                    Matcher matcher = pattern.matcher(token);
                    if (matcher.matches()) {
                        workDir = matcher.group(1);
                    }
                }
                if (tokens.length < 2) {
                    gitLabEmptyCall = true;
                } else {
                    gitLabArgs = tokens[tokens.length - 2].split(" ");
                }
            }

        }


        scanner.close();


    }
}
