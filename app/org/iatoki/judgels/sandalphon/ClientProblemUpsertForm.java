package org.iatoki.judgels.sandalphon;

import play.data.validation.Constraints;

public final class ClientProblemUpsertForm {

    public ClientProblemUpsertForm() {
    }

    public ClientProblemUpsertForm(ClientProblem clientProblem) {
        this.clientJid = clientProblem.getClientJid();
    }

    @Constraints.Required
    public String clientJid;
}
