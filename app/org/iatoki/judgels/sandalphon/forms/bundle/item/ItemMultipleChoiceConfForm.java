package org.iatoki.judgels.sandalphon.forms.bundle.item;

import com.google.common.collect.Lists;
import play.data.validation.Constraints;

import java.util.List;

public final class ItemMultipleChoiceConfForm {

    public ItemMultipleChoiceConfForm() {
        choiceAliases = Lists.newArrayList("a","b","c","d","e");
        choiceContents = Lists.newArrayList("","","","","");
        isCorrects = Lists.newArrayList(false, false, false, false, false);
        score = 1.0;
        penalty = 0.0;
    }

    @Constraints.Required
    public String meta;

    @Constraints.Required
    public String statement;

    @Constraints.Required
    public Double score;

    @Constraints.Required
    public Double penalty;

    public List<String> choiceAliases;

    public List<String> choiceContents;

    public List<Boolean> isCorrects;

}
