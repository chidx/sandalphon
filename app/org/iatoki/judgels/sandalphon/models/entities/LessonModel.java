package org.iatoki.judgels.sandalphon.models.entities;

import org.iatoki.judgels.commons.models.JidPrefix;
import org.iatoki.judgels.commons.models.domains.AbstractJudgelsModel;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

@Entity
@Table(name = "sandalphon_lesson")
@JidPrefix("LESS")
public final class LessonModel extends AbstractJudgelsModel {
    public String name;

    @Column(columnDefinition = "TEXT")
    public String additionalNote;
}
