package com.acme.questionnaire.model;

import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

@Getter
public class AnswerAggregate {
    private final AnswerSnapshot answer;
    private final List<OpinionSnapshot> opinions = new ArrayList<>();

    public AnswerAggregate(AnswerSnapshot answer) {
        this.answer = answer;
    }

    public void addOpinion(OpinionSnapshot opinion) {
        opinions.add(opinion);
    }
}
