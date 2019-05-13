import io.anemos.metastore.metastep.MetaStep;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class MetaStepTest {

  // TODO used for manual testing, can be removed?

  @Test
  @Ignore
  public void validateDefaultTest() throws Exception {
    String[] args =
        new String[] {
          "validate",
          "--package_prefix",
          "test",
          "--server",
          "127.0.0.1:8980",
          "--registry",
          "default",
          "--workspace",
          "/tmp/default"
        };
    MetaStep metaStep = new MetaStep(args);
    metaStep.start();
  }

  @Test
  @Ignore
  public void submitDefaultTest() throws Exception {
    String[] args =
        new String[] {
          "publish",
          "--package_prefix",
          "vptech",
          "--server",
          "127.0.0.1:8980",
          "--registry",
          "default",
          "--workspace",
          "/tmp/default"
        };
    MetaStep metaStep = new MetaStep(args);
    metaStep.start();
  }

  @Test
  @Ignore
  public void validateShadowTest() throws Exception {
    String[] args =
        new String[] {
          "validate",
          "--package_prefix",
          "test",
          "--server",
          "127.0.0.1:8980",
          "--registry",
          "shadow",
          "--workspace",
          "/tmp/shadow-contracts"
        };
    MetaStep metaStep = new MetaStep(args);
    metaStep.start();
  }

  @Test
  @Ignore
  public void publishShadowTest() throws Exception {
    String[] args =
        new String[] {
          "publish",
          "--package_prefix",
          "test",
          "--server",
          "127.0.0.1:8980",
          "--registry",
          "shadow",
          "--workspace",
          "/tmp/shadow-contracts"
        };
    MetaStep metaStep = new MetaStep(args);
    metaStep.start();
  }
}
