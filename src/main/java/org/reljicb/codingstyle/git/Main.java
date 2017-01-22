package org.reljicb.codingstyle.git;

import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.internal.storage.file.FileRepository;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.DepthWalk;
import org.eclipse.jgit.revwalk.RevCommit;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class Main
{
    private static final String BB_BACKBONE = "/Users/bojanreljic/development/workspace/bb-backbone";
    private static final String TEST = "/Users/bojanreljic/tmp/git-test/.git";

    private static final String REPO_PATH = BB_BACKBONE;

    final Git git;
    final Repository repo;

    public static void main(String[] args) throws IOException, GitAPIException
    {
        Main app = new Main();

        List<RevCommit> commits = app.walkBranch("master");
        commits.stream()
              .forEach(c -> System.out.println(String.format("%s %s", c.getName(), c.getShortMessage().trim())));

        for (RevCommit commit : commits)
        {
            Ref ref = app.checkoutCommit(commit);
            if (ref != null)
                System.out.println(String.format("ref name: ", ref.getName()));
        }
    }

    public Main() throws IOException
    {
        repo = new FileRepository(REPO_PATH + File.separator + ".git");

        git = new Git(repo);
    }

    private Ref checkoutCommit(RevCommit commit) throws GitAPIException
    {
        return git.checkout().setName(commit.getName()).call();
    }

    private List<RevCommit> walkBranch(final String branchName) throws IOException, GitAPIException
    {
        DepthWalk.RevWalk walk = new DepthWalk.RevWalk(repo, Integer.MAX_VALUE);

        List<Ref> branches = git.branchList().call();
        System.out.println(String.format("List of all branches: %s",
              Joiner.on(", ").join(branches.stream()
                    .map(branche -> branche.getName())
                    .collect(Collectors.toList()))));

        final String BRANCH_MASTER = branches.stream()
              .filter(b -> b.getName().contains(branchName))
              .findFirst()
              .map(b -> b.getName())
              .get();

//        System.out.println(String.format("Commits of branch: %s:", BRANCH_MASTER));
//        System.out.println("-------------------------------------");

        Iterable<RevCommit> commits = git.log().all().call();

        List<RevCommit> ret = Lists.newArrayList();
        for (RevCommit commit : commits)
        {
            boolean foundInThisBranch = false;

            RevCommit targetCommit = walk.parseCommit(repo.resolve(
                  commit.getName()));
            for (Map.Entry<String, Ref> e : repo.getAllRefs().entrySet())
            {
                if (e.getKey().startsWith(Constants.R_HEADS))
                {
                    if (walk.isMergedInto(targetCommit, walk.parseCommit(
                          e.getValue().getObjectId())))
                    {
                        String foundInBranch = e.getValue().getName();
                        if (BRANCH_MASTER.equals(foundInBranch))
                        {
                            foundInThisBranch = true;
                            break;
                        }
                    }
                }
            }

            if (foundInThisBranch)
            {
                ret.add(commit);
//                    System.out.println(commit.getName());
//                    System.out.println(commit.getAuthorIdent().getName());
//                    System.out.println(new Date(commit.getCommitTime()));
//                System.out.println(String.format("%s %s", commit.getName(), commit.getShortMessage().trim()));
            }
        }

        return ret;
    }
}
