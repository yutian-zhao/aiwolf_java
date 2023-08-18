package org.aiwolf.liuchang;

import org.aiwolf.sample.lib.AbstractRoleAssignPlayer;

public class HowlsRoleAssignPlayer extends AbstractRoleAssignPlayer {

	public HowlsRoleAssignPlayer() {
		setVillagerPlayer(new HowlsVillager());
		setBodyguardPlayer(new HowlsBodyguard());
		setMediumPlayer(new HowlsMedium());
		setSeerPlayer(new HowlsSeer());
		setPossessedPlayer(new HowlsPossessed());
		setWerewolfPlayer(new HowlsWerewolf());
	} 

	@Override
	public String getName() {
		return "Howls";
	}
}
