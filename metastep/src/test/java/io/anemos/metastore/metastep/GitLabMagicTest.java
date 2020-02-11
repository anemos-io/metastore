package io.anemos.metastore.metastep;

import java.io.ByteArrayInputStream;
import java.util.Arrays;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class GitLabMagicTest {

  String TEST_DATA =
      ": | eval $'export FF_CMD_DISABLE_DELAYED_ERROR_LEVEL_EXPANSION=$\\'false\\'"
          + "\\nexport FF_USE_LEGACY_BUILDS_DIR_FOR_DOCKER=$\\'false\\'\\nexport FF_USE_LEGACY_VOLUMES_MOUNTING_ORDER=$\\'false\\'"
          + "\\nexport CI_RUNNER_SHORT_TOKEN=$\\'X7jjQZ1J\\'\\nexport DOCKER_TLS_CERTDIR=\\'\\'"
          + "\\nexport DOCKER_DRIVER=$\\'overlay2\\'\\nexport CI_BUILDS_DIR=$\\'/builds\\'\\nexport CI_PROJECT_DIR=$\\'/builds/frisbee/contracts/data/staging\\'"
          + "\\nexport CI_CONCURRENT_ID=0\\nexport CI_PIPELINE_ID=376745\\nexport CI_PIPELINE_URL=$\\'https://git.mytech.eu/frisbee/contracts/data/staging/pipelines/376745\\'\\nexport CI_JOB_ID=1619725"
          + "\\nexport CI_JOB_URL=$\\'https://git.mytech.eu/frisbee/contracts/data/staging/-/jobs/1619725\\'\\nexport CI_JOB_TOKEN=$\\'[MASKED]\\'"
          + "\\nexport CI_BUILD_ID=1619725\\nexport CI_BUILD_TOKEN=$\\'[MASKED]\\'\\nexport CI_REGISTRY_USER=$\\'gitlab-ci-token\\'\\nexport CI_REGISTRY_PASSWORD=$\\'[MASKED]\\'"
          + "\\nexport CI_REPOSITORY_URL=$\\'https://gitlab-ci-token:[MASKED]@git.mytech.eu/frisbee/contracts/data/staging.git\\'\\nexport CI_JOB_NAME=$\\'test_contracts\\'"
          + "\\nexport CI_JOB_STAGE=$\\'test\\'\\nexport CI_NODE_TOTAL=1\\nexport CI_BUILD_NAME=$\\'test_contracts\\'\\nexport CI_BUILD_STAGE=$\\'test\\'"
          + "\\nexport CI=$\\'true\\'\\nexport GITLAB_CI=$\\'true\\'\\nexport CI_SERVER_URL=$\\'https://git.mytech.eu\\'\\nexport CI_SERVER_HOST=$\\'git.mytech.eu\\'"
          + "\\nexport CI_SERVER_NAME=$\\'GitLab\\'\\nexport CI_SERVER_VERSION=$\\'12.7.5-ee\\'\\nexport CI_SERVER_VERSION_MAJOR=12\\nexport CI_SERVER_VERSION_MINOR=7\\nexport CI_SERVER_VERSION_PATCH=5"
          + "\\nexport CI_SERVER_REVISION=$\\'19edff260da\\'\\nexport GITLAB_FEATURES=$\\'audit_events,burndown_charts,code_owners,code_review_analytics,contribution_analytics,description_diffs,elastic_search,export_issues,group_bulk_edit,group_burndown_charts,group_webhooks,issuable_default_templates,issue_board_focus_mode,issue_weights,jenkins_integration,ldap_group_sync,member_lock,merge_request_approvers,multiple_issue_assignees,multiple_ldap_servers,multiple_merge_request_assignees,protected_refs_for_users,push_rules,related_issues,repository_mirrors,repository_size_limit,scoped_issue_board,usage_quotas,visual_review_app,wip_limits\\'"
          + "\\nexport CI_PROJECT_ID=4071\\nexport CI_PROJECT_NAME=$\\'staging\\'\\nexport CI_PROJECT_TITLE=$\\'staging\\'\\nexport CI_PROJECT_PATH=$\\'frisbee/contracts/data/staging\\'"
          + "\\nexport CI_PROJECT_PATH_SLUG=$\\'frisbee-contracts-data-staging\\'\\nexport CI_PROJECT_NAMESPACE=$\\'frisbee/contracts/data\\'\\nexport CI_PROJECT_URL=$\\'https://git.mytech.eu/frisbee/contracts/data/staging\\'"
          + "\\nexport CI_PROJECT_VISIBILITY=$\\'internal\\'\\nexport CI_PROJECT_REPOSITORY_LANGUAGES=\\'\\'\\nexport CI_DEFAULT_BRANCH=$\\'master\\'\\nexport CI_PAGES_DOMAIN=$\\'pages.mytech.eu\\'"
          + "\\nexport CI_PAGES_URL=$\\'https://frisbee.pages.mytech.eu/contracts/data/staging\\'\\nexport CI_API_V4_URL=$\\'https://git.mytech.eu/api/v4\\'\\nexport CI_PIPELINE_IID=44\\nexport CI_PIPELINE_SOURCE=$\\'push\\'"
          + "\\nexport CI_CONFIG_PATH=$\\'.gitlab-ci.yml\\'\\nexport CI_COMMIT_SHA=$\\'34def962f1891d1dbe88bac997531fbe1793d4e5\\'\\nexport CI_COMMIT_SHORT_SHA=$\\'34def962\\'\\nexport CI_COMMIT_BEFORE_SHA=$\\'2ae74b4b1a4dc84c2610b2c4bc3a2889144aa810\\'"
          + "\\nexport CI_COMMIT_REF_NAME=$\\'master\\'\\nexport CI_COMMIT_REF_SLUG=$\\'master\\'\\nexport CI_COMMIT_BRANCH=$\\'master\\'\\nexport CI_COMMIT_MESSAGE=$\\'debug gitlab\\\\n\\'\\nexport CI_COMMIT_TITLE=$\\'debug gitlab\\'"
          + "\\nexport CI_COMMIT_DESCRIPTION=\\'\\'\\nexport CI_COMMIT_REF_PROTECTED=$\\'true\\'\\nexport CI_BUILD_REF=$\\'34def962f1891d1dbe88bac997531fbe1793d4e5\\'\\nexport CI_BUILD_BEFORE_SHA=$\\'2ae74b4b1a4dc84c2610b2c4bc3a2889144aa810\\'"
          + "\\nexport CI_BUILD_REF_NAME=$\\'master\\'\\nexport CI_BUILD_REF_SLUG=$\\'master\\'\\nexport CI_RUNNER_ID=368\\nexport CI_RUNNER_DESCRIPTION=$\\'vp-0c9f\\'\\nexport CI_RUNNER_TAGS=\\'\\'\\nexport METASTORE_HOST=$\\'project.test.data.mytech.eu:443\\'"
          + "\\nexport CERT_ENV=CERT_DATA_TEST\\nexport GITLAB_USER_ID=33\\nexport GITLAB_USER_EMAIL=$\\'alex.vanboxel@vente-exclusive.com\\'\\nexport GITLAB_USER_LOGIN=$\\'alexvb\\'\\nexport GITLAB_USER_NAME=$\\'Alex Van Boxel\\'"
          + "\\nexport CI_DISPOSABLE_ENVIRONMENT=$\\'true\\'\\nexport CI_RUNNER_VERSION=12.7.1\\nexport CI_RUNNER_REVISION=$"
          + "\\'003fe500\\'\\nexport CI_RUNNER_EXECUTABLE_ARCH=$\\'linux/amd64\\'"
          + "\\n$\\'cd\\' \"/builds/frisbee/contracts/data/staging\"\\necho $\\'\\\\x1b[32;1m$ validate --registry default --comment \"Forced Second Publish\" --package_prefix mytech --server $METASTORE_HOST --tls_env $CERT_ENV --source true\\\\x1b[0;m\\'\\nvalidate --registry default --comment \"Forced Second Publish\" --package_prefix mytech --server $METASTORE_HOST --single \"one\" --tls_env $CERT_ENV --source true\\n'";

  @Test
  public void test() {
    GitLabMagic gitLabMagic = new GitLabMagic(new ByteArrayInputStream(TEST_DATA.getBytes()));
    Assert.assertEquals("/builds/frisbee/contracts/data/staging", gitLabMagic.workDir);
    Assert.assertEquals("validate", gitLabMagic.gitLabArgs[0]);
    Assert.assertEquals("Forced Second Publish", gitLabMagic.gitLabArgs[4]);
    Assert.assertEquals("--package_prefix", gitLabMagic.gitLabArgs[5]);
    Assert.assertEquals("project.test.data.mytech.eu:443", gitLabMagic.gitLabArgs[8]);
    Assert.assertEquals("--single", gitLabMagic.gitLabArgs[9]);
    Assert.assertEquals("one", gitLabMagic.gitLabArgs[10]);
    System.out.println(Arrays.asList(gitLabMagic.gitLabArgs));
  }
}
