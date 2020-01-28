package io.anemos.metastore.core.proto.validation;

import com.google.protobuf.ByteString;
import io.anemos.metastore.core.proto.TestSets;
import io.anemos.metastore.core.proto.validate.ProtoDiff;
import io.anemos.metastore.core.proto.validate.ValidationResults;
import io.anemos.metastore.putils.ProtoDomain;
import io.anemos.metastore.v1alpha1.ChangeType;
import io.anemos.metastore.v1alpha1.FileResult;
import io.anemos.metastore.v1alpha1.MessageResult;
import io.anemos.metastore.v1alpha1.OptionChangeInfo;
import io.anemos.metastore.v1alpha1.Patch;
import java.io.IOException;
import org.junit.Assert;
import org.junit.Test;
import test.v1.Option;

public class OptionDiffTest {

  @Test
  public void addMessageOptionTest() throws IOException {
    ProtoDomain baseAddMessageOption = TestSets.baseAddMessageOption();
    ProtoDomain base = TestSets.baseKnownOption();

    MessageResult messageResult = diffMessage(base, baseAddMessageOption);
    OptionChangeInfo optionChangeInfo = messageResult.getOptionChangeList().get(0);

    Assert.assertEquals(ChangeType.ADDITION, optionChangeInfo.getChangeType());
    Assert.assertEquals(OptionChangeInfo.OptionType.MESSAGE_OPTION, optionChangeInfo.getType());

    ByteString payload = optionChangeInfo.getPayloadNew();
    Option.TestOption option = Option.TestOption.parseFrom(payload);
  }

  @Test
  public void removeMessageOptionTest() throws IOException {
    ProtoDomain base = TestSets.baseAddMessageOption();
    ProtoDomain baseRemoveFieldOption = TestSets.baseKnownOption();

    MessageResult messageResult = diffMessage(base, baseRemoveFieldOption);
    OptionChangeInfo optionChangeInfo = messageResult.getOptionChangeList().get(0);

    Assert.assertEquals(ChangeType.REMOVAL, optionChangeInfo.getChangeType());
    Assert.assertEquals(OptionChangeInfo.OptionType.MESSAGE_OPTION, optionChangeInfo.getType());

    ByteString payload = optionChangeInfo.getPayloadOld();
    Option.TestOption option = Option.TestOption.parseFrom(payload);
  }

  @Test
  public void changeMessageOptionTest() throws IOException {
    ProtoDomain baseAddMessageOption = TestSets.baseAddMessageOption();
    ProtoDomain baseChangeMessageOption = TestSets.baseChangeMessageOption();

    MessageResult messageResult = diffMessage(baseAddMessageOption, baseChangeMessageOption);
    OptionChangeInfo optionChangeInfo = messageResult.getOptionChangeList().get(0);

    Assert.assertEquals(ChangeType.PAYLOAD_CHANGED, optionChangeInfo.getChangeType());
    Assert.assertEquals(OptionChangeInfo.OptionType.MESSAGE_OPTION, optionChangeInfo.getType());

    ByteString payloadOld = optionChangeInfo.getPayloadOld();
    Option.TestOption optionOld = Option.TestOption.parseFrom(payloadOld);
    ByteString payloadNew = optionChangeInfo.getPayloadNew();
    Option.TestOption optionNew = Option.TestOption.parseFrom(payloadNew);
    Assert.assertNotEquals(optionOld, optionNew);
  }

  @Test
  public void addFieldOptionTest() throws IOException {
    ProtoDomain baseAddFieldOption = TestSets.baseAddFieldOption();
    ProtoDomain base = TestSets.baseKnownOption();

    MessageResult messageResult = diffMessage(base, baseAddFieldOption);
    OptionChangeInfo optionChangeInfo =
        messageResult.getFieldResults(0).getOptionChangeList().get(0);

    Assert.assertEquals(ChangeType.ADDITION, optionChangeInfo.getChangeType());
    Assert.assertEquals(OptionChangeInfo.OptionType.FIELD_OPTION, optionChangeInfo.getType());

    ByteString payload = optionChangeInfo.getPayloadNew();
    Option.TestOption option = Option.TestOption.parseFrom(payload);
  }

