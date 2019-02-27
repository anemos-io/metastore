package io.anemos.metastore;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.internal.storage.file.FileRepository;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.revwalk.RevWalk;

import java.io.File;
import java.io.IOException;

public class MonoRegistry {

    public MonoRegistry() throws IOException {

        File file = new File("tmp/_registry");
        FileRepository repo = new FileRepository(file);
        if(!file.exists()) {
            repo.create(true);
        }
//        file.mkdirs();

        //Git git = new Git(repo);



//        ObjectId objectIdOfTree = repo.resolve("HEAD");
//        RevWalk walk = new RevWalk(repo);
//        RevTree tree = walk.parseTree(objectIdOfTree);
//        System.out.println(tree);
    }
}
