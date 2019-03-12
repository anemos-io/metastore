package io.anemos.metastore.core.proto.validation;

import com.google.protobuf.Descriptors;
import io.anemos.metastore.core.Lint;
import io.anemos.metastore.core.proto.ProtoDescriptor;
import io.anemos.metastore.core.proto.validate.ProtoLint;
import io.anemos.metastore.core.proto.validate.ValidationResults;
import io.anemos.metastore.v1alpha1.FileResult;
import io.anemos.metastore.v1alpha1.Report;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;

public class LintTest {

    @Test
    public void fieldOkSnakeCase() throws IOException {
        Report result = lintMessage(Lint.LintFieldNamesGood.getDescriptor());
        Assert.assertEquals(0, result.getMessageResultsCount());
    }

    @Test
    public void fieldNokCamel() throws IOException {
        Report result = lintMessage(Lint.LintFieldNamesBad.getDescriptor());
        Assert.assertEquals(3, result.getMessageResultsMap().get("anemos.metastore.core.LintFieldNamesBad").getFieldResultsCount());
    }

    @Test
    public void messageLowerCase() throws IOException {
        Report result = lintMessage(Lint.lintmessagelowercase.getDescriptor());
        Assert.assertEquals(1, result.getMessageResultsMap().get("anemos.metastore.core.lintmessagelowercase").getInfoCount());
    }

    @Test
    public void messageCamelCase() throws IOException {
        Report result = lintMessage(Lint.lint_message_camelcase.getDescriptor());
        Assert.assertEquals(1, result.getMessageResultsMap().get("anemos.metastore.core.lint_message_camelcase").getInfoCount());
    }

    private Report lintMessage(Descriptors.Descriptor d) throws IOException {
        ProtoDescriptor pd = new ProtoDescriptor(d);
        String message = d.getFullName();

        ValidationResults results = new ValidationResults();
        ProtoLint lint = new ProtoLint(pd, results);
        lint.lintOnMessage(message);
        return results.getReport();
    }
}
