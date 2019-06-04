package io.anemos.metastore.core.proto.validation;

import com.google.protobuf.ByteString;
import io.anemos.metastore.core.proto.PContainer;
import io.anemos.metastore.core.proto.TestSets;
import io.anemos.metastore.core.proto.validate.ProtoDiff;
import io.anemos.metastore.core.proto.validate.ValidationResults;
import io.anemos.metastore.core.test.TestOption;
import io.anemos.metastore.v1alpha1.FileResult;
import io.anemos.metastore.v1alpha1.MessageResult;
import io.anemos.metastore.v1alpha1.OptionChangeInfo;
import io.anemos.metastore.v1alpha1.Report;
import java.io.IOException;
import org.junit.Assert;
import org.junit.Test;

public class OptionDiffTest {

  @Test
  public void addMessageOptionTest() throws IOException {
    PContainer baseAddMessageOption = TestSets.baseAddMessageOption();
    PContainer base = TestSets.baseKnownOption();

    MessageResult messageResult = diffMessage(base, baseAddMessageOption);
    OptionChangeInfo optionChangeInfo = messageResult.getOptionChangeList().get(0);

    Assert.assertEquals(
        OptionChangeInfo.OptionChangeType.OPTION_ADDED, optionChangeInfo.getChangeType());
    Assert.assertEquals(OptionChangeInfo.OptionType.MESSAGE_OPTION, optionChangeInfo.getType());

    ByteString payload = optionChangeInfo.getPayloadNew();
    TestOption option = TestOption.parseFrom(payload);
  }

  @Test
  public void removeMessageOptionTest() throws IOException {
    PContainer base = TestSets.baseAddMessageOption();
    PContainer baseRemoveFieldOption = TestSets.baseKnownOption();

    MessageResult messageResult = diffMessage(base, baseRemoveFieldOption);
    OptionChangeInfo optionChangeInfo = messageResult.getOptionChangeList().get(0);

    Assert.assertEquals(
        OptionChangeInfo.OptionChangeType.OPTION_REMOVED, optionChangeInfo.getChangeType());
    Assert.assertEquals(OptionChangeInfo.OptionType.MESSAGE_OPTION, optionChangeInfo.getType());

    ByteString payload = optionChangeInfo.getPayloadOld();
    TestOption option = TestOption.parseFrom(payload);
  }

  @Test
  public void changeMessageOptionTest() throws IOException {
    PContainer baseAddMessageOption = TestSets.baseAddMessageOption();
    PContainer baseChangeMessageOption = TestSets.baseChangeMessageOption();

    MessageResult messageResult = diffMessage(baseAddMessageOption, baseChangeMessageOption);
    OptionChangeInfo optionChangeInfo = messageResult.getOptionChangeList().get(0);

    Assert.assertEquals(
        OptionChangeInfo.OptionChangeType.OPTION_PAYLOAD_CHANGED, optionChangeInfo.getChangeType());
    Assert.assertEquals(OptionChangeInfo.OptionType.MESSAGE_OPTION, optionChangeInfo.getType());

    ByteString payloadOld = optionChangeInfo.getPayloadOld();
    TestOption optionOld = TestOption.parseFrom(payloadOld);
    ByteString payloadNew = optionChangeInfo.getPayloadNew();
    TestOption optionNew = TestOption.parseFrom(payloadNew);
    Assert.assertNotEquals(optionOld, optionNew);
  }

  @Test
  public void addFieldOptionTest() throws IOException {
    PContainer baseAddFieldOption = TestSets.baseAddFieldOption();
    PContainer base = TestSets.baseKnownOption();

    MessageResult messageResult = diffMessage(base, baseAddFieldOption);
    OptionChangeInfo optionChangeInfo =
        messageResult.getFieldResults(0).getOptionChangeList().get(0);

    Assert.assertEquals(
        OptionChangeInfo.OptionChangeType.OPTION_ADDED, optionChangeInfo.getChangeType());
    Assert.assertEquals(OptionChangeInfo.OptionType.FIELD_OPTION, optionChangeInfo.getType());

    ByteString payload = optionChangeInfo.getPayloadNew();
    TestOption option = TestOption.parseFrom(payload);
  }

  @Test
  public void removeFieldOptionTest() throws IOException {
    PContainer base = TestSets.baseAddFieldOption();
    PContainer baseRemoveFieldOption = TestSets.baseKnownOption();

    MessageResult messageResult = diffMessage(base, baseRemoveFieldOption);
    OptionChangeInfo optionChangeInfo =
        messageResult.getFieldResults(0).getOptionChangeList().get(0);

    Assert.assertEquals(
        OptionChangeInfo.OptionChangeType.OPTION_REMOVED, optionChangeInfo.getChangeType());
    Assert.assertEquals(OptionChangeInfo.OptionType.FIELD_OPTION, optionChangeInfo.getType());

    ByteString payload = optionChangeInfo.getPayloadNew();
    TestOption option = TestOption.parseFrom(payload);
  }

