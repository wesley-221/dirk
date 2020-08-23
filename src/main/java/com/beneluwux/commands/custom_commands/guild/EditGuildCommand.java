package com.beneluwux.commands.custom_commands.guild;

import com.beneluwux.helper.EmbedHelper;
import com.beneluwux.models.command.Command;
import com.beneluwux.models.command.CommandArgument;
import com.beneluwux.models.command.CommandArgumentType;
import com.beneluwux.models.command.CommandParameter;
import com.beneluwux.models.entities.CustomCommand;
import com.beneluwux.repositories.CustomCommandRepository;
import org.javacord.api.event.message.MessageCreateEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class EditGuildCommand extends Command {
    private final ApplicationContext applicationContext;
    private final CustomCommandRepository customCommandRepository;

    @Autowired
    public EditGuildCommand(ApplicationContext applicationContext, CustomCommandRepository customCommandRepository) {
        this.commandName = "editguildcommand";
        this.requiresAdmin = true;

        this.commandArguments.add(new CommandArgument("command name", "The name of the command to edit", CommandArgumentType.SingleString));
        this.commandArguments.add(new CommandArgument("command output", "The output of the command", CommandArgumentType.String));

        this.applicationContext = applicationContext;
        this.customCommandRepository = customCommandRepository;
    }

    @Override
    public void execute(MessageCreateEvent messageCreateEvent) {
    }

    @Override
    public void execute(MessageCreateEvent messageCreateEvent, List<CommandParameter> commandParams) {
        CommandParameter commandKey = commandParams.get(0);
        CommandParameter commandMessage = commandParams.get(1);

        CustomCommand customCommand = customCommandRepository.findByNameAndServerSnowflake((String) commandKey.getParamaterValue(), messageCreateEvent.getServer().get().getId());

        if (customCommand == null) {
            messageCreateEvent.getChannel().sendMessage(EmbedHelper.genericErrorEmbed("The guild command `" + commandKey.getParamaterValue() + "` doesn't exist.", messageCreateEvent.getMessageAuthor().getDiscriminatedName()));
        } else {
            customCommand.setMessage((String) commandMessage.getParamaterValue());
            customCommandRepository.save(customCommand);

            messageCreateEvent.getChannel().sendMessage(EmbedHelper.genericSuccessEmbed("Edited the guild command `" + commandKey.getParamaterValue() + "`: `" + commandMessage.getParamaterValue() + "`", messageCreateEvent.getMessageAuthor().getDiscriminatedName()));
        }
    }
}