  @Test
  public void removeFieldOptionTest() throws IOException {
    ProtoDomain base = TestSets.baseAddFieldOption();
    ProtoDomain baseRemoveFieldOption = TestSets.baseKnownOption();

    MessageResult messageResult = diffMessage(base, baseRemoveFieldOption);
    OptionChangeInfo optionChangeInfo =
        messageResult.getFieldResults(0).getOptionChangeList().get(0);

    Assert.assertEquals(ChangeType.REMOVAL, optionChangeInfo.getChangeType());
    Assert.assertEquals(OptionChangeInfo.OptionType.FIELD_OPTION, optionChangeInfo.getType());

    ByteString payload = optionChangeInfo.getPayloadNew();
    Option.TestOption option = Option.TestOption.parseFrom(payload);
  }

  @Test
  public void changeFieldOptionTest() throws IOException {
    ProtoDomain base = TestSets.baseAddFieldOption();
    ProtoDomain baseChangeFieldOption = TestSets.baseChangeFieldOption();

    MessageResult messageResult = diffMessage(base, baseChangeFieldOption);
    OptionChangeInfo optionChangeInfo =
        messageResult.getFieldResults(0).getOptionChangeList().get(0);

    Assert.assertEquals(ChangeType.PAYLOAD_CHANGED, optionChangeInfo.getChangeType());
    Assert.assertEquals(OptionChangeInfo.OptionType.FIELD_OPTION, optionChangeInfo.getType());

    ByteString payloadOld = optionChangeInfo.getPayloadOld();
    Option.TestOption optionOld = Option.TestOption.parseFrom(payloadOld);
    ByteString payloadNew = optionChangeInfo.getPayloadNew();
    Option.TestOption optionNew = Option.TestOption.parseFrom(payloadNew);
    Assert.assertNotEquals(optionOld, optionNew);
  }

  @Test
  public void addFileOptionTest() throws IOException {
    ProtoDomain baseAddFileOption = TestSets.baseAddFileOption();
    ProtoDomain base = TestSets.baseKnownOption();

    FileResult fileResult = diffFile(base, baseAddFileOption);
    OptionChangeInfo optionChangeInfo = fileResult.getOptionChangeList().get(0);

    Assert.assertEquals(ChangeType.ADDITION, optionChangeInfo.getChangeType());
    Assert.assertEquals(OptionChangeInfo.OptionType.FILE_OPTION, optionChangeInfo.getType());

    ByteString payload = optionChangeInfo.getPayloadNew();
    Option.TestOption option = Option.TestOption.parseFrom(payload);
  }

  @Test
  public void removeFileOptionTest() throws IOException {
    ProtoDomain baseRemoveFileOption = TestSets.baseKnownOption();
    ProtoDomain base = TestSets.baseAddFileOption();

    FileResult fileResult = diffFile(base, baseRemoveFileOption);
    OptionChangeInfo optionChangeInfo = fileResult.getOptionChangeList().get(0);

    Assert.assertEquals(ChangeType.REMOVAL, optionChangeInfo.getChangeType());
    Assert.assertEquals(OptionChangeInfo.OptionType.FILE_OPTION, optionChangeInfo.getType());

    ByteString payload = optionChangeInfo.getPayloadNew();
    Option.TestOption option = Option.TestOption.parseFrom(payload);
  }

  @Test
  public void changeFileOptionTest() throws IOException {
    ProtoDomain baseChangeFileOption = TestSets.baseChangeFileOption();
    ProtoDomain base = TestSets.baseAddFileOption();

    FileResult fileResult = diffFile(base, baseChangeFileOption);
    OptionChangeInfo optionChangeInfo = fileResult.getOptionChangeList().get(0);

    Assert.assertEquals(ChangeType.PAYLOAD_CHANGED, optionChangeInfo.getChangeType());
    Assert.assertEquals(OptionChangeInfo.OptionType.FILE_OPTION, optionChangeInfo.getType());

    ByteString payload = optionChangeInfo.getPayloadNew();
    Option.TestOption option = Option.TestOption.parseFrom(payload);
  }

  private MessageResult diffMessage(ProtoDomain dRef, ProtoDomain dNew) throws IOException {
    ValidationResults results = new ValidationResults();
    ProtoDiff diff = new ProtoDiff(dRef, dNew, results);
    diff.diffOnMessage("test.v1.ProtoBeamBasicMessage");

    Patch result = results.getPatch();
    return result.getMessageResultsMap().get("test.v1.ProtoBeamBasicMessage");
  }

  private FileResult diffFile(ProtoDomain dRef, ProtoDomain dNew) throws IOException {
    ValidationResults results = new ValidationResults();
    ProtoDiff diff = new ProtoDiff(dRef, dNew, results);
    diff.diffOnFileName("test/v1/simple.proto");

    Patch result = results.getPatch();
    return result.getFileResultsMap().get("test/v1/simple.proto");
  }
}
