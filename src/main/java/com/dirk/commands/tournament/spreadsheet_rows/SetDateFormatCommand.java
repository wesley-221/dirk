/*
 * MIT License
 *
 * Copyright (c) 2020 Wesley
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.dirk.commands.tournament.spreadsheet_rows;

import com.dirk.helper.EmbedHelper;
import com.dirk.helper.TournamentHelper;
import com.dirk.models.command.Command;
import com.dirk.models.command.CommandArgument;
import com.dirk.models.command.CommandArgumentType;
import com.dirk.models.command.CommandParameter;
import com.dirk.models.tournament.Tournament;
import com.dirk.repositories.TournamentRepository;
import org.javacord.api.event.message.MessageCreateEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class SetDateFormatCommand extends Command {
    private final TournamentRepository tournamentRepository;

    @Autowired
    public SetDateFormatCommand(TournamentRepository tournamentRepository) {
        this.commandName = "setdateformat";
        this.description = "Set format of how the date is formatted.";
        this.group = "Tournament management";

        this.requiresAdmin = true;
        this.guildOnly = true;

        this.commandArguments.add(new CommandArgument("date format", "Enter the format of how the date is formatted. \n\n**Allowed input:** `%d` (day), `%m` (month), `/`, `-`\n**Example:** `%d/%m`, `%m-%d`", CommandArgumentType.String));

        this.tournamentRepository = tournamentRepository;
    }

    @Override
    public void execute(MessageCreateEvent messageCreateEvent) {
    }

    @Override
    public void execute(MessageCreateEvent messageCreateEvent, List<CommandParameter> commandParams) {
        String dateFormat = (String) commandParams.stream().findFirst().get().getValue();

        if (!TournamentHelper.validateDateFormat(dateFormat)) {
            messageCreateEvent
                    .getChannel()
                    .sendMessage(EmbedHelper.genericErrorEmbed(this.getCommandHelpFormat("Invalid `date format` argument given.\n\n"), messageCreateEvent.getMessageAuthor().getDiscriminatedName()));
            return;
        }

        Tournament existingTournament = TournamentHelper.getRunningTournament(messageCreateEvent, tournamentRepository);

        if (existingTournament == null) {
            messageCreateEvent
                    .getChannel()
                    .sendMessage(EmbedHelper.genericErrorEmbed("There is no tournament running in this server.", messageCreateEvent.getMessageAuthor().getDiscriminatedName()));
            return;
        }

        existingTournament.setDateFormat(dateFormat);
        tournamentRepository.save(existingTournament);

        messageCreateEvent
                .getChannel()
                .sendMessage(EmbedHelper.genericSuccessEmbed("Set the date format to `" + dateFormat + "`.", messageCreateEvent.getMessageAuthor().getDiscriminatedName()));
    }
}