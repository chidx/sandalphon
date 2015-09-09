package org.iatoki.judgels.sandalphon.controllers;

import com.google.common.base.Joiner;
import com.google.common.collect.Sets;

import java.util.Set;

final class PartnerControllerUtils {

    private PartnerControllerUtils() {
        // prevent instantiation
    }

    static Set<String> splitByComma(String s) {
        if (s == null || s.isEmpty()) {
            return null;
        }
        return Sets.newHashSet(s.split(","));
    }

    static String combineByComma(Set<String> list) {
        if (list == null) {
            return null;
        }
        return Joiner.on(",").join(list);
    }
}
