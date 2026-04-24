package com.company.officerservice.domain.model;

public sealed interface OfficerView permits OfficerFullView, OfficerRestrictedView {
}
