package com.avairebot.commands.administration;

import com.avairebot.AvaIre;
import com.avairebot.commands.CommandMessage;
import com.avairebot.contracts.commands.Command;
import com.avairebot.contracts.commands.CommandContext;
import com.avairebot.database.transformers.GuildTransformer;
import com.avairebot.factories.MessageFactory;
import com.avairebot.utilities.RoleUtil;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.Role;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class IAmNotCommand extends Command {

    public IAmNotCommand(AvaIre avaire) {
        super(avaire, false);
    }

    @Override
    public String getName() {
        return "I Am Not Command";
    }

    @Override
    public String getDescription() {
        return "Removes the role with the given name from you if it is in the self-assignable list of roles.";
    }

    @Override
    public List<String> getUsageInstructions() {
        return Collections.singletonList("`:command <role>`");
    }

    @Override
    public List<String> getExampleUsage() {
        return Collections.singletonList("`:command DJ`");
    }

    @Override
    public List<Class<? extends Command>> getRelations() {
        return Collections.singletonList(IAmCommand.class);
    }

    @Override
    public List<String> getTriggers() {
        return Arrays.asList("iamnot", "iamn");
    }

    @Override
    public boolean onCommand(CommandMessage context, String[] args) {
        if (args.length == 0) {
            return sendErrorMessage(context, "Missing argument, the `role` argument is required.");
        }

        String roleName = String.join(" ", args);
        Role role = RoleUtil.getRoleFromMentionsOrName(context.getMessage(), roleName);
        if (role == null) {
            context.makeWarning(":user Invalid role, I couldn't find any role called **:role**")
                .set("role", roleName)
                .queue(message -> handleMessage(context, message));
            return false;
        }

        GuildTransformer transformer = context.getGuildTransformer();
        if (transformer == null) {
            return sendErrorMessage(context, "errors.errorOccurredWhileLoading", "server settings");
        }

        if (!transformer.getSelfAssignableRoles().containsValue(role.getName().toLowerCase())) {
            context.makeWarning(":user Invalid role, **:role** is not a self-assignable role.")
                .set("role", roleName)
                .queue(message -> handleMessage(context, message));
            return false;
        }

        if (RoleUtil.isRoleHierarchyHigher(context.getGuild().getSelfMember().getRoles(), role)) {
            context.makeWarning(":user The role is higher in the role hierarchy, I can't give/remove the **:role**  role from anyone.")
                .set("role", roleName)
                .queue(message -> handleMessage(context, message));
            return false;
        }

        if (RoleUtil.hasRole(context.getMember(), role)) {
            context.getGuild().getController().removeSingleRoleFromMember(context.getMember(), role).queue();
        }

        context.makeSuccess(":user You no longer have the **:role** role!")
            .set("role", role.getName())
            .queue(message -> handleMessage(context, message));
        return true;
    }

    private void handleMessage(CommandContext context, Message message) {
        MessageFactory.deleteMessage(context.getMessage());
        MessageFactory.deleteMessage(message, 45);
    }
}
