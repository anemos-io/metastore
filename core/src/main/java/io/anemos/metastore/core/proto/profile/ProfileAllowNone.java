package io.anemos.metastore.core.proto.profile;

import io.anemos.metastore.v1alpha1.Report;

public class ProfileAllowNone implements ValidationProfile {


    @Override
    public Report validate(Report report) {
        return report;
    }
}
