package io.anemos.metastore.core.proto.profile;

import io.anemos.metastore.v1alpha1.Report;

public class ProfileAllowAll implements ValidationProfile {

    public String profileName = "proto:all";

    @Override
    public Report validate(Report report) {
        return report;
    }
}
