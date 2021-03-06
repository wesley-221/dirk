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

package com.dirk.listeners;

import com.dirk.helper.EmbedHelper;
import com.dirk.helper.Log;
import com.dirk.helper.RegisterListener;
import com.dirk.meta.CustomCommandComponent;
import com.dirk.models.command.Command;
import com.dirk.models.command.CommandArgument;
import com.dirk.models.command.CommandArgumentType;
import com.dirk.models.command.CommandParameter;
import com.dirk.models.entities.CustomCommand;
import org.javacord.api.event.message.MessageCreateEvent;
import org.javacord.api.listener.message.MessageCreateListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

@Component
public class CommandListener implements MessageCreateListener, RegisterListener {
    private final String discordCommandPrefix;
    private final CustomCommandComponent customCommandComponent;
    private final List<Command> allCommands = new ArrayList<>();
    private List<CustomCommand> allCustomCommands;

    @Autowired
    public CommandListener(ApplicationContext applicationContext, @Value("${discord.prefix}") String discordCommandPrefix, CustomCommandComponent customCommandComponent) {
        this.discordCommandPrefix = discordCommandPrefix;
        this.customCommandComponent = customCommandComponent;
        this.allCustomCommands = customCommandComponent.getAllCustomCommands();

        // Validate commands
        for (Command command : applicationContext.getBeansOfType(Command.class).values()) {
            if (command.getCommandName() == null) {
                Log.error("Unable to register the command " + command.getClass() + ". You have to set the command name in order for it to be recognized.");
            } else if (command.getDescription() == null) {
                Log.error("Unable to register the command " + command.getClass() + ". You have to set the description in order for it to be recognized.");
            } else if (command.getGroup() == null) {
                Log.error("Unable to register the command " + command.getClass() + ". You have to set the group in order for it to be recognized.");
            } else if (command.getCommandArgumentsCount() > 0) {
                int commandArgumentIndex = 1;
                boolean commandArgumentsValid = true;

                // Loop through all command arguments
                for (CommandArgument commandArgument : command.getCommandArguments()) {
                    // Check for String arguments
                    if (commandArgument.getType() == CommandArgumentType.String) {
                        // String argument was found, check if it is the last argument
                        if (commandArgumentIndex != command.getCommandArgumentsCount()) {
                            commandArgumentsValid = false;
                            break;
                        }
                    }

                    commandArgumentIndex++;
                }

                if (commandArgumentsValid) {
                    allCommands.add(command);
                    Log.info("Registered the command " + command.getClass().getName());
                } else {
                    Log.error("Unable to register the command " + command.getClass() + ". The String argument can only be last parameter. Use SingleString for a single word.");
                }
            } else {
                allCommands.add(command);
                Log.info("Registered the command " + command.getClass().getName());
            }
        }

        // Log the custom commands
        for (CustomCommand customCommand : this.allCustomCommands) {
            Log.info("Registered the custom " + (customCommand.getServerSnowflake() == 0L ? "global" : "guild") + " command " + customCommand.getName());
        }
    }

