package io.anemos.metastore;

import java.io.File;
import java.io.IOException;
import org.eclipse.jgit.internal.storage.file.FileRepository;

public class MonoRegistry {

  public MonoRegistry() throws IOException {

    File file = new File("tmp/_registry");
    FileRepository repo = new FileRepository(file);
    if (!file.exists()) {
      repo.create(true);
    }
    //        file.mkdirs();

    // Git git = new Git(repo);

    //        ObjectId objectIdOfTree = repo.resolve("HEAD");
    //        RevWalk walk = new RevWalk(repo);
    //        RevTree tree = walk.parseTree(objectIdOfTree);
    //        System.out.println(tree);
  }
}
