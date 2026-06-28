package com.acme.questionnaire.model;

import lombok.Value;

@Value
public class TableOrderClause {
    String expression;
    String direction;
}
