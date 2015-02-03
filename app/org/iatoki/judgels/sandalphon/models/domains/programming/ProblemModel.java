package org.iatoki.judgels.sandalphon.models.domains.programming;

import org.iatoki.judgels.commons.models.domains.AbstractJudgelsModel;
import org.iatoki.judgels.gabriel.GradingType;

import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Table;

@Entity
@Table(name = "sandalphon_problem_programming")
public final class ProblemModel extends AbstractJudgelsModel {
    public String name;

    public String gradingType;

    public String additionalNote;

    public ProblemModel() {

    }

    public ProblemModel(String name, String gradingType, String additionalNote) {
        this.name = name;
        this.gradingType = gradingType;
        this.additionalNote = additionalNote;
    }

    public ProblemModel(long id, String jid, String name, String gradingType, String note) {
        this(name, gradingType, note);
        this.id = id;
        this.jid = jid;
    }
}