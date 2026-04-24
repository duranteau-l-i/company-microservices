package com.company.officerservice.domain.exception;

import java.util.UUID;

public class DuplicateLinkException extends RuntimeException {
    public DuplicateLinkException(UUID companyId, String title) {
        super("Officer already linked to company " + companyId + " with title: " + title);
    }
}