  @Test
  public void changeFieldOptionTest() throws IOException {
    PContainer base = TestSets.baseAddFieldOption();
    PContainer baseChangeFieldOption = TestSets.baseChangeFieldOption();

    MessageResult messageResult = diffMessage(base, baseChangeFieldOption);
    OptionChangeInfo optionChangeInfo =
        messageResult.getFieldResults(0).getOptionChangeList().get(0);

    Assert.assertEquals(
        OptionChangeInfo.OptionChangeType.OPTION_PAYLOAD_CHANGED, optionChangeInfo.getChangeType());
    Assert.assertEquals(OptionChangeInfo.OptionType.FIELD_OPTION, optionChangeInfo.getType());

    ByteString payloadOld = optionChangeInfo.getPayloadOld();
    TestOption optionOld = TestOption.parseFrom(payloadOld);
    ByteString payloadNew = optionChangeInfo.getPayloadNew();
    TestOption optionNew = TestOption.parseFrom(payloadNew);
    Assert.assertNotEquals(optionOld, optionNew);
  }

  @Test
  public void addFileOptionTest() throws IOException {
    PContainer baseAddFileOption = TestSets.baseAddFileOption();
    PContainer base = TestSets.baseKnownOption();

    FileResult fileResult = diffFile(base, baseAddFileOption);
    OptionChangeInfo optionChangeInfo = fileResult.getOptionChangeList().get(0);

    Assert.assertEquals(
        OptionChangeInfo.OptionChangeType.OPTION_ADDED, optionChangeInfo.getChangeType());
    Assert.assertEquals(OptionChangeInfo.OptionType.FILE_OPTION, optionChangeInfo.getType());

    ByteString payload = optionChangeInfo.getPayloadNew();
    TestOption option = TestOption.parseFrom(payload);
  }

  @Test
  public void removeFileOptionTest() throws IOException {
    PContainer baseRemoveFileOption = TestSets.baseKnownOption();
    PContainer base = TestSets.baseAddFileOption();

    FileResult fileResult = diffFile(base, baseRemoveFileOption);
    OptionChangeInfo optionChangeInfo = fileResult.getOptionChangeList().get(0);

    Assert.assertEquals(
        OptionChangeInfo.OptionChangeType.OPTION_REMOVED, optionChangeInfo.getChangeType());
    Assert.assertEquals(OptionChangeInfo.OptionType.FILE_OPTION, optionChangeInfo.getType());

    ByteString payload = optionChangeInfo.getPayloadNew();
    TestOption option = TestOption.parseFrom(payload);
  }

  @Test
  public void changeFileOptionTest() throws IOException {
    PContainer baseChangeFileOption = TestSets.baseChangeFileOption();
    PContainer base = TestSets.baseAddFileOption();

    FileResult fileResult = diffFile(base, baseChangeFileOption);
    OptionChangeInfo optionChangeInfo = fileResult.getOptionChangeList().get(0);

    Assert.assertEquals(
        OptionChangeInfo.OptionChangeType.OPTION_PAYLOAD_CHANGED, optionChangeInfo.getChangeType());
    Assert.assertEquals(OptionChangeInfo.OptionType.FILE_OPTION, optionChangeInfo.getType());

    ByteString payload = optionChangeInfo.getPayloadNew();
    TestOption option = TestOption.parseFrom(payload);
  }

  private MessageResult diffMessage(PContainer dRef, PContainer dNew) throws IOException {
    ValidationResults results = new ValidationResults();
    ProtoDiff diff = new ProtoDiff(dRef, dNew, results);
    diff.diffOnMessage("test.v1.ProtoBeamBasicMessage");

    Report result = results.getReport();
    System.out.println(result);
    return result.getMessageResultsMap().get("test.v1.ProtoBeamBasicMessage");
  }

  private FileResult diffFile(PContainer dRef, PContainer dNew) throws IOException {
    ValidationResults results = new ValidationResults();
    ProtoDiff diff = new ProtoDiff(dRef, dNew, results);
    diff.diffOnFileName("test/v1/simple.proto");

    Report result = results.getReport();
    System.out.println(result);
    return result.getFileResultsMap().get("test/v1/simple.proto");
  }
}