    @Override
    public void onMessageCreate(MessageCreateEvent messageCreateEvent) {
        // Check if the author is not a bot
        if (messageCreateEvent.getMessageAuthor().isBotUser())
            return;

        // Check if the message starts with the command prefix
        if (!messageCreateEvent.getMessage().getContent().startsWith(discordCommandPrefix))
            return;

        this.allCustomCommands = customCommandComponent.getAllCustomCommands();

        // Remove the discord prefix
        List<String> commandSplit = new ArrayList<>(Arrays.asList(messageCreateEvent.getMessage().getContent().substring(discordCommandPrefix.length()).split(" ")));
        String commandName = commandSplit.get(0);
        commandSplit.remove(0);

        // Get the command by the given name
        Command command = this.getCommandByName(commandName);

        // Check if the command exists
        if (command != null) {
            // Check if the command is guild only
            if (command.isGuildOnly() != null && command.isGuildOnly() && !messageCreateEvent.isServerMessage()) {
                return;
            }

            // Check if the command requires owner privileges
            if (command.getRequiresBotOwner() != null && command.getRequiresBotOwner()) {
                if (!messageCreateEvent.getMessageAuthor().isBotOwner()) {
                    messageCreateEvent.getChannel().sendMessage(EmbedHelper.genericErrorEmbed("You have to be the owner of the bot to use this command.", messageCreateEvent.getMessageAuthor().getDiscriminatedName()));
                    return;
                }
            }

            // Check if the command requires administrator privileges
            if (command.getRequiresAdmin() != null && command.getRequiresAdmin()) {
                // The user is not an administrator
                if (!messageCreateEvent.getMessageAuthor().isServerAdmin()) {
                    messageCreateEvent.getChannel().sendMessage(EmbedHelper.genericErrorEmbed("You have to be an administrator to use this command.", messageCreateEvent.getMessageAuthor().getDiscriminatedName()));
                    return;
                }
            }

            // Check if the command has arguments
            if (command.hasCommandArguments()) {
                // Check if last argument is a String
                if (command.getCommandArguments().get(command.getCommandArgumentsCount() - 1).getType() == CommandArgumentType.String) {
                    String commandArgumentsString = String.join(" ", commandSplit);
                    commandSplit = new ArrayList<>(Arrays.asList(commandArgumentsString.split(" ", command.getCommandArgumentsCount())));
                }

                long optionalParameters = command.getCommandArguments().stream().filter(CommandArgument::isOptional).count();

                // Check if the arguments match
                if (command.getCommandArgumentsCount() != commandSplit.size()) {
                    // There are optional parameters
                    if (optionalParameters > 0) {
                        if (command.getCommandArgumentsCount() != (commandSplit.size() + optionalParameters)) {
                            messageCreateEvent.getChannel().sendMessage(EmbedHelper.genericErrorEmbed(command.getIncorrectCommandHelpFormat(), messageCreateEvent.getMessageAuthor().getDiscriminatedName()));
                            return;
                        }
                    }
                    // There are no optional parameters
                    else {
                        messageCreateEvent.getChannel().sendMessage(EmbedHelper.genericErrorEmbed(command.getIncorrectCommandHelpFormat(), messageCreateEvent.getMessageAuthor().getDiscriminatedName()));
                        return;
                    }
                }

                List<CommandParameter> commandParameters = new ArrayList<>();
                int index = 0;

                // Loop through all parameters
                for (CommandArgument commandArgument : command.getCommandArguments()) {
                    try {
                        String commandSplitIndex = commandSplit.get(index);

                        CommandParameter commandParameter;

                        // Check for the command types and parse them
                        switch (commandArgument.getType()) {
                            case SingleString:
                            case String:
                                commandParameters.add(new CommandParameter(commandArgument.getKey(), commandSplitIndex, true));
                                break;
                            case Boolean:
                                commandParameters.add(new CommandParameter(commandArgument.getKey(), Boolean.valueOf(commandSplitIndex), true));
                                break;
                            case Date:
                                try {
                                    Date formattedDate;
                                    DateFormat format = new SimpleDateFormat("dd/MM/yyyy");
                                    formattedDate = format.parse(commandSplitIndex);

                                    commandParameter = new CommandParameter(commandArgument.getKey(), formattedDate, true);
                                } catch (Exception ex) {
                                    commandParameter = new CommandParameter(commandArgument.getKey(), new Date(), false);
                                }

                                commandParameters.add(commandParameter);
                                break;
                            case Integer:
                                try {
                                    Integer parsed = Integer.parseInt(commandSplitIndex);
                                    commandParameters.add(new CommandParameter(commandArgument.getKey(), parsed, true));
                                } catch (Exception ex) {
                                    commandParameters.add(new CommandParameter(commandArgument.getKey(), -1, false));
                                }

                                break;
                            default:
                                break;
                        }
                    } catch (IndexOutOfBoundsException exception) {
                        CommandParameter commandParameter = new CommandParameter(commandArgument.getKey(), 0, true);
                        commandParameter.setIsOptional(true);

                        commandParameters.add(commandParameter);
                    }

                    index++;
                }

                command.execute(messageCreateEvent, commandParameters);
            }
            // The command has no arguments
            else {
                command.execute(messageCreateEvent);
            }

            Log.info(String.format("%s ran the command: %s", messageCreateEvent.getMessageAuthor().getDiscriminatedName(), commandName));
        } else {
            CustomCommand customCommand = this.getCustomCommandByName(commandName);

            if (customCommand != null) {
                // Global command, run regardless of server
                if (customCommand.getServerSnowflake() == 0L) {
                    messageCreateEvent.getChannel().sendMessage(customCommand.getMessage());
                } else {
                    if (messageCreateEvent.isServerMessage() && messageCreateEvent.getServer().get().getId() == customCommand.getServerSnowflake()) {
                        messageCreateEvent.getChannel().sendMessage(customCommand.getMessage());
                    }
                }
            }
        }
    }

    /**
     * Get a command by the given name
     *
     * @param commandName the name of the command
     * @return the command
     */
    private Command getCommandByName(String commandName) {
        Command foundCommand = null;

        for (Command command : this.allCommands) {
            if (command.getCommandName().equals(commandName)) {
                foundCommand = command;
            }
        }

        return foundCommand;
    }

    /**
     * Get a custom command by the given name and server snowflake
     *
     * @param commandName the name of the custom command
     * @return the custom command
     */
    private CustomCommand getCustomCommandByName(String commandName) {
        CustomCommand foundCommand = null;

        for (CustomCommand customCommand : this.allCustomCommands) {
            if (customCommand.getName().equals(commandName)) {
                foundCommand = customCommand;
            }
        }

        return foundCommand;
    }
}
