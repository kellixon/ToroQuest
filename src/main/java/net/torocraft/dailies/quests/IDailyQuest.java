package net.torocraft.dailies.quests;

import net.minecraft.entity.player.EntityPlayer;
import net.torocraft.dailies.capabilities.IDailiesCapability;

public interface IDailyQuest extends IDailiesCapability {

	boolean isComplete();

	String getStatusMessage();

	String getDisplayName();

	String getName();

	String getType();

	String getId();

	void reward(EntityPlayer player);

}