package com.company.companyservice.domain.model;

public sealed interface CompanyView permits CompanyFullView, CompanyRestrictedView {
}
