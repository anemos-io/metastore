package io.anemos.metastore.core.proto.profile;

import io.anemos.metastore.v1alpha1.Report;

public class ProfileAllowAll implements ValidationProfile {


    @Override
    public Report validate(Report report) {
        return report;
    }
}
