package com.tripass.saving.analysis;

public record EligibilityResult(boolean eligible, String exclusionReason) {

    public static EligibilityResult pass() {
        return new EligibilityResult(true, null);
    }

    public static EligibilityResult excluded(String reason) {
        return new EligibilityResult(false, reason);
    }
}
