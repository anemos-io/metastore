package io.anemos.metastore.core.proto.profile;

import io.anemos.metastore.v1alpha1.*;

import static io.anemos.metastore.v1alpha1.FieldChangeInfo.FieldChangeType.FIELD_REMOVED;

public class ProfileAvroEvolve implements ValidationProfile {


    @Override
    public Report validate(Report report) {
        Report.Builder builder = Report.newBuilder(report);
        int error = 0;
        for (MessageResult messageResult : builder.getMessageResultsMap().values()) {
            for (FieldResult fieldResult : messageResult.getFieldResultsList()) {
                if (fieldResult.getChange().getChangeType() == FIELD_REMOVED) {
                    error++;
                }
            }
        }
        builder.setResultCount(ResultCount.newBuilder()
                .setDiffErrors(error)
                .build());
        return builder.build();
    }
}
