package io.anemos.metastore.core.proto.validation;

import com.google.protobuf.ByteString;
import io.anemos.metastore.core.proto.TestSets;
import io.anemos.metastore.core.proto.validate.ProtoDiff;
import io.anemos.metastore.core.proto.validate.ValidationResults;
import io.anemos.metastore.putils.ProtoDomain;
import io.anemos.metastore.v1alpha1.ChangeType;
import io.anemos.metastore.v1alpha1.FilePatch;
import io.anemos.metastore.v1alpha1.MessagePatch;
import io.anemos.metastore.v1alpha1.OptionChange;
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

    MessagePatch messageResult = diffMessage(base, baseAddMessageOption);
    OptionChange optionChange = messageResult.getOptionChangeList().get(0);

    Assert.assertEquals(ChangeType.ADDITION, optionChange.getChangeType());
    Assert.assertEquals(OptionChange.OptionType.MESSAGE_OPTION, optionChange.getType());

    ByteString payload = optionChange.getPayloadNew();
    Option.TestOption option = Option.TestOption.parseFrom(payload);
  }

  @Test
  public void removeMessageOptionTest() throws IOException {
    ProtoDomain base = TestSets.baseAddMessageOption();
    ProtoDomain baseRemoveFieldOption = TestSets.baseKnownOption();

    MessagePatch messageResult = diffMessage(base, baseRemoveFieldOption);
    OptionChange optionChange = messageResult.getOptionChangeList().get(0);

    Assert.assertEquals(ChangeType.REMOVAL, optionChange.getChangeType());
    Assert.assertEquals(OptionChange.OptionType.MESSAGE_OPTION, optionChange.getType());

    ByteString payload = optionChange.getPayloadOld();
    Option.TestOption option = Option.TestOption.parseFrom(payload);
  }

  @Test
  public void changeMessageOptionTest() throws IOException {
    ProtoDomain baseAddMessageOption = TestSets.baseAddMessageOption();
    ProtoDomain baseChangeMessageOption = TestSets.baseChangeMessageOption();

    MessagePatch messageResult = diffMessage(baseAddMessageOption, baseChangeMessageOption);
    OptionChange optionChange = messageResult.getOptionChangeList().get(0);

    Assert.assertEquals(ChangeType.PAYLOAD_CHANGED, optionChange.getChangeType());
    Assert.assertEquals(OptionChange.OptionType.MESSAGE_OPTION, optionChange.getType());

    ByteString payloadOld = optionChange.getPayloadOld();
    Option.TestOption optionOld = Option.TestOption.parseFrom(payloadOld);
    ByteString payloadNew = optionChange.getPayloadNew();
    Option.TestOption optionNew = Option.TestOption.parseFrom(payloadNew);
    Assert.assertNotEquals(optionOld, optionNew);
  }

  @Test
  public void addFieldOptionTest() throws IOException {
    ProtoDomain baseAddFieldOption = TestSets.baseAddFieldOption();
    ProtoDomain base = TestSets.baseKnownOption();

    MessagePatch messageResult = diffMessage(base, baseAddFieldOption);
    OptionChange optionChange = messageResult.getFieldPatches(0).getOptionChangeList().get(0);

    Assert.assertEquals(ChangeType.ADDITION, optionChange.getChangeType());
    Assert.assertEquals(OptionChange.OptionType.FIELD_OPTION, optionChange.getType());

    ByteString payload = optionChange.getPayloadNew();
    Option.TestOption option = Option.TestOption.parseFrom(payload);
  }

  @Test
  public void removeFieldOptionTest() throws IOException {
    ProtoDomain base = TestSets.baseAddFieldOption();
    ProtoDomain baseRemoveFieldOption = TestSets.baseKnownOption();

    MessagePatch messageResult = diffMessage(base, baseRemoveFieldOption);
    OptionChange optionChange = messageResult.getFieldPatches(0).getOptionChangeList().get(0);

    Assert.assertEquals(ChangeType.REMOVAL, optionChange.getChangeType());
    Assert.assertEquals(OptionChange.OptionType.FIELD_OPTION, optionChange.getType());

    ByteString payload = optionChange.getPayloadNew();
    Option.TestOption option = Option.TestOption.parseFrom(payload);
  }

  @Test
  public void changeFieldOptionTest() throws IOException {
    ProtoDomain base = TestSets.baseAddFieldOption();
    ProtoDomain baseChangeFieldOption = TestSets.baseChangeFieldOption();

    MessagePatch messageResult = diffMessage(base, baseChangeFieldOption);
    OptionChange optionChange = messageResult.getFieldPatches(0).getOptionChangeList().get(0);

    Assert.assertEquals(ChangeType.PAYLOAD_CHANGED, optionChange.getChangeType());
    Assert.assertEquals(OptionChange.OptionType.FIELD_OPTION, optionChange.getType());

    ByteString payloadOld = optionChange.getPayloadOld();
    Option.TestOption optionOld = Option.TestOption.parseFrom(payloadOld);
    ByteString payloadNew = optionChange.getPayloadNew();
    Option.TestOption optionNew = Option.TestOption.parseFrom(payloadNew);
    Assert.assertNotEquals(optionOld, optionNew);
  }

  @Test
  public void addFileOptionTest() throws IOException {
    ProtoDomain baseAddFileOption = TestSets.baseAddFileOption();
    ProtoDomain base = TestSets.baseKnownOption();

    FilePatch fileResult = diffFile(base, baseAddFileOption);
    OptionChange optionChange = fileResult.getOptionChangeList().get(0);

    Assert.assertEquals(ChangeType.ADDITION, optionChange.getChangeType());
    Assert.assertEquals(OptionChange.OptionType.FILE_OPTION, optionChange.getType());

    ByteString payload = optionChange.getPayloadNew();
    Option.TestOption option = Option.TestOption.parseFrom(payload);
  }

  @Test
  public void removeFileOptionTest() throws IOException {
    ProtoDomain baseRemoveFileOption = TestSets.baseKnownOption();
    ProtoDomain base = TestSets.baseAddFileOption();

    FilePatch fileResult = diffFile(base, baseRemoveFileOption);
    OptionChange optionChange = fileResult.getOptionChangeList().get(0);

    Assert.assertEquals(ChangeType.REMOVAL, optionChange.getChangeType());
    Assert.assertEquals(OptionChange.OptionType.FILE_OPTION, optionChange.getType());

    ByteString payload = optionChange.getPayloadNew();
    Option.TestOption option = Option.TestOption.parseFrom(payload);
  }

  @Test
  public void changeFileOptionTest() throws IOException {
    ProtoDomain baseChangeFileOption = TestSets.baseChangeFileOption();
    ProtoDomain base = TestSets.baseAddFileOption();

    FilePatch fileResult = diffFile(base, baseChangeFileOption);
    OptionChange optionChange = fileResult.getOptionChangeList().get(0);

    Assert.assertEquals(ChangeType.PAYLOAD_CHANGED, optionChange.getChangeType());
    Assert.assertEquals(OptionChange.OptionType.FILE_OPTION, optionChange.getType());

    ByteString payload = optionChange.getPayloadNew();
    Option.TestOption option = Option.TestOption.parseFrom(payload);
  }

  private MessagePatch diffMessage(ProtoDomain dRef, ProtoDomain dNew) throws IOException {
    ValidationResults results = new ValidationResults();
    ProtoDiff diff = new ProtoDiff(dRef, dNew, results);
    diff.diffOnMessage("test.v1.ProtoBeamBasicMessage");

    Patch result = results.createProto();
    System.out.println(result);
    return result.getMessagePatchesMap().get("test.v1.ProtoBeamBasicMessage");
  }

  private FilePatch diffFile(ProtoDomain dRef, ProtoDomain dNew) throws IOException {
    ValidationResults results = new ValidationResults();
    ProtoDiff diff = new ProtoDiff(dRef, dNew, results);
    diff.diffOnFileName("test/v1/simple.proto");

    Patch result = results.createProto();
    System.out.println(result);
    return result.getFilePatchesMap().get("test/v1/simple.proto");
  }
}
