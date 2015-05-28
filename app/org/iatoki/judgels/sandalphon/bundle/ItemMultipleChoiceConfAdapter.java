package org.iatoki.judgels.sandalphon.bundle;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.gson.Gson;
import org.iatoki.judgels.sandalphon.forms.bundle.item.ItemMultipleChoiceConfForm;
import org.iatoki.judgels.sandalphon.views.html.bundle.item.itemMultipleChoiceConfView;
import play.api.mvc.Call;
import play.data.Form;
import play.mvc.Http;
import play.twirl.api.Html;

public final class ItemMultipleChoiceConfAdapter implements BundleItemConfAdapter {
    @Override
    public Form generateForm() {
        return Form.form(ItemMultipleChoiceConfForm.class).fill(new ItemMultipleChoiceConfForm());
    }

    @Override
    public Form generateForm(String conf, String meta) {
        ItemMultipleChoiceConf itemConf = new Gson().fromJson(conf, ItemMultipleChoiceConf.class);
        ItemMultipleChoiceConfForm itemForm = new ItemMultipleChoiceConfForm();
        itemForm.statement = itemConf.statement;
        itemForm.meta = meta;
        if (itemConf.score != null) {
            itemForm.score = itemConf.score;
        }
        if (itemConf.penalty != null) {
            itemForm.penalty = itemConf.penalty;
        }
        itemForm.choiceAliases = Lists.newArrayList();
        itemForm.choiceContents = Lists.newArrayList();
        itemForm.isCorrects = Lists.newArrayList();
        for (ItemChoice itemChoice : itemConf.choices) {
            itemForm.choiceAliases.add(itemChoice.getAlias());
            itemForm.choiceContents.add(itemChoice.getContent());
            itemForm.isCorrects.add(itemChoice.isCorrect());
        }

        return Form.form(ItemMultipleChoiceConfForm.class).fill(itemForm);
    }

    @Override
    public Html getConfHtml(Form form, Call target, String submitLabel) {
        return itemMultipleChoiceConfView.render(form, target, submitLabel);
    }

    @Override
    public Form bindFormFromRequest(Http.Request request) {
        return Form.form(ItemMultipleChoiceConfForm.class).bindFromRequest();
    }

    @Override
    public String getMetaFromForm(Form form) {
        Form<ItemMultipleChoiceConfForm> realForm = (Form<ItemMultipleChoiceConfForm>)form;
        ItemMultipleChoiceConfForm itemForm = realForm.get();

        return itemForm.meta;
    }

    @Override
    public String processRequestForm(Form form) {
        Form<ItemMultipleChoiceConfForm> realForm = (Form<ItemMultipleChoiceConfForm>)form;
        ItemMultipleChoiceConfForm itemForm = realForm.get();
        ItemMultipleChoiceConf itemConf = new ItemMultipleChoiceConf();
        itemConf.statement = itemForm.statement;
        itemConf.score = itemForm.score;
        itemConf.penalty = itemForm.penalty;
        ImmutableList.Builder<ItemChoice> itemChoiceBuilder = ImmutableList.builder();
        for (int i=0;i<itemForm.choiceContents.size();++i) {
            boolean isCorrect = false;
            if ((itemForm.isCorrects.size() > i) && (itemForm.isCorrects.get(i) != null)) {
                isCorrect = itemForm.isCorrects.get(i);
            }
            itemChoiceBuilder.add(new ItemChoice(itemForm.choiceAliases.get(i), itemForm.choiceContents.get(i), isCorrect));
        }
        itemConf.choices = itemChoiceBuilder.build();

        return new Gson().toJson(itemConf);
    }

    @Override
    public BundleItemConf parseConfString(String conf) {
        return new Gson().fromJson(conf, ItemMultipleChoiceConf.class);
    }
}